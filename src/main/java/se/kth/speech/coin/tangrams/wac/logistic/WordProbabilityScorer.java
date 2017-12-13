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
import java.util.Iterator;
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
import se.kth.speech.coin.tangrams.wac.data.Utterance;
import se.kth.speech.coin.tangrams.wac.data.Vocabulary;
import se.kth.speech.coin.tangrams.wac.logistic.LogisticModel.FeatureAttributeData;
import se.kth.speech.coin.tangrams.wac.logistic.LogisticModel.Scorer;
import se.kth.speech.coin.tangrams.wac.logistic.LogisticModel.TrainingData;
import se.kth.speech.coin.tangrams.wac.logistic.LogisticModel.WordClassifiers;
import weka.classifiers.functions.Logistic;
import weka.core.Instance;

public final class WordProbabilityScorer
		implements Function<SessionSet, Stream<RoundEvaluationResult<WordProbabilityScorer.ReferentWordScore[]>>> {

	public static final class ReferentWordScore {

		private final boolean isInstructor;

		private final boolean isOov;

		private final Referent ref;

		private final double score;

		private final int uttSeqOrdinality;

		private final int tokSeqOrdinality;

		private final String word;

		private final long wordObsCount;

		private ReferentWordScore(final Referent ref, final int uttSeqOrdinality, final int tokSeqOrdinality,
				final String word, final boolean isInstructor, final boolean isOov, final long wordObsCount,
				final double score) {
			this.ref = ref;
			this.uttSeqOrdinality = uttSeqOrdinality;
			this.tokSeqOrdinality = tokSeqOrdinality;
			this.word = word;
			this.isInstructor = isInstructor;
			this.isOov = isOov;
			this.wordObsCount = wordObsCount;
			this.score = score;
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
		 * @return the tokSeqOrdinality
		 */
		public int getTokSeqOrdinality() {
			return tokSeqOrdinality;
		}

		/**
		 * @return the uttSeqOrdinality
		 */
		public int getUttSeqOrdinality() {
			return uttSeqOrdinality;
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

		/**
		 * @return the isInstructor
		 */
		public boolean isInstructor() {
			return isInstructor;
		}

		/**
		 * @return the isOov
		 */
		public boolean isOov() {
			return isOov;
		}

	}

	private static final Consumer<Round> DUMMY_INCREMENTAL_ROUND_TRAINING_UPDATER = round -> {
		// Do nothing
	};

	private static final Logger LOGGER = LoggerFactory.getLogger(WordProbabilityScorer.class);

	private static List<Utterance> createUttList(final Round round, final boolean onlyInstructor) {
		final List<Utterance> allUtts = round.getUtts();
		return onlyInstructor
				? Arrays.asList(allUtts.stream().filter(Utterance::isInstructor).toArray(Utterance[]::new))
				: allUtts;
	}

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
		final List<Utterance> utts = createUttList(round, (Boolean) modelParams.get(ModelParameter.ONLY_INSTRUCTOR));
		final Optional<Stream<ReferentWordScore>> result;
		if (utts.size() < 1) {
			result = Optional.empty();
		} else {
			// NOTE: Values are retrieved directly from the map instead of
			// e.g. assigning them to a final field because it's possible
			// that
			// the map values change at another place in the code and
			// performance isn't
			// an issue here anyway
			final TrainingData trainingData = model.getTrainingData();
			final WordClassifiers wordClassifiers = trainingData.getWordClassifiers();
			final FeatureAttributeData featureAttrs = trainingData.getFeatureAttrs();
			final Vocabulary vocab = trainingData.getVocabulary();
			final boolean weightByFreq = (Boolean) modelParams.get(ModelParameter.WEIGHT_BY_FREQ);
			final long discountCutoffValue = ((Number) modelParams.get(ModelParameter.DISCOUNT)).longValue();
			final double discountWeightingValue = discountCutoffValue;
			final Logistic discountClassifier = wordClassifiers.getDiscountClassifier();
			final List<Referent> refs = round.getReferents();
			result = Optional.of(refs.stream().flatMap(ref -> {
				final Stream.Builder<ReferentWordScore> refWordScores = Stream.builder();
				final Instance inst = featureAttrs.createInstance(ref);

				final ListIterator<Utterance> uttIter = utts.listIterator();
				do {
					// Process each utterance
					final Utterance utt = uttIter.next();
					final int uttSeqOrdinality = uttIter.nextIndex();
					final boolean isInstructor = utt.isInstructor();

					{
						// Process each token in the given utterance
						int tokSeqOrdinality = 0;
						final Iterator<String> wordIter = utt.getReferringTokens().stream().iterator();
						while (wordIter.hasNext()) {
							final String word = wordIter.next();
							tokSeqOrdinality++;
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
								final double effectiveObsCountValue = isNullWordObservationCount(
										extantWordObservationCount) ? discountWeightingValue
												: extantWordObservationCount;
								wordScore *= Math.log10(effectiveObsCountValue);
							}
							final ReferentWordScore refWordScore = new ReferentWordScore(ref, uttSeqOrdinality,
									tokSeqOrdinality, word, isInstructor, isOov, wordObsCount, wordScore);
							refWordScores.add(refWordScore);
						}
					}

				} while (uttIter.hasNext());

				return refWordScores.build();
			}));
		}
		return result;
	}
}