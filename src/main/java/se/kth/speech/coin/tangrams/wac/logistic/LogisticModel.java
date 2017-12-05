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
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.Future;
import java.util.function.Supplier;
import java.util.stream.DoubleStream;
import java.util.stream.Stream;

import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import se.kth.speech.HashedCollections;
import se.kth.speech.NumberTypeConversions;
import se.kth.speech.coin.tangrams.wac.data.Referent;
import se.kth.speech.coin.tangrams.wac.data.Round;
import se.kth.speech.coin.tangrams.wac.data.RoundSet;
import se.kth.speech.coin.tangrams.wac.data.SessionSet;
import se.kth.speech.coin.tangrams.wac.data.Vocabulary;
import weka.classifiers.Classifier;
import weka.classifiers.functions.Logistic;
import weka.core.Attribute;
import weka.core.BatchPredictor;
import weka.core.DenseInstance;
import weka.core.Instance;
import weka.core.Instances;

public final class LogisticModel { // NO_UCD (use default)

	public static final class TrainingException extends RuntimeException { // NO_UCD
																			// (use
																			// private)

		/**
		 *
		 */
		private static final long serialVersionUID = -9069805591747785094L;

		private TrainingException(final Exception cause) {
			super(cause);
		}
	}

	private class DiscountModelTrainer implements Callable<String> {
		private final Callable<Entry<String, Logistic>> wordTrainer;

		private DiscountModelTrainer(final Callable<Entry<String, Logistic>> wordTrainer) {
			this.wordTrainer = wordTrainer;
		}

		/*
		 * (non-Javadoc)
		 *
		 * @see java.util.concurrent.Callable#call()
		 */
		@Override
		public String call() throws Exception {
			final Entry<String, Logistic> wordClassifier = wordTrainer.call();
			final String result = wordClassifier.getKey();
			assert OOV_CLASS_LABEL.equals(result);
			discountModel = wordClassifier.getValue();
			return result;
		}
	}

	private enum ReferentFeature {
		BLUE {
			@Override
			protected void setValue(final Instance instance, final Referent ref,
					final Map<ReferentFeature, Attribute> attrMap) {
				instance.setValue(getAttr(attrMap), ref.getBlue());
			}
		},
		GREEN {
			@Override
			protected void setValue(final Instance instance, final Referent ref,
					final Map<ReferentFeature, Attribute> attrMap) {
				instance.setValue(getAttr(attrMap), ref.getGreen());
			}
		},
		// HUE {
		// @Override
		// protected void setValue(final Instance instance, final Referent ref,
		// final Map<ReferentFeature, Attribute> attrMap) {
		// instance.setValue(getAttr(attrMap), ref.getHue());
		// }
		// },
		MID_X {
			@Override
			protected void setValue(final Instance instance, final Referent ref,
					final Map<ReferentFeature, Attribute> attrMap) {
				instance.setValue(getAttr(attrMap), ref.getMidX());
			}
		},
		MID_Y {
			@Override
			protected void setValue(final Instance instance, final Referent ref,
					final Map<ReferentFeature, Attribute> attrMap) {
				instance.setValue(getAttr(attrMap), ref.getMidY());
			}
		},
		POSITION_X {
			@Override
			protected void setValue(final Instance instance, final Referent ref,
					final Map<ReferentFeature, Attribute> attrMap) {
				instance.setValue(getAttr(attrMap), ref.getPositionX());
			}
		},
		POSITION_Y {

			@Override
			protected void setValue(final Instance instance, final Referent ref,
					final Map<ReferentFeature, Attribute> attrMap) {
				instance.setValue(getAttr(attrMap), ref.getPositionY());
			}
		},
		RED {
			@Override
			protected void setValue(final Instance instance, final Referent ref,
					final Map<ReferentFeature, Attribute> attrMap) {
				instance.setValue(getAttr(attrMap), ref.getRed());
			}
		},
		SHAPE {
			@Override
			protected Attribute createAttribute(final List<String> shapeUniqueValues) {
				return new Attribute(name(), shapeUniqueValues);
			}

			@Override
			protected void setValue(final Instance instance, final Referent ref,
					final Map<ReferentFeature, Attribute> attrMap) {
				instance.setValue(getAttr(attrMap), ref.getShape());
			}
		},
		SIZE {
			@Override
			protected void setValue(final Instance instance, final Referent ref,
					final Map<ReferentFeature, Attribute> attrMap) {
				instance.setValue(getAttr(attrMap), ref.getSize());
			}
		},
		TARGET {
			@Override
			protected Attribute createAttribute(final List<String> shapeUniqueValues) {
				return new Attribute(name(), REFERENT_CLASSIFICATION_VALUES);
			}

			@Override
			protected void setValue(final Instance instance, final Referent ref,
					final Map<ReferentFeature, Attribute> attrMap) {
				instance.setValue(getAttr(attrMap), Boolean.toString(ref.isTarget()));
			}
		};

		protected Attribute createAttribute(final List<String> shapeUniqueValues) {
			return new Attribute(name());
		}

		protected final Attribute getAttr(final Map<ReferentFeature, Attribute> attrMap) {
			return attrMap.get(this);
		}

		protected abstract void setValue(Instance instance, Referent ref, Map<ReferentFeature, Attribute> attrMap);
	}

	private static class SessionRoundDatum {

		private final Round round;

		private final int roundId;

		private final String sessionId;

		private SessionRoundDatum(final String sessionId, final int roundId, final Round round) {
			this.sessionId = sessionId;
			this.roundId = roundId;
			this.round = round;
		}
	}

	private class WordClassifierMapPopulator implements Callable<String> {

		private final Callable<Entry<String, Logistic>> wordTrainer;

		private WordClassifierMapPopulator(final Callable<Entry<String, Logistic>> wordTrainer) {
			this.wordTrainer = wordTrainer;
		}

		/*
		 * (non-Javadoc)
		 *
		 * @see java.util.concurrent.Callable#call()
		 */
		@Override
		public String call() throws Exception {
			final Entry<String, Logistic> wordClassifier = wordTrainer.call();
			final String result = wordClassifier.getKey();
			wordModels.put(result, wordClassifier.getValue());
			return result;
		}
	}

	private class WordClassifierTrainer implements Callable<Entry<String, Logistic>> {

		/**
		 * A {@link Supplier} of {@link Weighted} {@link Referent} instances to
		 * use as training examples.
		 */
		private final Supplier<? extends Stream<Weighted<Referent>>> exampleSupplier;

		/**
		 * The weight of each datapoint representing a single observation for
		 * the given word.
		 */
		private final double weight;

		/**
		 * The word to train a {@link Logistic} classifier for.
		 */
		private final String word;

		/**
		 *
		 * @param word
		 *            The word to train a {@link Logistic} classifier for.
		 * @param exampleSupplier
		 *            A {@link Supplier} of {@link Weighted} {@link Referent}
		 *            instances to use as training examples.
		 * @param weight
		 *            The weight of each datapoint representing a single
		 *            observation for the given word.
		 */
		private WordClassifierTrainer(final String word,
				final Supplier<? extends Stream<Weighted<Referent>>> exampleSupplier, final double weight) {
			this.word = word;
			this.exampleSupplier = exampleSupplier;
			this.weight = weight;
		}

		@Override
		public Entry<String, Logistic> call() throws WordClassifierTrainingException {
			final Logistic logistic = new Logistic();
			@SuppressWarnings("unchecked")
			final Weighted<Referent>[] examples = exampleSupplier.get().toArray(Weighted[]::new);
			final Instances dataset = new Instances("Referents", featureAttrs.getValue(), examples.length);
			dataset.setClass(featureAttrs.getKey().get(ReferentFeature.TARGET));

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

	private static final int DEFAULT_EXPECTED_WORD_CLASS_COUNT = 1130;

	private static final Logger LOGGER = LoggerFactory.getLogger(LogisticModel.class);

	private static final String OOV_CLASS_LABEL = "__OUT_OF_VOCABULARY__";

	private static final int POSITIVE_REFERENT_CLASSIFICATION_VALUE_IDX;

	private static final List<String> REFERENT_CLASSIFICATION_VALUES;

	static {
		final String posRefClassificationValue = Boolean.TRUE.toString().intern();
		final String negRefClassificationValue = Boolean.FALSE.toString().intern();
		REFERENT_CLASSIFICATION_VALUES = Arrays.asList(posRefClassificationValue, negRefClassificationValue);
		POSITIVE_REFERENT_CLASSIFICATION_VALUE_IDX = REFERENT_CLASSIFICATION_VALUES.indexOf(posRefClassificationValue);
	}

	private static void consumeTrainedWordClasses(final Iterable<Future<String>> futureTrainedWordClasses)
			throws InterruptedException, ExecutionException {
		for (final Future<String> futureTrainedWordClass : futureTrainedWordClasses) {
			@SuppressWarnings("unused")
			final String trainedWordClass = futureTrainedWordClass.get();
			// Do nothing with the result
		}
	}

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

	private static Entry<Map<ReferentFeature, Attribute>, ArrayList<Attribute>> createFeatureAttributes() {
		return createFeatureAttributes(Arrays.asList(Referent.getShapes().stream().toArray(String[]::new)));
	}

	// private Attribute HUE;

	// private Attribute EDGE_COUNT;

	private static Entry<Map<ReferentFeature, Attribute>, ArrayList<Attribute>> createFeatureAttributes(
			final List<String> shapeUniqueValues) {
		final Map<ReferentFeature, Attribute> attrMap = new EnumMap<>(ReferentFeature.class);
		Arrays.stream(ReferentFeature.values())
				.forEach(feature -> attrMap.put(feature, feature.createAttribute(shapeUniqueValues)));
		final ArrayList<Attribute> attrList = new ArrayList<>(attrMap.values());
		return Pair.of(attrMap, attrList);
	}

	private static Stream<Weighted<Referent>> createWeightedReferents(final Referent[] refs, final double weight) {
		return Arrays.stream(refs).map(ref -> new Weighted<>(ref, weight));
	}

	private static Stream<Weighted<Referent>> createWordClassExamples(final RoundSet rounds, final String word) {
		return rounds.getExampleRounds(word).flatMap(LogisticModel::createClassWeightedReferents);
	}

	private static void debugTrainedWordClasses(final Iterable<Future<String>> futureTrainedWordClasses)
			throws InterruptedException, ExecutionException {
		for (final Future<String> futureTrainedWordClass : futureTrainedWordClasses) {
			final String trainedWordClass = futureTrainedWordClass.get();
			LOGGER.debug("Successfully trained word class \"{}\".", trainedWordClass);
		}
	}

	private Logistic discountModel;

	private Entry<Map<ReferentFeature, Attribute>, ArrayList<Attribute>> featureAttrs;

	private final Map<ModelParameter, Object> modelParams;

	private final ForkJoinPool taskPool;

	private RoundSet trainingSet;

	private Vocabulary vocab;

	private final ConcurrentMap<String, Logistic> wordModels;

	public LogisticModel() {
		this(ModelParameter.createDefaultParamValueMap());
	}

	public LogisticModel(final Map<ModelParameter, Object> modelParams) { // NO_UCD
																			// (use
																			// default)
		this(modelParams, ForkJoinPool.commonPool());
	}

	public LogisticModel(final Map<ModelParameter, Object> modelParams, final ForkJoinPool taskPool) { // NO_UCD
																										// (use
																										// default)
		this(modelParams, taskPool, DEFAULT_EXPECTED_WORD_CLASS_COUNT);
	}

	public LogisticModel(final Map<ModelParameter, Object> modelParams, final ForkJoinPool taskPool, // NO_UCD
																										// (use
																										// private)
			final int expectedWordClassCount) {
		this.modelParams = modelParams;
		this.taskPool = taskPool;
		wordModels = new ConcurrentHashMap<>(HashedCollections.capacity(expectedWordClassCount));
		featureAttrs = createFeatureAttributes();
	}

	/**
	 * Creates a new {@link Instances} representing the given {@link Referent}
	 * instances.
	 *
	 * @param refs
	 *            The {@code Referent} instances to create {@code Instance}
	 *            objects for; Their index in the given {@link List} will be the
	 *            index of their corresponding {@code Instance} objects in the
	 *            result data structure.
	 * @return A new {@code Instances} object containing {@code Instance}
	 *         objects representing each given {@code Referent}.
	 */
	public Instances createInstances(final List<Referent> refs) {
		final Map<ReferentFeature, Attribute> attrMap = featureAttrs.getKey();
		final Instances result = new Instances("Referents", featureAttrs.getValue(), refs.size());
		assert result.numAttributes() == attrMap.size();
		refs.stream().map(this::createInstance).forEachOrdered(result::add);
		assert result.size() == refs.size();
		return result;
	}

	public double getPositiveReferentClassificationConfidence(final double[] dist) {
		return dist[POSITIVE_REFERENT_CLASSIFICATION_VALUE_IDX];
	}

	public Vocabulary getVocabulary() {
		return vocab;
	}

	/**
	 *
	 * @param word
	 *            The word to get the corresponding {@link Classifier} for.
	 * @return The {@code Classifier} instance representing the given word.
	 */
	public Logistic getWordClassifier(final String word) {
		return wordModels.get(word);
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
		final double discount = ((Integer) modelParams.get(ModelParameter.DISCOUNT)).doubleValue();

		final boolean onlyInstructor = (Boolean) modelParams.get(ModelParameter.ONLY_INSTRUCTOR);
		final String[] words = round.getReferringTokens(onlyInstructor).toArray(String[]::new);
		final Map<String, Logistic> wordClassifiers = new HashMap<>(HashedCollections.capacity(words.length));
		final List<String> oovObservations = new ArrayList<>();
		for (final String word : words) {
			final Logistic wordClassifier = wordClassifiers.computeIfAbsent(word,
					k -> wordModels.getOrDefault(k, discountModel));
			if (wordClassifier.equals(discountModel)) {
				oovObservations.add(word);
			}
		}
		assert wordClassifiers.values().stream().noneMatch(Objects::isNull);

		final List<Referent> refs = round.getReferents();
		final Stream<Weighted<Referent>> scoredRefs = refs.stream().map(ref -> {
			final Instance inst = createInstance(ref);
			final DoubleStream wordScores = Arrays.stream(words).mapToDouble(word -> {
				final Logistic wordClassifier = wordClassifiers.get(word);
				assert wordClassifier != null;
				double score = score(wordClassifier, inst);
				if (weightByFreq) {
					final Long seenWordObservationCount = vocab.getCount(word);
					final double effectiveObservationCount = seenWordObservationCount == null ? discount
							: seenWordObservationCount.doubleValue();
					score *= Math.log10(effectiveObservationCount);
				}
				return score;
			});
			final double score = wordScores.average().orElse(Double.NaN);
			return new Weighted<>(ref, score);
		});
		@SuppressWarnings("unchecked")
		final List<Weighted<Referent>> scoredRefList = Arrays.asList(scoredRefs.toArray(Weighted[]::new));
		return new ClassificationResult(scoredRefList, words, oovObservations);
	}

	/**
	 *
	 * @param wordClassifier
	 *            The word {@link Classifier classifier} to use.
	 * @param refs
	 *            The {@link Referent} instances to classify.
	 * @return The scores of the given referent being a target referent and not
	 *         being a target referent.
	 * @throws ClassificationException
	 *             If an {@link Exception} occurs during
	 *             {@link BatchPredictor#distributionForInstances(Instances)
	 *             classification}.
	 */
	public double[][] score(final BatchPredictor wordClassifier, final List<Referent> refs) {
		final Instances insts = createInstances(refs);
		double[][] result;
		try {
			result = wordClassifier.distributionsForInstances(insts);
		} catch (final Exception e) {
			throw new ClassificationException(e);
		}
		return result;
	}

	/**
	 *
	 * @param wordClassifier
	 *            The word {@link Classifier classifier} to use.
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
	public double score(final Classifier wordClassifier, final Referent ref) throws ClassificationException {
		return score(wordClassifier, createInstance(ref));
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
		final Map<ReferentFeature, Attribute> attrMap = featureAttrs.getKey();
		final DenseInstance instance = new DenseInstance(attrMap.size());
		Arrays.stream(ReferentFeature.values()).forEach(feature -> feature.setValue(instance, ref, attrMap));
		assert instance.numAttributes() == attrMap.size();
		return instance;
	}

	/**
	 *
	 * @param wordClassifier
	 *            The word {@link Classifier classifier} to use.
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
			// NOTE: This cannot be (much) slower than
			// "weka.core.BatchPredictor.distributionsForInstances(Instances)"
			// because the class Logistic simply calls the following method for
			// each Instance in a given Instances collection, and creating an
			// Instances is more expensive than a simple e.g. ArrayList
			dist = wordClassifier.distributionForInstance(inst);
		} catch (final Exception e) {
			throw new ClassificationException(e);
		}
		return getPositiveReferentClassificationConfidence(dist);
	}

	// /**
	// *
	// * @param word
	// * The word to use for classification.
	// * @param inst
	// * The {@link Instance} to classify.
	// * @return The score of the given referent being a target referent,
	// * i.e.&nbsp; the true referent the dialogue participants should be
	// * referring to in the game in the given round.
	// * @throws ClassificationException
	// * If an {@link Exception} occurs during
	// * {@link Classifier#distributionForInstance(Instance)
	// * classification}.
	// */
	// private double score(final String word, final Instance inst) throws
	// ClassificationException {
	// final Logistic model = wordModels.get(word);
	// return score(model, inst);
	// }

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
		// Train a model for each word
		final Stream<Callable<String>> wordClassifierTrainingJobs = words.stream()
				.map(word -> new WordClassifierTrainer(word, () -> createWordClassExamples(trainingSet, word), weight))
				.map(wordTrainer -> new WordClassifierMapPopulator(wordTrainer));
		// Train the discount model. NOTE: The discount model should not be in
		// the same map as the classifiers for actually-seen observations
		final Callable<String> discountClassifierTrainingJob = new DiscountModelTrainer(new WordClassifierTrainer(
				OOV_CLASS_LABEL, () -> createDiscountClassExamples(trainingSet, words), weight));
		@SuppressWarnings("unchecked")
		final List<Callable<String>> allJobs = Arrays.asList(Stream
				.concat(wordClassifierTrainingJobs, Stream.of(discountClassifierTrainingJob)).toArray(Callable[]::new));
		// NOTE: "ForkJoinPool.invokeAll(..)" creates a ForkJoinTask for each
		// individual Callable passed to it, which is potentially more efficient
		// than e.g. using "CompletableFuture.runAsync(..)"
		final List<Future<String>> futureTrainedWordClasses = taskPool.invokeAll(allJobs);
		assert futureTrainedWordClasses.size() == allJobs.size();
		try {
			if (LOGGER.isDebugEnabled()) {
				debugTrainedWordClasses(futureTrainedWordClasses);
			} else {
				consumeTrainedWordClasses(futureTrainedWordClasses);
			}
		} catch (InterruptedException | ExecutionException e) {
			throw new TrainingException(e);
		}
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
			final long startNanos = System.nanoTime();
			final ClassificationResult classificationResult = rank(round);
			// TODO: Currently, this blocks until updating is complete, which
			// could take a long time; Make this asynchronous and return the
			// evaluation results, ensuring to block the NEXT evaluation until
			// updating for THIS iteration is finished
			if (updateWeight > 0.0) {
				updateModel(round);
			}
			final long endNanos = System.nanoTime();
			return new RoundEvaluationResult(startNanos, endNanos, sessionRoundDatum.sessionId,
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
		final boolean onlyInstructor = (Boolean) modelParams.get(ModelParameter.ONLY_INSTRUCTOR);
		trainingSet = new RoundSet(set, onlyInstructor);
		vocab = trainingSet.createVocabulary();
		// NOTE: Values are retrieved directly from the map instead of e.g.
		// assigning
		// them to a final field because it's possible that the map values
		// change at another place in the code and performance isn't an issue
		// here anyway
		vocab.prune((Integer) modelParams.get(ModelParameter.DISCOUNT));
		featureAttrs = createFeatureAttributes();
		train(vocab.getWords(), 1.0);
	}

}
