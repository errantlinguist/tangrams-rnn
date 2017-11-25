/*
 * 	Copyright 2017 Todd Shore
 *
 *	Licensed under the Apache License, Version 2.0 (the "License");
 *	you may not use this file except in compliance with the License.
 *	You may obtain a copy of the License at
 *
 *		http://www.apache.org/licenses/LICENSE-2.0
 *
 *	Unless required by applicable law or agreed to in writing, software
 *	distributed under the License is distributed on an "AS IS" BASIS,
 *	WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *	See the License for the specific language governing permissions and
 *	limitations under the License.
 */
package se.kth.speech.coin.tangrams.wac.logistic;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Executor;
import java.util.concurrent.ForkJoinPool;
import java.util.function.Supplier;
import java.util.stream.Stream;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.math3.stat.descriptive.moment.Mean;

import se.kth.speech.NumberTypeConversions;
import se.kth.speech.coin.tangrams.wac.data.Referent;
import se.kth.speech.coin.tangrams.wac.data.Round;
import se.kth.speech.coin.tangrams.wac.data.RoundSet;
import se.kth.speech.coin.tangrams.wac.data.Session;
import se.kth.speech.coin.tangrams.wac.data.SessionSet;
import se.kth.speech.coin.tangrams.wac.data.Vocabulary;
import weka.classifiers.functions.Logistic;
import weka.core.Attribute;
import weka.core.DenseInstance;
import weka.core.Instance;
import weka.core.Instances;

public class LogisticModel {

	private class WordClassifierTrainer implements Supplier<Entry<String, Logistic>> {

		private final Supplier<? extends Stream<Weighted<Referent>>> exampleSupplier;

		private final double weight;

		private final String word;

		private WordClassifierTrainer(final String word,
				final Supplier<? extends Stream<Weighted<Referent>>> exampleSupplier, final double weight) {
			this.word = word;
			this.exampleSupplier = exampleSupplier;
			this.weight = weight;
		}

		@Override
		public Entry<String, Logistic> get() {
			final Logistic logistic = new Logistic();
			@SuppressWarnings("unchecked")
			final Weighted<Referent>[] examples = exampleSupplier.get().toArray(Weighted[]::new);
			final Instances dataset = new Instances("Dataset", atts, examples.length);
			dataset.setClass(TARGET);

			for (final Weighted<Referent> example : examples) {
				final Referent ref = example.getWrapped();
				final Instance inst = toInstance(ref);
				// The instance's current weight is the weight of the instance's
				// class; now multiply it by the weight of the word the model is
				// being trained for
				final double totalWeight = weight * example.getWeight();
				inst.setWeight(totalWeight);
				dataset.add(inst);
			}

			try {
				logistic.buildClassifier(dataset);
			} catch (final Exception e) {
				throw new WordClassifierTrainingException(word, e);
			}

			return Pair.of(word, logistic);
		}

	}

	private static final int DEFAULT_EXPECTED_WORD_CLASS_COUNT = 1000;

	private static final List<String> REFERENT_CLASSIFICATION_VALUES = Arrays.asList(
			Stream.of(Boolean.TRUE, Boolean.FALSE).map(Object::toString).map(String::intern).toArray(String[]::new));

	private static Stream<Weighted<Referent>> createClassWeightedReferents(final Round round) {
		final List<Referent> refs = round.getReferents();
		final Referent[] posRefs = refs.stream().filter(Referent::isTarget).toArray(Referent[]::new);
		final Referent[] negRefs = refs.stream().filter(ref -> !ref.isTarget()).toArray(Referent[]::new);
		final double negClassWeight = 1.0;
		final double posClassWeight = negRefs.length / (double) posRefs.length;
		return Stream.concat(createWeightedReferents(posRefs, posClassWeight),
				createWeightedReferents(negRefs, negClassWeight));
	}

	private static Stream<Weighted<Referent>> createDiscountClassExamples(final RoundSet rounds,
			final Collection<? super String> words) {
		return rounds.getDiscountRounds(words).flatMap(LogisticModel::createClassWeightedReferents);
	}

	private static Stream<Weighted<Referent>> createWeightedReferents(final Referent[] refs, final double weight) {
		return Arrays.stream(refs).map(ref -> new Weighted<>(ref, weight));
	}

	private static Stream<Weighted<Referent>> createWordClassExamples(final RoundSet rounds, final String word) {
		return rounds.getExampleRounds(word).flatMap(LogisticModel::createClassWeightedReferents);
	}

	private final Executor asynchronousJobExecutor;

	private ArrayList<Attribute> atts;

	// private Attribute HUE;

	// private Attribute EDGE_COUNT;

	private Attribute BLUE;

	private Logistic discountModel;

	private Attribute GREEN;

	private Attribute MIDX;

	private Attribute MIDY;

	private Attribute POSX;

	private Attribute POSY;

	private Attribute RED;

	private Attribute SHAPE;

	private Attribute SIZE;

	private Attribute TARGET;

	private RoundSet trainingSet;

	private Vocabulary vocab;

	private final ConcurrentMap<String, Logistic> wordModels;

	private final Map<ModelParameter, Object> modelParams;

	public LogisticModel() {
		this(ModelParameter.createDefaultParamValueMap());
	}

	public LogisticModel(final Map<ModelParameter, Object> modelParams) {
		this(modelParams, ForkJoinPool.commonPool());
	}

	public LogisticModel(final Map<ModelParameter, Object> modelParams, final Executor asynchronousJobExecutor) {
		this(modelParams, asynchronousJobExecutor, DEFAULT_EXPECTED_WORD_CLASS_COUNT);
	}

	public LogisticModel(final Map<ModelParameter, Object> modelParams, final Executor asynchronousJobExecutor,
			final int expectedWordClassCount) {
		this.modelParams = modelParams;
		this.asynchronousJobExecutor = asynchronousJobExecutor;
		wordModels = new ConcurrentHashMap<>(expectedWordClassCount);
	}

	public Vocabulary getVocabulary() {
		return vocab;
	}

	public List<Referent> rank(final Round round) throws ClassificationException {
		final Map<Referent, Double> scores = new HashMap<>();
		for (final Referent ref : round.getReferents()) {
			final Instance inst = toInstance(ref);
			final Mean mean = new Mean();
			final Iterator<String> wordIter = round.getWords(modelParams).iterator();
			while (wordIter.hasNext()) {
				final String word = wordIter.next();
				double score = score(word, inst);
				// NOTE: Values are retrieved directly from the map instead of
				// e.g.
				// assigning
				// them to a final field because it's possible that the map
				// values
				// change at another place in the code and performance isn't an
				// issue
				// here anyway
				if ((Boolean) modelParams.get(ModelParameter.WEIGHT_BY_FREQ)) {
					score *= Math.log10(vocab.getCount(word, 3));
				}
				mean.increment(score);
			}
			scores.put(ref, mean.getResult());
		}
		final List<Referent> ranking = new ArrayList<>(round.getReferents());
		ranking.sort(new Comparator<Referent>() {
			@Override
			public int compare(final Referent o1, final Referent o2) {
				return scores.get(o2).compareTo(scores.get(o1));
			}
		});
		return ranking;
	}

	public double score(final String word, final Instance inst) throws ClassificationException {
		final Logistic model = wordModels.getOrDefault(word, discountModel);
		double[] dist;
		try {
			dist = model.distributionForInstance(inst);
		} catch (final Exception e) {
			throw new ClassificationException(e);
		}
		return dist[0];
	}

	public double score(final String word, final Referent ref) throws ClassificationException {
		return score(word, toInstance(ref));
	}

	public Instance toInstance(final Referent ref) {
		// NOTE: There is no reason to cache Instance values because
		// "Instances.add(Instance)" always creates a shallow copy thereof
		// anyway, so the only possible benefit of a cache would be avoiding the
		// computational cost of object construction at the cost of extra memory
		// consumption
		final DenseInstance instance = new DenseInstance(atts.size());
		instance.setValue(SHAPE, ref.getShape());
		// instance.setValue(EDGE_COUNT, Integer.toString(ref.getEdgeCount()));
		instance.setValue(SIZE, ref.getSize());
		instance.setValue(RED, ref.getRed());
		instance.setValue(GREEN, ref.getGreen());
		instance.setValue(BLUE, ref.getBlue());
		// instance.setValue(HUE, ref.hue);
		instance.setValue(POSX, ref.getPositionX());
		instance.setValue(POSY, ref.getPositionY());
		instance.setValue(MIDX, ref.getMidX());
		instance.setValue(MIDY, ref.getMidY());

		instance.setValue(TARGET, Boolean.toString(ref.isTarget()));
		return instance;
	}

	/**
	 * Returns the rank of the target referent in a round
	 */
	private int targetRank(final Round round) throws ClassificationException {
		int rank = 0;
		final Iterator<Referent> nbestRefIter = rank(round).iterator();
		while (nbestRefIter.hasNext()) {
			final Referent ref = nbestRefIter.next();
			rank++;
			if (ref.isTarget()) {
				return rank;
			}
		}
		throw new IllegalArgumentException("No target referent found.");
	}

	/**
	 * Trains models for the specified words asynchronously.
	 *
	 * @param words
	 *            The vocabulary words to train models for.
	 * @param weight
	 *            The weight of each datapoint representing a single observation
	 *            for a given word.
	 */
	private void train(final List<String> words, final double weight) {
		final CompletableFuture<Void> trainingJob = trainAsynchronously(words, weight);
		trainingJob.join();
	}

	/**
	 * Trains models for the specified words asynchronously.
	 *
	 * @param words
	 *            The vocabulary words to train models for.
	 * @param weight
	 *            The weight of each datapoint representing a single observation
	 *            for a given word.
	 * @return A {@link CompletableFuture} representing the
	 *         {@link CompletableFuture#complete(Object) completion} of the
	 *         training of models for all given words.
	 */
	private CompletableFuture<Void> trainAsynchronously(final List<String> words, final double weight) {
		// Train a model for each word
		final Stream<CompletableFuture<Void>> wordClassifierTrainingJobs = words.stream()
				.map(word -> CompletableFuture
						.supplyAsync(new WordClassifierTrainer(word, () -> createWordClassExamples(trainingSet, word),
								weight), asynchronousJobExecutor)
						.thenAccept(
								wordClassifier -> wordModels.put(wordClassifier.getKey(), wordClassifier.getValue())));
		// Train the discount model
		final CompletableFuture<Void> discountClassifierTrainingJob = CompletableFuture
				.supplyAsync(
						new WordClassifierTrainer("__OUT_OF_VOCABULARY__",
								() -> createDiscountClassExamples(trainingSet, vocab.getWords()), weight),
						asynchronousJobExecutor)
				.thenAccept(wordClassifier -> discountModel = wordClassifier.getValue());
		return CompletableFuture
				.allOf(Stream.concat(wordClassifierTrainingJobs, Stream.of(discountClassifierTrainingJob))
						.toArray(CompletableFuture[]::new));
	}

	/**
	 * Updates (trains) the models with the new round
	 */
	private void updateModel(final Round round) {
		trainingSet.getRounds().add(round);
		final Vocabulary oldVocab = vocab;
		vocab = trainingSet.createVocabulary();
		// NOTE: Values are retrieved directly from the map instead of e.g.
		// assigning
		// them to a final field because it's possible that the map values
		// change at another place in the code and performance isn't an issue
		// here anyway
		vocab.prune((Integer) modelParams.get(ModelParameter.DISCOUNT));
		final Number updateWeight = (Number) modelParams.get(ModelParameter.UPDATE_WEIGHT);
		train(vocab.getUpdatedWordsSince(oldVocab),
				NumberTypeConversions.finiteDoubleValue(updateWeight.doubleValue()));
	}

	/**
	 * Evaluates a SessionSet and returns the mean rank
	 */
	double eval(final SessionSet set) throws ClassificationException {
		final Mean mean = new Mean();
		for (final Session session : set.getSessions()) {
			for (final Round round : session.getRounds()) {
				mean.increment(targetRank(round));
				// NOTE: Values are retrieved directly from the map instead of
				// e.g.
				// assigning
				// them to a final field because it's possible that the map
				// values
				// change at another place in the code and performance isn't an
				// issue
				// here anyway
				final double updateWeight = ((Number) modelParams.get(ModelParameter.UPDATE_WEIGHT)).doubleValue();
				if (updateWeight > 0.0) {
					updateModel(round);
				}
			}
		}
		return mean.getResult();
	}

	/**
	 * Trains the word models using all data from a {@link SessionSet}.
	 *
	 * @param set
	 *            The {@code SessionSet} to use as training data.
	 */
	void train(final SessionSet set) {
		final CompletableFuture<Void> trainingJob = trainAsynchronously(set);
		trainingJob.join();
	}

	/**
	 * Trains the word models using all data from a {@link SessionSet}
	 * asynchronously.
	 *
	 * @param set
	 *            The {@code SessionSet} to use as training data.
	 * @return A {@link CompletableFuture} representing the
	 *         {@link CompletableFuture#complete(Object) completion} of the
	 *         training of models for all words in the vocabulary.
	 */
	CompletableFuture<Void> trainAsynchronously(final SessionSet set) {

		trainingSet = new RoundSet(set, modelParams);
		vocab = trainingSet.createVocabulary();
		// NOTE: Values are retrieved directly from the map instead of e.g.
		// assigning
		// them to a final field because it's possible that the map values
		// change at another place in the code and performance isn't an issue
		// here anyway
		vocab.prune((Integer) modelParams.get(ModelParameter.DISCOUNT));

		atts = new ArrayList<>();

		final List<String> shapeUniqueValues = new ArrayList<>(Referent.getShapes());
		atts.add(SHAPE = new Attribute("shape", shapeUniqueValues));
		// final List<String> edgeCountUniqueValues = Arrays
		// .asList(Referent.getEdgeCounts().stream().map(Number::toString).toArray(String[]::new));
		// atts.add(EDGE_COUNT = new Attribute("edge_count",
		// edgeCountUniqueValues));
		atts.add(SIZE = new Attribute("size"));
		atts.add(RED = new Attribute("red"));
		atts.add(GREEN = new Attribute("green"));
		atts.add(BLUE = new Attribute("blue"));
		// atts.add(HUE = new Attribute("hue"));

		atts.add(POSX = new Attribute("posx"));
		atts.add(POSY = new Attribute("posy"));

		atts.add(MIDX = new Attribute("midx"));
		atts.add(MIDY = new Attribute("midy"));
		atts.add(TARGET = new Attribute("target", REFERENT_CLASSIFICATION_VALUES));

		return trainAsynchronously(vocab.getWords(), 1.0);
	}

}
