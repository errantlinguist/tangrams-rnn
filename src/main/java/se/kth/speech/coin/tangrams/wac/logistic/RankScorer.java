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
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.DoubleStream;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import it.unimi.dsi.fastutil.objects.Object2LongMap;
import it.unimi.dsi.fastutil.objects.Object2LongOpenHashMap;
import se.kth.speech.coin.tangrams.wac.data.Referent;
import se.kth.speech.coin.tangrams.wac.data.Round;
import se.kth.speech.coin.tangrams.wac.data.SessionSet;
import se.kth.speech.coin.tangrams.wac.data.Vocabulary;
import se.kth.speech.coin.tangrams.wac.logistic.LogisticModel.Scorer;
import se.kth.speech.coin.tangrams.wac.logistic.LogisticModel.TrainingData;
import se.kth.speech.coin.tangrams.wac.logistic.LogisticModel.WordClassifiers;
import weka.classifiers.functions.Logistic;
import weka.core.Instances;

public final class RankScorer
		implements Function<SessionSet, Stream<RoundEvaluationResult<RankScorer.ClassificationResult>>> {

	public static final class ClassificationResult {

		/**
		 * The number of tokens used for initial training, before any updating.
		 */
		private final long backgroundDataTokenCount;

		/**
		 * The number of tokens added during updating thus far.
		 */
		private final long interactionDataTokenCount;

		/**
		 * The words which were encountered during classification for which no
		 * trained model could be found, thus using the discount model for them
		 * instead.
		 */
		private final String[] oovObservations;

		/**
		 * A list of {@link Weighted} instances representing the confidence
		 * score of each {@link Referent} being the target referent for the
		 * given game round.
		 */
		private final List<Weighted<Referent>> scoredReferents;

		/**
		 * The counts of observations of each word used for classification in
		 * the dataset used for training.
		 */
		private final Object2LongMap<String> wordObservationCounts;

		/**
		 * The strings used for choosing word classifiers during classification.
		 */
		private final List<String> words;

		/**
		 *
		 * @param scoredReferents
		 *            A list of {@link Weighted} instances representing the
		 *            confidence score of each {@link Referent} being the target
		 *            referent for the given game round.
		 * @param words
		 *            The strings used for choosing word classifiers during
		 *            classification.
		 * @param oovObservations
		 *            The words which were encountered during classification for
		 *            which no trained model could be found, thus using the
		 *            discount model for them instead.
		 * @param wordObservationCounts
		 *            The counts of observations of each word used for
		 *            classification in the dataset used for training.
		 * @param backgroundDataTokenCount
		 *            The number of tokens used for initial training, before any
		 *            updating.
		 * @param interactionDataTokenCount
		 *            The number of tokens added during updating thus far.
		 *
		 */
		private ClassificationResult(final List<Weighted<Referent>> scoredReferents, final List<String> words,
				final String[] oovObservations, final Object2LongMap<String> wordObservationCounts,
				final long backgroundDataTokenCount, final long interactionDataTokenCount) {
			this.scoredReferents = scoredReferents;
			this.words = words;
			this.oovObservations = oovObservations;
			this.wordObservationCounts = wordObservationCounts;
			this.backgroundDataTokenCount = backgroundDataTokenCount;
			this.interactionDataTokenCount = interactionDataTokenCount;
		}

		/**
		 * @return The number of tokens used for initial training, before any
		 *         updating.
		 */
		public long getBackgroundDataTokenCount() {
			return backgroundDataTokenCount;
		}

		/**
		 * @return The number of tokens added during updating thus far.
		 */
		public long getInteractionDataTokenCount() {
			return interactionDataTokenCount;
		}

		/**
		 * @return The words which were encountered during classification for
		 *         which no trained model could be found, thus using the
		 *         discount model for them instead.
		 */
		String[] getOovObservations() {
			return oovObservations;
		}

		/**
		 * @return A list of {@link Weighted} instances representing the
		 *         confidence score of each {@link Referent} being the target
		 *         referent for the given game round.
		 */
		List<Weighted<Referent>> getScoredReferents() {
			return scoredReferents;
		}

		/**
		 * @return The counts of observations of each word used for
		 *         classification in the dataset used for training.
		 */
		Object2LongMap<String> getWordObservationCounts() {
			return wordObservationCounts;
		}

		/**
		 * @return The strings used for choosing word classifiers during
		 *         classification.
		 */
		List<String> getWords() {
			return words;
		}
	}

	private static final Consumer<Round> DUMMY_INCREMENTAL_ROUND_TRAINING_UPDATER = round -> {
		// Do nothing
	};

	private static final Logger LOGGER = LoggerFactory.getLogger(RankScorer.class);

	private static Object2LongMap<String> createWordObservationCountMap(final Collection<String> words,
			final Vocabulary vocab) {
		final Object2LongMap<String> result = new Object2LongOpenHashMap<>(words.size());
		result.defaultReturnValue(0L);
		for (final String word : words) {
			final long oldWordObsCount = result.computeLongIfAbsent(word, vocab::getCount);
			assert oldWordObsCount >= 0L;
		}
		return result;
	}

	private static long sumValues(final Object2LongMap<?> map) {
		long result = 0L;
		for (final long value : map.values()) {
			result += value;
		}
		return result;
	}

	private final LogisticModel model;

	private final Scorer scorer;

	private CompletableFuture<TrainingData> updatedTrainingDataFuture;

	RankScorer(final LogisticModel model, final Scorer scorer) {
		this.model = model;
		this.scorer = scorer;
		updatedTrainingDataFuture = CompletableFuture.completedFuture(model.getTrainingData());
	}

	@Override
	public Stream<RoundEvaluationResult<ClassificationResult>> apply(final SessionSet set) {
		final Stream<SessionRoundDatum> sessionRoundData = set.getSessions().stream().map(session -> {
			final String sessionId = session.getName();
			final List<Round> rounds = session.getRounds();
			final List<SessionRoundDatum> roundData = new ArrayList<>(rounds.size());
			final ListIterator<Round> roundIter = rounds.listIterator();
			while (roundIter.hasNext()) {
				final Round round = roundIter.next();
				// Game rounds are 1-indexed, thus calling this after
				// calling
				// "ListIterator.next()" rather than before
				final int roundId = roundIter.nextIndex();
				roundData.add(new SessionRoundDatum(sessionId, roundId, round));
			}
			return roundData;
		}).flatMap(List::stream);

		final Map<ModelParameter, Object> modelParams = model.getModelParams();
		final double updateWeight = ((Number) modelParams.get(ModelParameter.UPDATE_WEIGHT)).doubleValue();
		final Consumer<Round> incrementalRoundTrainingUpdater = updateWeight > 0.0 ? this::updateModel
				: DUMMY_INCREMENTAL_ROUND_TRAINING_UPDATER;
		return sessionRoundData.flatMap(sessionRoundDatum -> {
			final Round round = sessionRoundDatum.getRound();
			final long startNanos = System.nanoTime();
			final Optional<ClassificationResult> optClassificationResult = rank(round, modelParams);
			return optClassificationResult.map(classificationResult -> {
				incrementalRoundTrainingUpdater.accept(round);
				final long endNanos = System.nanoTime();
				return Stream.of(new RoundEvaluationResult<>(startNanos, endNanos, sessionRoundDatum.getSessionId(),
						sessionRoundDatum.getRoundId(), round, classificationResult));
			}).orElseGet(() -> {
				LOGGER.warn(
						"No referring language found for round {} of session \"{}\"; Skipping classification of that round.",
						sessionRoundDatum.getRoundId(), sessionRoundDatum.getSessionId());
				return Stream.empty();
			});
		});
	}

	/**
	 * Creates an <em>n</em>-best list of possible target referents for a given
	 * {@link Round}.
	 *
	 * @param round
	 *            The {@code Round} to classify the {@code Referent} instances
	 *            thereof.
	 * @return An {@link Optional optional} {@link ClassificationResult}
	 *         representing the results.
	 */
	private Optional<ClassificationResult> rank(final Round round, final Map<ModelParameter, Object> modelParams) {
		final List<String> words = Arrays.asList(round
				.getReferringTokens((Boolean) modelParams.get(ModelParameter.ONLY_INSTRUCTOR)).toArray(String[]::new));
		final Optional<ClassificationResult> result;
		if (words.size() < 1) {
			result = Optional.empty();
		} else {
			final TrainingData trainingData = updatedTrainingDataFuture.join();
			final WordClassifiers wordClassifiers = trainingData.getWordClassifiers();
			final Vocabulary vocab = trainingData.getVocabulary();
			final Object2LongMap<String> wordObservationCounts = createWordObservationCountMap(words, vocab);
			final String[] oovObservations = words.stream().filter(word -> wordObservationCounts.getLong(word) < 1L)
					.toArray(String[]::new);

			final List<Referent> refs = round.getReferents();
			final Instances refInsts = trainingData.getFeatureAttrs().createInstances(refs);
			// NOTE: Values are retrieved directly from the map instead of
			// e.g. assigning them to a final field because it's possible
			// that the map values change at another place in the code and
			// performance isn't an issue here anyway
			final boolean weightByFreq = (Boolean) modelParams.get(ModelParameter.WEIGHT_BY_FREQ);
			// For each word, create an array of scores for each referent
			// NOTE: Logistic instances are not thread-safe!
			final double[][] wordRefScores = words.stream().map(word -> {
				final double[] refScores;

				final Optional<Logistic> optWordClassifier = wordClassifiers.getWordClassifier(word);
				if (optWordClassifier.isPresent()) {
					final Logistic wordClassifier = optWordClassifier.get();
					assert wordClassifier != null;
					final DoubleStream unweightedRefScores = scorer.score(wordClassifier, refInsts);
					if (weightByFreq) {
						final long extantWordObservationCount = wordObservationCounts.getLong(word);
						refScores = unweightedRefScores.map(score -> score * Math.log10(extantWordObservationCount))
								.toArray();
					} else {
						refScores = unweightedRefScores.toArray();
					}
				} else {
					refScores = new double[refInsts.size()];
				}

				return refScores;
			}).toArray(double[][]::new);

			final List<Weighted<Referent>> scoredRefList = new ArrayList<>(refs.size());
			for (final ListIterator<Referent> refIter = refs.listIterator(); refIter.hasNext();) {
				final int refIdx = refIter.nextIndex();
				final Referent ref = refIter.next();
				final Stream<double[]> wordRefScoreArrays = Arrays.stream(wordRefScores);
				// For each row representing a word, get the column representing
				// the score for the given referent
				final DoubleStream wordScores = wordRefScoreArrays
						.mapToDouble(wordRefScoreArray -> wordRefScoreArray[refIdx]);
				final double refScore = wordScores.average().orElse(0.0);
				scoredRefList.add(new Weighted<Referent>(ref, refScore));
			}
			assert refs.size() == scoredRefList.size();

			final Object2LongMap<String> interactiondata = trainingData.getInteractionData();
			result = Optional.of(new ClassificationResult(scoredRefList, words, oovObservations, wordObservationCounts,
					trainingData.getBackgroundDataTokenCount(), sumValues(interactiondata)));
		}
		return result;
	}

	/**
	 * @param round
	 *            The {@link Round} to add to the dataset for training.
	 */
	private void updateModel(final Round round) {
		updatedTrainingDataFuture = model.updateModelAsynchronously(round);
	}
}