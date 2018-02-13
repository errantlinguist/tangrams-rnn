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

import java.io.IOException;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Function;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import it.unimi.dsi.fastutil.objects.Object2LongMap;
import se.kth.speech.coin.tangrams.wac.data.Referent;
import se.kth.speech.coin.tangrams.wac.data.Round;
import se.kth.speech.coin.tangrams.wac.data.Utterance;
import se.kth.speech.coin.tangrams.wac.logistic.RankScorer.ClassificationResult;

/**
 * Prints cross-validation results. Is thread-safe.
 *
 * @author <a href="mailto:errantlinguist+github@gmail.com">Todd Shore</a>
 * @since 2017-11-26
 *
 */
public final class CrossValidationTablularDataWriter { // NO_UCD (use default)

	// @formatter:off
	public enum Datum implements Function<CrossValidator.Result<RoundEvaluationResult<ClassificationResult>>, String> {
		START_TIME {
			@Override
			public String apply(final CrossValidator.Result<RoundEvaluationResult<ClassificationResult>> cvResult) {
				final RoundEvaluationResult<ClassificationResult> evalResult = cvResult.getEvalResult();
				final long start = evalResult.getStartNanos();
				return Long.toString(start);
			}
		},
		END_TIME {
			@Override
			public String apply(final CrossValidator.Result<RoundEvaluationResult<ClassificationResult>> cvResult) {
				final RoundEvaluationResult<ClassificationResult> evalResult = cvResult.getEvalResult();
				final long end = evalResult.getEndNanos();
				return Long.toString(end);
			}
		},
		CROSS_VALIDATION_ITER {

			@Override
			public String apply(final CrossValidator.Result<RoundEvaluationResult<ClassificationResult>> cvResult) {
				return Integer.toString(cvResult.getCrossValidationIteration());
			}

		},
		DYAD {

			@Override
			public String apply(final CrossValidator.Result<RoundEvaluationResult<ClassificationResult>> cvResult) {
				final RoundEvaluationResult<ClassificationResult> evalResult = cvResult.getEvalResult();
				return evalResult.getSessionId();
			}

		},
		ROUND {

			@Override
			public String apply(final CrossValidator.Result<RoundEvaluationResult<ClassificationResult>> cvResult) {
				final RoundEvaluationResult<ClassificationResult> evalResult = cvResult.getEvalResult();
				return Integer.toString(evalResult.getRoundId());
			}

		},
		RANK {

			private final Comparator<Weighted<Referent>> refScoreComparator = createComparator();

			@Override
			public String apply(final CrossValidator.Result<RoundEvaluationResult<ClassificationResult>> cvResult) {
				final RoundEvaluationResult<ClassificationResult> evalResult = cvResult.getEvalResult();
				final ClassificationResult classificationResult = evalResult.getClassificationResult();
				final List<Weighted<Referent>> scoredRefs = classificationResult.getScoredReferents();
				scoredRefs.sort(refScoreComparator);
				final int targetRank = targetRank(scoredRefs.iterator());
				return Integer.toString(targetRank);
			}

			private Comparator<Weighted<Referent>> createComparator() {
				final Comparator<Weighted<Referent>> naturalOrder = Comparator.naturalOrder();
				return naturalOrder.reversed();
			}

		},
		SCORE {

			@Override
			public String apply(final CrossValidator.Result<RoundEvaluationResult<ClassificationResult>> cvResult) {
				final RoundEvaluationResult<ClassificationResult> evalResult = cvResult.getEvalResult();
				final Round round = evalResult.getRound();
				return Integer.toString(round.getScore());
			}

		},
		ROUND_START_TIME {

			@Override
			public String apply(final CrossValidator.Result<RoundEvaluationResult<ClassificationResult>> cvResult) {
				final RoundEvaluationResult<ClassificationResult> evalResult = cvResult.getEvalResult();
				final Round round = evalResult.getRound();
				return Float.toString(round.getTime());
			}

		},
		UTT_COUNT {

			@Override
			public String apply(final CrossValidator.Result<RoundEvaluationResult<ClassificationResult>> cvResult) {
				final RoundEvaluationResult<ClassificationResult> evalResult = cvResult.getEvalResult();
				final Round round = evalResult.getRound();
				final List<Utterance> utts = round.getUtts();
				return Integer.toString(utts.size());
			}

		},
		REFERRING_TOKEN_COUNT {

			@Override
			public String apply(final CrossValidator.Result<RoundEvaluationResult<ClassificationResult>> cvResult) {
				final RoundEvaluationResult<ClassificationResult> evalResult = cvResult.getEvalResult();
				final ClassificationResult classificationResult = evalResult.getClassificationResult();
				final String[] refTokens = classificationResult.getWords();
				final int count = refTokens.length;
				return Integer.toString(count);
				// final Round round = evalResult.getRound();
				// final List<Utterance> utts = round.getUtts();
				// final Stream<String> refTokens =
				// utts.stream().map(Utterance::getReferringTokens).flatMap(List::stream);
				// return Long.toString(refTokens.count());
			}

		},
		REFERRING_TOKENS {

			@Override
			public String apply(final CrossValidator.Result<RoundEvaluationResult<ClassificationResult>> cvResult) {
				final RoundEvaluationResult<ClassificationResult> evalResult = cvResult.getEvalResult();
				final ClassificationResult classificationResult = evalResult.getClassificationResult();
				final String[] refTokens = classificationResult.getWords();
				return Arrays.stream(refTokens).collect(TOKEN_JOINER);
			}

		},
		OOV_TOKENS {

			@Override
			public String apply(final CrossValidator.Result<RoundEvaluationResult<ClassificationResult>> cvResult) {
				final RoundEvaluationResult<ClassificationResult> evalResult = cvResult.getEvalResult();
				final ClassificationResult classificationResult = evalResult.getClassificationResult();
				final String[] oovObservations = classificationResult.getOovObservations();
				return Arrays.stream(oovObservations).collect(TOKEN_JOINER);
			}

		},
		OOV_COUNT {

			@Override
			public String apply(final CrossValidator.Result<RoundEvaluationResult<ClassificationResult>> cvResult) {
				final RoundEvaluationResult<ClassificationResult> evalResult = cvResult.getEvalResult();
				final ClassificationResult classificationResult = evalResult.getClassificationResult();
				final String[] oovObservations = classificationResult.getOovObservations();
				return Integer.toString(oovObservations.length);
			}

		},
		ORIG_TOKEN_COUNT {

			@Override
			public String apply(final CrossValidator.Result<RoundEvaluationResult<ClassificationResult>> cvResult) {
				final RoundEvaluationResult<ClassificationResult> evalResult = cvResult.getEvalResult();
				final Round round = evalResult.getRound();
				final List<Utterance> utts = round.getUtts();
				final Stream<String> tokens = utts.stream().map(Utterance::getTokens).flatMap(List::stream);
				return Long.toString(tokens.count());
			}

		},
		SHAPE {

			@Override
			public String apply(final CrossValidator.Result<RoundEvaluationResult<ClassificationResult>> cvResult) {
				final RoundEvaluationResult<ClassificationResult> evalResult = cvResult.getEvalResult();
				final Round round = evalResult.getRound();
				final List<Referent> refs = round.getReferents();
				assert refs.stream().filter(Referent::isTarget).count() == 1L;
				final Referent targetRef = refs.stream().filter(Referent::isTarget).findAny().get();
				return targetRef.getShape();
			}

		},
		EDGE_COUNT {

			@Override
			public String apply(final CrossValidator.Result<RoundEvaluationResult<ClassificationResult>> cvResult) {
				final RoundEvaluationResult<ClassificationResult> evalResult = cvResult.getEvalResult();
				final Round round = evalResult.getRound();
				final List<Referent> refs = round.getReferents();
				assert refs.stream().filter(Referent::isTarget).count() == 1L;
				final Referent targetRef = refs.stream().filter(Referent::isTarget).findAny().get();
				return Integer.toString(targetRef.getEdgeCount());
			}

		},
		SIZE {

			@Override
			public String apply(final CrossValidator.Result<RoundEvaluationResult<ClassificationResult>> cvResult) {
				final RoundEvaluationResult<ClassificationResult> evalResult = cvResult.getEvalResult();
				final Round round = evalResult.getRound();
				final List<Referent> refs = round.getReferents();
				assert refs.stream().filter(Referent::isTarget).count() == 1L;
				final Referent targetRef = refs.stream().filter(Referent::isTarget).findAny().get();
				return Double.toString(targetRef.getSize());
			}

		},
		RED {

			@Override
			public String apply(final CrossValidator.Result<RoundEvaluationResult<ClassificationResult>> cvResult) {
				final RoundEvaluationResult<ClassificationResult> evalResult = cvResult.getEvalResult();
				final Round round = evalResult.getRound();
				final List<Referent> refs = round.getReferents();
				assert refs.stream().filter(Referent::isTarget).count() == 1L;
				final Referent targetRef = refs.stream().filter(Referent::isTarget).findAny().get();
				return Float.toString(targetRef.getRedLinear());
			}

		},
		GREEN {

			@Override
			public String apply(final CrossValidator.Result<RoundEvaluationResult<ClassificationResult>> cvResult) {
				final RoundEvaluationResult<ClassificationResult> evalResult = cvResult.getEvalResult();
				final Round round = evalResult.getRound();
				final List<Referent> refs = round.getReferents();
				assert refs.stream().filter(Referent::isTarget).count() == 1L;
				final Referent targetRef = refs.stream().filter(Referent::isTarget).findAny().get();
				return Float.toString(targetRef.getGreenLinear());
			}

		},
		BLUE {

			@Override
			public String apply(final CrossValidator.Result<RoundEvaluationResult<ClassificationResult>> cvResult) {
				final RoundEvaluationResult<ClassificationResult> evalResult = cvResult.getEvalResult();
				final Round round = evalResult.getRound();
				final List<Referent> refs = round.getReferents();
				assert refs.stream().filter(Referent::isTarget).count() == 1L;
				final Referent targetRef = refs.stream().filter(Referent::isTarget).findAny().get();
				return Float.toString(targetRef.getBlueLinear());
			}

		},
		HUE {

			@Override
			public String apply(final CrossValidator.Result<RoundEvaluationResult<ClassificationResult>> cvResult) {
				final RoundEvaluationResult<ClassificationResult> evalResult = cvResult.getEvalResult();
				final Round round = evalResult.getRound();
				final List<Referent> refs = round.getReferents();
				assert refs.stream().filter(Referent::isTarget).count() == 1L;
				final Referent targetRef = refs.stream().filter(Referent::isTarget).findAny().get();
				return Float.toString(targetRef.getHue());
			}

		},
		POSITION_X {

			@Override
			public String apply(final CrossValidator.Result<RoundEvaluationResult<ClassificationResult>> cvResult) {
				final RoundEvaluationResult<ClassificationResult> evalResult = cvResult.getEvalResult();
				final Round round = evalResult.getRound();
				final List<Referent> refs = round.getReferents();
				assert refs.stream().filter(Referent::isTarget).count() == 1L;
				final Referent targetRef = refs.stream().filter(Referent::isTarget).findAny().get();
				return Double.toString(targetRef.getPositionX());
			}

		},
		POSITION_Y {

			@Override
			public String apply(final CrossValidator.Result<RoundEvaluationResult<ClassificationResult>> cvResult) {
				final RoundEvaluationResult<ClassificationResult> evalResult = cvResult.getEvalResult();
				final Round round = evalResult.getRound();
				final List<Referent> refs = round.getReferents();
				assert refs.stream().filter(Referent::isTarget).count() == 1L;
				final Referent targetRef = refs.stream().filter(Referent::isTarget).findAny().get();
				return Double.toString(targetRef.getPositionY());
			}

		},
		ONLY_INSTRUCTOR {

			@Override
			public String apply(final CrossValidator.Result<RoundEvaluationResult<ClassificationResult>> cvResult) {
				final Map<ModelParameter, Object> modelParams = cvResult.getModelParams();
				return modelParams.get(ModelParameter.ONLY_INSTRUCTOR).toString();
			}

		},
		RANDOM_SEED {

			@Override
			public String apply(final CrossValidator.Result<RoundEvaluationResult<ClassificationResult>> cvResult) {
				final Map<ModelParameter, Object> modelParams = cvResult.getModelParams();
				return modelParams.get(ModelParameter.RANDOM_SEED).toString();
			}

		},
		TRAINING_SET_SIZE_DISCOUNT {

			@Override
			public String apply(final CrossValidator.Result<RoundEvaluationResult<ClassificationResult>> cvResult) {
				final Map<ModelParameter, Object> modelParams = cvResult.getModelParams();
				return modelParams.get(ModelParameter.TRAINING_SET_SIZE_DISCOUNT).toString();
			}

		},
		UPDATE_WEIGHT {

			@Override
			public String apply(final CrossValidator.Result<RoundEvaluationResult<ClassificationResult>> cvResult) {
				final Map<ModelParameter, Object> modelParams = cvResult.getModelParams();
				return modelParams.get(ModelParameter.UPDATE_WEIGHT).toString();
			}

		},
		WEIGHT_BY_FREQ {

			@Override
			public String apply(final CrossValidator.Result<RoundEvaluationResult<ClassificationResult>> cvResult) {
				final Map<ModelParameter, Object> modelParams = cvResult.getModelParams();
				return modelParams.get(ModelParameter.WEIGHT_BY_FREQ).toString();
			}

		},
//		TARGET_WORD_CLASSIFIER_SCORES {
//
//			@Override
//			public String apply(final CrossValidator.Result<RoundEvaluationResult<ClassificationResult>> cvResult) {
//				final RoundEvaluationResult<ClassificationResult> evalResult = cvResult.getEvalResult();
//				final ClassificationResult classificationResult = evalResult.getClassificationResult();
//				final Map<Referent, Object2DoubleMap<String>> refWordClassifierScoreMaps = classificationResult.getRefWordClassifierScoreMaps();
//				final NavigableMap<String, DoubleList> combinedWordScores = createCombinedWordScoreMap(refWordClassifierScoreMaps, Referent::isTarget);
//				try {
//					return JSON_MAPPER.writeValueAsString(combinedWordScores);
//				} catch (final JsonProcessingException e) {
//					throw new RuntimeException(e);
//				}
//			}
//
//		},
//		NONTARGET_WORD_CLASSIFIER_SCORES {
//
//
//			@Override
//			public String apply(final CrossValidator.Result<RoundEvaluationResult<ClassificationResult>> cvResult) {
//				final RoundEvaluationResult<ClassificationResult> evalResult = cvResult.getEvalResult();
//				final ClassificationResult classificationResult = evalResult.getClassificationResult();
//				final Map<Referent, Object2DoubleMap<String>> refWordClassifierScoreMaps = classificationResult.getRefWordClassifierScoreMaps();
//				final NavigableMap<String, DoubleList> combinedWordScores = createCombinedWordScoreMap(refWordClassifierScoreMaps, ref -> !ref.isTarget());
//				try {
//					return JSON_MAPPER.writeValueAsString(combinedWordScores);
//				} catch (final JsonProcessingException e) {
//					throw new RuntimeException(e);
//				}
//			}
//
//		},
		BACKGROUND_DATA_WORD_TOKEN_COUNT {
			@Override
			public String apply(final CrossValidator.Result<RoundEvaluationResult<ClassificationResult>> cvResult) {
				final RoundEvaluationResult<ClassificationResult> evalResults = cvResult.getEvalResult();
				final ClassificationResult classificationResult = evalResults.getClassificationResult();
				return Long.toString(classificationResult.getBackgroundDataTokenCount());
			}
		},
		INTERACTION_DATA_WORD_TOKEN_COUNT {
			@Override
			public String apply(final CrossValidator.Result<RoundEvaluationResult<ClassificationResult>> cvResult) {
				final RoundEvaluationResult<ClassificationResult> evalResults = cvResult.getEvalResult();
				final ClassificationResult classificationResult = evalResults.getClassificationResult();
				return Long.toString(classificationResult.getInteractionDataTokenCount());
			}
		},
		WORD_COUNTS {


			@Override
			public String apply(final CrossValidator.Result<RoundEvaluationResult<ClassificationResult>> cvResult) {
				final RoundEvaluationResult<ClassificationResult> evalResult = cvResult.getEvalResult();
				final ClassificationResult classificationResult = evalResult.getClassificationResult();
				final Object2LongMap<String> wordObsCounts = classificationResult.getWordObservationCounts();
				try {
					return JSON_MAPPER.writeValueAsString(new TreeMap<>(wordObsCounts));
				} catch (final JsonProcessingException e) {
					throw new RuntimeException(e);
				}
			}

		};

	}
	// @formatter:on

	private static final ObjectMapper JSON_MAPPER = new ObjectMapper();

	private static final Collector<CharSequence, ?, String> TOKEN_JOINER = Collectors.joining(",");

	private static final CSVFormat FORMAT = CSVFormat.TDF.withHeader(Datum.class);

	// private static NavigableMap<String, DoubleList>
	// createCombinedWordScoreMap(
	// final Map<Referent, Object2DoubleMap<String>> refWordClassifierScoreMaps,
	// final Predicate<? super Referent> refFilter) {
	// @SuppressWarnings("unchecked")
	// final Entry<Referent, Object2DoubleMap<String>>[] targetScores =
	// refWordClassifierScoreMaps.entrySet().stream()
	// .filter(entry -> refFilter.test(entry.getKey())).toArray(Entry[]::new);
	// final NavigableMap<String, DoubleList> result = new TreeMap<>();
	// Arrays.stream(targetScores).map(Entry::getValue).forEach(wordScoreLists
	// -> {
	// for (final Object2DoubleMap.Entry<String> wordScore :
	// wordScoreLists.object2DoubleEntrySet()) {
	// final DoubleList combinedScores =
	// result.computeIfAbsent(wordScore.getKey(),
	// key -> new DoubleArrayList(targetScores.length));
	// combinedScores.add(wordScore.getDoubleValue());
	// }
	// });
	// return result;
	// }

	private static final List<Datum> createDefaultDataToWriteList() {
		return Arrays.asList(Datum.values());
	}

	/**
	 * Returns the rank of the target {@link Referent} in a round.
	 *
	 * @param nbestRefIter
	 *            An {@link Iterator} of <em>n</em>-best target referents.
	 * @return The rank of the true {@link Referent#isTarget() target} referent.
	 */
	private static int targetRank(final Iterator<Weighted<Referent>> nbestRefIter) {
		int rank = 0;
		while (nbestRefIter.hasNext()) {
			final Weighted<Referent> scoredRef = nbestRefIter.next();
			final Referent ref = scoredRef.getWrapped();
			rank++;
			if (ref.isTarget()) {
				return rank;
			}
		}
		throw new IllegalArgumentException("No target referent found.");
	}

	private final List<Datum> dataToWrite;

	private final CSVPrinter printer;

	private final Lock writeLock;

	private CrossValidationTablularDataWriter(final CSVPrinter printer, final List<Datum> dataToWrite) {
		this.printer = printer;
		this.dataToWrite = dataToWrite;
		writeLock = new ReentrantLock();
	}

	CrossValidationTablularDataWriter(final Appendable out) throws IOException {
		this(out, createDefaultDataToWriteList());
	}

	CrossValidationTablularDataWriter(final Appendable out, final List<Datum> dataToWrite) throws IOException {
		this(FORMAT.print(out), dataToWrite);
	}

	public void accept(final CrossValidator.Result<RoundEvaluationResult<ClassificationResult>> input)
			throws IOException { // NO_UCD
		// (use
		// default)
		final List<String> row = Arrays
				.asList(dataToWrite.stream().map(datum -> datum.apply(input)).toArray(String[]::new));
		writeLock.lock();
		try {
			printer.printRecord(row);
		} finally {
			writeLock.unlock();
		}
	}

}