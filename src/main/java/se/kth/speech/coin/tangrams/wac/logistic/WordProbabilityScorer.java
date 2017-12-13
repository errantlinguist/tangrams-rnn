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
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import se.kth.speech.coin.tangrams.wac.data.Referent;
import se.kth.speech.coin.tangrams.wac.data.Round;
import se.kth.speech.coin.tangrams.wac.data.SessionSet;
import se.kth.speech.coin.tangrams.wac.data.Vocabulary;
import se.kth.speech.coin.tangrams.wac.logistic.LogisticModel.FeatureAttributeData;
import se.kth.speech.coin.tangrams.wac.logistic.LogisticModel.Scorer;
import se.kth.speech.coin.tangrams.wac.logistic.LogisticModel.WordClassifiers;
import weka.classifiers.functions.Logistic;
import weka.core.Instance;

public final class WordProbabilityScorer
		implements Function<SessionSet, Stream<RoundEvaluationResult<WordProbabilityScorer.ReferentWordScore[]>>> {

	public static final class ReferentWordScore {

		private final boolean isOov;

		private final Referent ref;

		private final double score;

		private final String word;

		private final long wordObsCount;

		private ReferentWordScore(final Referent ref, final String word, final boolean isOov, final long wordObsCount,
				final double score) {
			this.ref = ref;
			this.word = word;
			this.isOov = isOov;
			this.wordObsCount = wordObsCount;
			this.score = score;
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
			if (!(obj instanceof ReferentWordScore)) {
				return false;
			}
			final ReferentWordScore other = (ReferentWordScore) obj;
			if (isOov != other.isOov) {
				return false;
			}
			if (ref == null) {
				if (other.ref != null) {
					return false;
				}
			} else if (!ref.equals(other.ref)) {
				return false;
			}
			if (Double.doubleToLongBits(score) != Double.doubleToLongBits(other.score)) {
				return false;
			}
			if (word == null) {
				if (other.word != null) {
					return false;
				}
			} else if (!word.equals(other.word)) {
				return false;
			}
			if (wordObsCount != other.wordObsCount) {
				return false;
			}
			return true;
		}

		/**
		 * @return the ref
		 */
		public Referent getRef() {
			return ref;
		}

		/**
		 * @return the score
		 */
		public double getScore() {
			return score;
		}

		/**
		 * @return the word
		 */
		public String getWord() {
			return word;
		}

		/**
		 * @return the wordObsCount
		 */
		public long getWordObsCount() {
			return wordObsCount;
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
			result = prime * result + (isOov ? 1231 : 1237);
			result = prime * result + (ref == null ? 0 : ref.hashCode());
			long temp;
			temp = Double.doubleToLongBits(score);
			result = prime * result + (int) (temp ^ temp >>> 32);
			result = prime * result + (word == null ? 0 : word.hashCode());
			result = prime * result + (int) (wordObsCount ^ wordObsCount >>> 32);
			return result;
		}

		/**
		 * @return the isOov
		 */
		public boolean isOov() {
			return isOov;
		}

		/*
		 * (non-Javadoc)
		 *
		 * @see java.lang.Object#toString()
		 */
		@Override
		public String toString() {
			final StringBuilder builder = new StringBuilder(256);
			builder.append("ReferentWordScore [ref=");
			builder.append(ref);
			builder.append(", word=");
			builder.append(word);
			builder.append(", isOov=");
			builder.append(isOov);
			builder.append(", wordObsCount=");
			builder.append(wordObsCount);
			builder.append(", score=");
			builder.append(score);
			builder.append("]");
			return builder.toString();
		}

	}

	private static final Consumer<Round> DUMMY_INCREMENTAL_ROUND_TRAINING_UPDATER = round -> {
		// Do nothing
	};

	private static final Logger LOGGER = LoggerFactory.getLogger(WordProbabilityScorer.class);

	private static boolean isNullWordObservationCount(final long count) {
		return count < 1;
	}

	private final LogisticModel model;

	private final Scorer scorer;

	WordProbabilityScorer(final LogisticModel model, final Scorer scorer) {
		this.model = model;
		this.scorer = scorer;
	}

	@Override
	public Stream<RoundEvaluationResult<ReferentWordScore[]>> apply(final SessionSet set) {
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
			final Optional<Stream<ReferentWordScore>> optClassificationResultStream = score(round, modelParams);
			return optClassificationResultStream.map(classificationResultStream -> {
				final ReferentWordScore[] classificationResult = classificationResultStream
						.toArray(ReferentWordScore[]::new);
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

	private Optional<Stream<ReferentWordScore>> score(final Round round,
			final Map<ModelParameter, Object> modelParams) {
		final boolean onlyInstructor = (Boolean) modelParams.get(ModelParameter.ONLY_INSTRUCTOR);
		final List<String> words = Arrays.asList(round.getReferringTokens(onlyInstructor).toArray(String[]::new));
		final WordClassifiers wordClassifiers = model.getWordClassifiers();
		final FeatureAttributeData featureAttrs = model.getFeatureAttrs();
		final Vocabulary vocab = model.getVocabulary();

		final Optional<Stream<ReferentWordScore>> result;
		if (words.size() < 1) {
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
			final Logistic discountClassifier = wordClassifiers.getDiscountClassifier();
			final List<Referent> refs = round.getReferents();
			result = Optional.of(refs.stream().flatMap(ref -> {
				final Instance inst = featureAttrs.createInstance(ref);
				return words.stream().map(word -> {
					Logistic wordClassifier = wordClassifiers.getWordClassifier(word);
					final boolean isOov;
					final long wordObsCount;
					if (isOov = wordClassifier == null) {
						wordClassifier = discountClassifier;
						wordObsCount = discountCutoffValue;
					} else {
						wordObsCount = vocab.getCount(word);
					}
					double wordScore = scorer.score(wordClassifier, inst);
					if (weightByFreq) {
						final long extantWordObservationCount = vocab.getCount(word);
						final double effectiveObsCountValue = isNullWordObservationCount(extantWordObservationCount)
								? discountWeightingValue : extantWordObservationCount;
						wordScore *= Math.log10(effectiveObsCountValue);
					}
					return new ReferentWordScore(ref, word, isOov, wordObsCount, wordScore);
				});
			}));
		}
		return result;
	}
}