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

import se.kth.speech.coin.tangrams.wac.data.Parameters;
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

		private final String word;

		private final double weight;

		private final Supplier<? extends Collection<Referent>> exampleSupplier;

		private WordClassifierTrainer(final String word, final Supplier<? extends Collection<Referent>> exampleSupplier,
				final double weight) {
			this.word = word;
			this.exampleSupplier = exampleSupplier;
			this.weight = weight;
		}

		@Override
		public Entry<String, Logistic> get() {
			final Logistic logistic = new Logistic();
			final Collection<Referent> examples = exampleSupplier.get();
			final Instances dataset = new Instances("Dataset", atts, examples.size());
			dataset.setClass(TARGET);

			for (final Referent ref : examples) {
				final Instance instance = toInstance(ref);
				final double totalWeight = weight * (ref.isTarget() ? 19 : 1);
				instance.setWeight(totalWeight);
				dataset.add(instance);
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

	private final ConcurrentMap<String, Logistic> wordModels;
	private Attribute SHAPE;

	private Attribute SIZE;

	private Attribute GREEN;

	private Attribute RED;

	private Attribute BLUE;

	private Attribute HUE;

	private Attribute EDGE_COUNT;

	private Attribute POSX;

	private Attribute POSY;

	private Attribute MIDX;

	private Attribute MIDY;

	private Attribute TARGET;

	private ArrayList<Attribute> atts;

	private Logistic discountModel;

	private RoundSet trainingSet;

	private Vocabulary vocab;

	private final Executor asynchronousJobExecutor;

	public LogisticModel() {
		this(ForkJoinPool.commonPool(), DEFAULT_EXPECTED_WORD_CLASS_COUNT);
	}

	public LogisticModel(final Executor asynchronousJobExecutor) {
		this(asynchronousJobExecutor, DEFAULT_EXPECTED_WORD_CLASS_COUNT);
	}

	public LogisticModel(final Executor asynchronousJobExecutor, final int expectedWordClassCount) {
		this.asynchronousJobExecutor = asynchronousJobExecutor;
		wordModels = new ConcurrentHashMap<>(expectedWordClassCount);
	}

	public Vocabulary getVocabulary() {
		return vocab;
	}

	/**
	 * Returns a ranking of the referents in a round
	 */
	public List<Referent> rank(final Round round) throws ClassificationException {
		final Map<Referent, Double> scores = new HashMap<>();
		for (final Referent ref : round.getReferents()) {
			final Instance inst = toInstance(ref);
			final Mean mean = new Mean();
			for (final String word : round.getWords()) {
				double score = score(word, inst);
				if (Parameters.WEIGHT_BY_FREQ) {
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
		final DenseInstance instance = new DenseInstance(atts.size());
		instance.setValue(SHAPE, ref.getShape());
		// instance.setValue(EDGE_COUNT, ref.getEdgeCount());
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
		for (final Referent ref : rank(round)) {
			rank++;
			if (ref.isTarget()) {
				return rank;
			}
		}
		return rank;
	}

	/**
	 * Trains models for the specified words.
	 */
	private void train(final List<String> words, final double weight) {
		final CompletableFuture<Void> trainingJob = trainAsynchronously(words, weight);
		trainingJob.join();
	}

	/**
	 * Trains models for the specified words.
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

		// System.out.println("Training " + words);

		// Train a model for each word
		final Stream<CompletableFuture<Void>> wordClassifierTrainingJobs = words.stream()
				.map(word -> CompletableFuture
						.supplyAsync(new WordClassifierTrainer(word, () -> trainingSet.getExamples(word), weight),
								asynchronousJobExecutor)
						.thenAccept(
								wordClassifier -> wordModels.put(wordClassifier.getKey(), wordClassifier.getValue())));
		// Train the discount model
		final CompletableFuture<Void> discountClassifierTrainingJob = CompletableFuture
				.supplyAsync(
						new WordClassifierTrainer("__OUT_OF_VOCABULARY__",
								() -> trainingSet.getDiscountExamples(vocab.getWords()), weight),
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
		vocab = trainingSet.getVocabulary();
		vocab.prune(Parameters.DISCOUNT);
		train(vocab.getUpdatedWordsSince(oldVocab), Parameters.UPDATE_WEIGHT);
	}

	/**
	 * Evaluates a SessionSet and returns the mean rank
	 */
	double eval(final SessionSet set) throws ClassificationException {
		final Mean mean = new Mean();
		for (final Session session : set.getSessions()) {
			for (final Round round : session.getRounds()) {
				mean.increment(targetRank(round));
				if (Parameters.UPDATE_MODEL) {
					updateModel(round);
				}
			}
		}
		return mean.getResult();
	}

	/**
	 * Trains the word models using all data from a SessionSet
	 */
	void train(final SessionSet set) {
		final CompletableFuture<Void> trainingJob = trainAsynchronously(set);
		trainingJob.join();
	}

	/**
	 * Trains the word models using all data from a {@link SessionSet}.
	 *
	 * @param set
	 *            The {@code SessionSet} to use as training data.
	 * @return A {@link CompletableFuture} representing the
	 *         {@link CompletableFuture#complete(Object) completion} of the
	 *         training of models for all words in the vocabulary.
	 */
	CompletableFuture<Void> trainAsynchronously(final SessionSet set) {

		trainingSet = new RoundSet(set);
		vocab = trainingSet.getVocabulary();
		vocab.prune(Parameters.DISCOUNT);

		atts = new ArrayList<>();

		atts.add(SHAPE = new Attribute("shape", new ArrayList<>(Referent.getShapes())));
		// atts.add(EDGE_COUNT = new Attribute("edge_count"));
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
