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
import java.util.IdentityHashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import it.unimi.dsi.fastutil.objects.Object2DoubleMap;
import it.unimi.dsi.fastutil.objects.Object2DoubleOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2LongMap;
import it.unimi.dsi.fastutil.objects.Object2LongOpenHashMap;
import se.kth.speech.HashedCollections;
import se.kth.speech.coin.tangrams.wac.data.Referent;
import se.kth.speech.coin.tangrams.wac.data.Round;
import se.kth.speech.coin.tangrams.wac.data.SessionSet;
import se.kth.speech.coin.tangrams.wac.data.Vocabulary;
import se.kth.speech.coin.tangrams.wac.logistic.LogisticModel.FeatureAttributeData;
import se.kth.speech.coin.tangrams.wac.logistic.LogisticModel.Scorer;
import se.kth.speech.coin.tangrams.wac.logistic.LogisticModel.WordClassifiers;
import weka.classifiers.functions.Logistic;
import weka.core.Instance;

public final class RankScorer implements Function<SessionSet, Stream<RankScorer.RoundEvaluationResult>> {

	public static final class RoundEvaluationResult { // NO_UCD (use default)

		private final ClassificationResult classificationResult;

		private final long endNanos;

		private final Round round;

		private final int roundId;

		private final String sessionId;

		private final long startNanos;

		RoundEvaluationResult(final long startNanos, final long endNanos, final String sessionId, final int roundId,
				final Round round, final ClassificationResult classificationResult) {
			this.startNanos = startNanos;
			this.endNanos = endNanos;
			this.sessionId = sessionId;
			this.roundId = roundId;
			this.round = round;
			this.classificationResult = classificationResult;
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see java.lang.Object#equals(java.lang.Object)
		 */
		@Override
		public boolean equals(final Object obj) {
			if (this == obj) {
				return true;
			}
			if (obj == null) {
				return false;
			}
			if (!(obj instanceof RoundEvaluationResult)) {
				return false;
			}
			final RoundEvaluationResult other = (RoundEvaluationResult) obj;
			if (classificationResult == null) {
				if (other.classificationResult != null) {
					return false;
				}
			} else if (!classificationResult.equals(other.classificationResult)) {
				return false;
			}
			if (endNanos != other.endNanos) {
				return false;
			}
			if (round == null) {
				if (other.round != null) {
					return false;
				}
			} else if (!round.equals(other.round)) {
				return false;
			}
			if (roundId != other.roundId) {
				return false;
			}
			if (sessionId == null) {
				if (other.sessionId != null) {
					return false;
				}
			} else if (!sessionId.equals(other.sessionId)) {
				return false;
			}
			if (startNanos != other.startNanos) {
				return false;
			}
			return true;
		}

		/**
		 * @return the classificationResult
		 */
		public ClassificationResult getClassificationResult() {
			return classificationResult;
		}

		/**
		 * @return the endNanos
		 */
		public long getEndNanos() {
			return endNanos;
		}

		/**
		 * @return the round
		 */
		public Round getRound() {
			return round;
		}

		/**
		 * @return the roundId
		 */
		public int getRoundId() {
			return roundId;
		}

		/**
		 * @return the sessionId
		 */
		public String getSessionId() {
			return sessionId;
		}

		/**
		 * @return the startNanos
		 */
		public long getStartNanos() {
			return startNanos;
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see java.lang.Object#hashCode()
		 */
		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + (classificationResult == null ? 0 : classificationResult.hashCode());
			result = prime * result + (int) (endNanos ^ endNanos >>> 32);
			result = prime * result + (round == null ? 0 : round.hashCode());
			result = prime * result + roundId;
			result = prime * result + (sessionId == null ? 0 : sessionId.hashCode());
			result = prime * result + (int) (startNanos ^ startNanos >>> 32);
			return result;
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see java.lang.Object#toString()
		 */
		@Override
		public String toString() {
			final StringBuilder builder = new StringBuilder(512);
			builder.append("RoundEvaluationResult [sessionId=");
			builder.append(sessionId);
			builder.append(", roundId=");
			builder.append(roundId);
			builder.append(", classificationResult=");
			builder.append(classificationResult);
			builder.append(", round=");
			builder.append(round);
			builder.append(", startNanos=");
			builder.append(startNanos);
			builder.append(", endNanos=");
			builder.append(endNanos);
			builder.append("]");
			return builder.toString();
		}

	}

	private static final Consumer<Round> DUMMY_INCREMENTAL_ROUND_TRAINING_UPDATER = round -> {
		// Do nothing
	};

	private static final Logger LOGGER = LoggerFactory.getLogger(RankScorer.class);

	private static final double NULL_WORD_CLASSIFIER_SCORE = Double.NaN;

	private static final long NULL_WORD_OBSERVATION_COUNT = 0L;

	private static Object2DoubleMap<String> createWordClassifierScoreMap(final int expectedTokenTypeCount) {
		final Object2DoubleMap<String> result = new Object2DoubleOpenHashMap<>(expectedTokenTypeCount);
		result.defaultReturnValue(NULL_WORD_CLASSIFIER_SCORE);
		return result;
	}

	private static Object2LongMap<String> createWordObservationCountMap(final int expectedTokenTypeCount) {
		final Object2LongMap<String> result = new Object2LongOpenHashMap<>(expectedTokenTypeCount);
		result.defaultReturnValue(NULL_WORD_OBSERVATION_COUNT);
		return result;
	}

	private static boolean isNullWordClassifierScore(final double score) {
		return Double.isNaN(score);
	}

	private static boolean isNullWordObservationCount(final long count) {
		return NULL_WORD_OBSERVATION_COUNT == count;
	}

	private final LogisticModel model;

	private final Scorer scorer;

	RankScorer(final LogisticModel model, final Scorer scorer) {
		this.model = model;
		this.scorer = scorer;
	}

	@Override
	public Stream<RoundEvaluationResult> apply(final SessionSet set) {
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
		// TODO: Currently, this blocks until updating is complete,
		// which could take a long time; Make this asynchronous and return
		// the evaluation results, ensuring to block the NEXT evaluation
		// until updating for THIS iteration is finished
		final Consumer<Round> incrementalRoundTrainingUpdater = updateWeight > 0.0 ? model::updateModel
				: DUMMY_INCREMENTAL_ROUND_TRAINING_UPDATER;
		return sessionRoundData.flatMap(sessionRoundDatum -> {
			final Round round = sessionRoundDatum.getRound();
			final long startNanos = System.nanoTime();
			final Optional<ClassificationResult> optClassificationResult = rank(round, modelParams);
			return optClassificationResult.map(classificationResult -> {
				incrementalRoundTrainingUpdater.accept(round);
				final long endNanos = System.nanoTime();
				return Stream.of(new RoundEvaluationResult(startNanos, endNanos, sessionRoundDatum.getSessionId(),
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
		final boolean onlyInstructor = (Boolean) modelParams.get(ModelParameter.ONLY_INSTRUCTOR);
		final String[] words = round.getReferringTokens(onlyInstructor).toArray(String[]::new);
		final WordClassifiers wordClassifiers = model.getWordClassifiers();
		final FeatureAttributeData featureAttrs = model.getFeatureAttrs();
		final Vocabulary vocab = model.getVocabulary();

		final Optional<ClassificationResult> result;
		if (words.length < 1) {
			result = Optional.empty();
		} else {
			// NOTE: Values are retrieved directly from the map instead of
			// e.g. assigning them to a final field because it's possible
			// that
			// the map values change at another place in the code and
			// performance isn't
			// an issue here anyway
			final boolean weightByFreq = (Boolean) modelParams.get(ModelParameter.WEIGHT_BY_FREQ);
			final long discountCutoffValue = ((Number) modelParams.get(ModelParameter.DISCOUNT)).longValue();
			final double discountWeightingValue = discountCutoffValue;
			final List<String> oovObservations = new ArrayList<>();
			final Logistic discountClassifier = wordClassifiers.getDiscountClassifier();
			final List<Referent> refs = round.getReferents();
			final Map<Referent, Object2DoubleMap<String>> refWordClassifierScoreMaps = new IdentityHashMap<>(
					HashedCollections.capacity(refs.size()));
			refs.forEach(ref -> refWordClassifierScoreMaps.put(ref, createWordClassifierScoreMap(words.length)));
			// Create an entirely-new map rather than referencing the
			// classifiers so that they can be properly garbage-collected
			// after ranking and before possible updating preceding the next
			// classification
			final Object2LongMap<String> wordObservationCounts = createWordObservationCountMap(words.length);

			final Stream<Weighted<Referent>> scoredRefs = refs.stream().map(ref -> {
				final Instance inst = featureAttrs.createInstance(ref);
				final Object2DoubleMap<String> refWordClassifierScoreMap = refWordClassifierScoreMaps.get(ref);
				final double[] wordScoreArray = new double[words.length];
				for (int i = 0; i < words.length; ++i) {
					final String word = words[i];
					Logistic wordClassifier = wordClassifiers.getWordClassifier(word);
					final String wordClassifierScoreMapKey;
					if (wordClassifier == null) {
						wordClassifier = discountClassifier;
						oovObservations.add(word);
						final long oldDiscountObsCount = wordObservationCounts
								.putIfAbsent(LogisticModel.OOV_CLASS_LABEL, discountCutoffValue);
						assert isNullWordObservationCount(oldDiscountObsCount) ? true
								: oldDiscountObsCount == discountCutoffValue;
						wordClassifierScoreMapKey = LogisticModel.OOV_CLASS_LABEL;
					} else {
						final long oldWordObsCount = wordObservationCounts.computeLongIfAbsent(word, vocab::getCount);
						assert isNullWordObservationCount(oldWordObsCount) ? true
								: oldWordObsCount == vocab.getCount(word);
						wordClassifierScoreMapKey = word;
					}
					double wordScore = scorer.score(wordClassifier, inst);
					if (weightByFreq) {
						final long extantWordObservationCount = wordObservationCounts.getLong(word);
						final double effectiveObsCountValue = isNullWordObservationCount(extantWordObservationCount)
								? discountWeightingValue : extantWordObservationCount;
						wordScore *= Math.log10(effectiveObsCountValue);
					}
					wordScoreArray[i] = wordScore;
					final double oldWordClassifierScore = refWordClassifierScoreMap
							.putIfAbsent(wordClassifierScoreMapKey, wordScore);
					assert isNullWordClassifierScore(oldWordClassifierScore) ? true
							: oldWordClassifierScore == wordScore;
				}
				final double score = Arrays.stream(wordScoreArray).average().getAsDouble();
				return new Weighted<>(ref, score);
			});
			@SuppressWarnings("unchecked")
			final List<Weighted<Referent>> scoredRefList = Arrays.asList(scoredRefs.toArray(Weighted[]::new));
			result = Optional.of(new ClassificationResult(scoredRefList, words, oovObservations,
					refWordClassifierScoreMaps, wordObservationCounts));
		}
		return result;
	}
}