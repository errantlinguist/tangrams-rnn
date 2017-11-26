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

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Executor;
import java.util.concurrent.ForkJoinPool;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.DoubleStream;
import java.util.stream.Stream;

import org.apache.commons.lang3.tuple.Pair;

import se.kth.speech.HashedCollections;
import se.kth.speech.MapCollectors;
import se.kth.speech.NumberTypeConversions;
import se.kth.speech.coin.tangrams.wac.data.Referent;
import se.kth.speech.coin.tangrams.wac.data.Round;
import se.kth.speech.coin.tangrams.wac.data.RoundSet;
import se.kth.speech.coin.tangrams.wac.data.SessionSet;
import se.kth.speech.coin.tangrams.wac.data.Vocabulary;
import weka.classifiers.Classifier;
import weka.classifiers.functions.Logistic;
import weka.core.Attribute;
import weka.core.DenseInstance;
import weka.core.Instance;
import weka.core.Instances;

public class LogisticModel {

	private static class SessionRoundDatum {

		private final String sessionId;

		private final int roundId;

		private final Round round;

		private SessionRoundDatum(final String sessionId, final int roundId, final Round round) {
			this.sessionId = sessionId;
			this.roundId = roundId;
			this.round = round;
		}
	}

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
				final Instance inst = createInstance(ref);
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

	// private Attribute HUE;

	// private Attribute EDGE_COUNT;

	private final Executor asynchronousJobExecutor;

	private ArrayList<Attribute> atts;

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

	/**
	 * Creates an <em>n</em>-best list of possible target referents for a given
	 * {@link Round}.
	 *
	 * @param round
	 *            The {@code Round} to classify the {@code Referent} instances
	 *            thereof.
	 * @return A new {@link ClassificationResult} representing the results.
	 */
	public ClassificationResult rank(final Round round) {
		// NOTE: Values are retrieved directly from the map instead of
		// e.g. assigning them to a final field because it's possible that the
		// map
		// values change at another place in the code and performance isn't an
		// issue here anyway
		final boolean weightByFreq = (Boolean) modelParams.get(ModelParameter.WEIGHT_BY_FREQ);
		final int discount = (Integer) modelParams.get(ModelParameter.DISCOUNT);
		final List<Referent> refs = round.getReferents();
		final String[] words = round.getWords(modelParams).toArray(String[]::new);
		final Map<String, Logistic> wordClassifiers = new HashMap<>(HashedCollections.capacity(words.length));
		int oovObservationCount = 0;
		for (final String word : words) {
			final Logistic wordClassifier = wordModels.getOrDefault(word, discountModel);
			wordClassifiers.put(word, wordClassifier);
			if (wordClassifier.equals(discountModel)) {
				oovObservationCount++;
			}
		}

		final Map<Referent, Double> scores = refs.stream().collect(MapCollectors.toMap(Function.identity(), ref -> {
			final Instance inst = createInstance(ref);
			final DoubleStream wordScores = Arrays.stream(words).mapToDouble(word -> {
				final Logistic wordClassifier = wordClassifiers.get(word);
				double score = score(wordClassifier, inst);
				if (weightByFreq) {
					score *= Math.log10(vocab.getCount(word, discount));
				}
				return score;
			});
			return wordScores.average().orElse(Double.NaN);
		}, refs.size()));
		final List<Referent> ranking = new ArrayList<>(refs);
		ranking.sort(new Comparator<Referent>() {
			@Override
			public int compare(final Referent o1, final Referent o2) {
				return scores.get(o2).compareTo(scores.get(o1));
			}
		});
		return new ClassificationResult(ranking, words, oovObservationCount);
	}

	/**
	 *
	 * @param word
	 *            The word to use for classification.
	 * @param ref
	 *            The {@link Referent} to classify.
	 * @return The score of the given referent being a target referent,
	 *         i.e.&nbsp; the true referent the dialogue participants should be
	 *         referring to in the game in the given round.
	 * @throws ClassificationException
	 *             If an {@link Exception} occurs during
	 *             {@link Classifier#distributionForInstance(Instance)
	 *             classification}.
	 */
	public double score(final String word, final Referent ref) throws ClassificationException {
		return score(word, createInstance(ref));
	}

	/**
	 * Creates a new {@link Instance} representing a given {@link Referent}.
	 *
	 * @param ref
	 *            The {@code Referent} to create an {@code Instance} for.
	 * @return A new {@code Instance}; There is no reason to cache Instance
	 *         values because {@link Instances#add(Instance)} always creates a
	 *         shallow copy thereof anyway, so the only possible benefit of a
	 *         cache would be avoiding the computational cost of object
	 *         construction at the cost of greater memory requirements.
	 */
	private Instance createInstance(final Referent ref) {
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
	 *
	 * @param wordClassifier
	 *            The word to {@link Classifier} to use.
	 * @param inst
	 *            The {@link Instance} to classify.
	 * @return The score of the given referent being a target referent,
	 *         i.e.&nbsp; the true referent the dialogue participants should be
	 *         referring to in the game in the given round.
	 * @throws ClassificationException
	 *             If an {@link Exception} occurs during
	 *             {@link Classifier#distributionForInstance(Instance)
	 *             classification}.
	 */
	private double score(final Classifier wordClassifier, final Instance inst) throws ClassificationException {
		double[] dist;
		try {
			dist = wordClassifier.distributionForInstance(inst);
		} catch (final Exception e) {
			throw new ClassificationException(e);
		}
		return dist[0];
	}

	/**
	 *
	 * @param word
	 *            The word to use for classification.
	 * @param inst
	 *            The {@link Instance} to classify.
	 * @return The score of the given referent being a target referent,
	 *         i.e.&nbsp; the true referent the dialogue participants should be
	 *         referring to in the game in the given round.
	 * @throws ClassificationException
	 *             If an {@link Exception} occurs during
	 *             {@link Classifier#distributionForInstance(Instance)
	 *             classification}.
	 */
	private double score(final String word, final Instance inst) throws ClassificationException {
		final Logistic model = wordModels.getOrDefault(word, discountModel);
		return score(model, inst);
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

	Stream<RoundEvaluationResult> eval(final SessionSet set) {
		final Stream<SessionRoundDatum> sessionRoundData = set.getSessions().stream().map(session -> {
			final String sessionId = session.getName();
			final List<Round> rounds = session.getRounds();
			final List<SessionRoundDatum> roundData = new ArrayList<>(rounds.size());
			final ListIterator<Round> roundIter = rounds.listIterator();
			while (roundIter.hasNext()) {
				final Round round = roundIter.next();
				// Game rounds are 1-indexed, thus calling this after calling
				// "ListIterator.next()" rather than before
				final int roundId = roundIter.nextIndex();
				roundData.add(new SessionRoundDatum(sessionId, roundId, round));
			}
			return roundData;
		}).flatMap(List::stream);

		// NOTE: Values are retrieved directly from the map instead of e.g.
		// assigning them to a final field because it's possible that the map
		// values change at another place in the code and performance isn't an
		// issue here anyway
		final double updateWeight = ((Number) modelParams.get(ModelParameter.UPDATE_WEIGHT)).doubleValue();
		return sessionRoundData.map(sessionRoundDatum -> {
			final Round round = sessionRoundDatum.round;
			final OffsetDateTime classificationStartTime = OffsetDateTime.now();
			final ClassificationResult classificationResult = rank(round);
			// TODO: Currently, this blocks until updating is complete, which
			// could take a long time; Make this asynchronous and return the
			// evaluation results, ensuring to block the NEXT evaluation until
			// updating for THIS iteration is finished
			if (updateWeight > 0.0) {
				updateModel(round);
			}
			return new RoundEvaluationResult(classificationStartTime, sessionRoundDatum.sessionId,
					sessionRoundDatum.roundId, round, classificationResult);
		});
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
