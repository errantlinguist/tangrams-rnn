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
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;

import se.kth.speech.coin.tangrams.wac.data.Referent;
import se.kth.speech.coin.tangrams.wac.data.Round;
import se.kth.speech.coin.tangrams.wac.data.Utterance;

public final class RoundEvaluationResultTablularDataWriter {

	private enum Datum implements Function<RoundEvaluationResult, String> {
		CLASSIFICATION_START {
			@Override
			public String apply(final RoundEvaluationResult input) {
				final OffsetDateTime classificationStartTime = input.getClassificationStartTime();
				return TIMESTAMP_FORMATTER.format(classificationStartTime);
			}
		},
		DYAD {

			@Override
			public String apply(final RoundEvaluationResult input) {
				return input.getSessionId();
			}

		},
		ROUND {

			@Override
			public String apply(final RoundEvaluationResult input) {
				return Integer.toString(input.getRoundId());
			}

		},
		RANK {

			@Override
			public String apply(final RoundEvaluationResult evalResult) {
				final ClassificationResult classificationResult = evalResult.getClassificationResult();
				final List<Referent> ranking = classificationResult.getRanking();
				final int targetRank = targetRank(ranking.iterator());
				return Integer.toString(targetRank);
			}

		},
		SCORE {

			@Override
			public String apply(final RoundEvaluationResult evalResult) {
				final Round round = evalResult.getRound();
				return Integer.toString(round.getScore());
			}

		},
		ROUND_START_TIME {

			@Override
			public String apply(final RoundEvaluationResult evalResult) {
				final Round round = evalResult.getRound();
				return Float.toString(round.getTime());
			}

		},
		UTT_COUNT {

			@Override
			public String apply(final RoundEvaluationResult evalResult) {
				final Round round = evalResult.getRound();
				final List<Utterance> utts = round.getUtts();
				return Integer.toString(utts.size());
			}

		},
		REFERRING_TOKEN_COUNT {

			@Override
			public String apply(final RoundEvaluationResult evalResult) {
				final ClassificationResult classificationResult = evalResult.getClassificationResult();
				final int count = classificationResult.getWords().length;
				return Integer.toString(count);
				// final Round round = evalResult.getRound();
				// final List<Utterance> utts = round.getUtts();
				// final Stream<String> refTokens =
				// utts.stream().map(Utterance::getReferringTokens).flatMap(List::stream);
				// return Long.toString(refTokens.count());
			}

		},
		REFERRING_TOKEN_TYPES {

			@Override
			public String apply(final RoundEvaluationResult evalResult) {
				final Round round = evalResult.getRound();
				final List<Utterance> utts = round.getUtts();
				final Stream<String> refTokens = utts.stream().map(Utterance::getReferringTokens).flatMap(List::stream);
				return refTokens.distinct().sorted().collect(TOKEN_JOINER);
			}

		},
		OOV_COUNT {

			@Override
			public String apply(final RoundEvaluationResult evalResult) {
				final ClassificationResult classificationResult = evalResult.getClassificationResult();
				return Integer.toString(classificationResult.getOovObservationCount());
			}

		},
		ORIG_TOKEN_COUNT {

			@Override
			public String apply(final RoundEvaluationResult evalResult) {
				final Round round = evalResult.getRound();
				final List<Utterance> utts = round.getUtts();
				final Stream<String> tokens = utts.stream().map(Utterance::getTokens).flatMap(List::stream);
				return Long.toString(tokens.count());
			}

		}, SHAPE{

			@Override
			public String apply(RoundEvaluationResult evalResult) {
				final Round round = evalResult.getRound();
				final List<Referent> refs = round.getReferents();
				assert refs.stream().filter(Referent::isTarget).count() == 1L;
				final Referent targetRef = refs.stream().filter(Referent::isTarget).findAny().get();
				return targetRef.getShape();
			}
			
		}, EDGE_COUNT{

			@Override
			public String apply(RoundEvaluationResult evalResult) {
				final Round round = evalResult.getRound();
				final List<Referent> refs = round.getReferents();
				assert refs.stream().filter(Referent::isTarget).count() == 1L;
				final Referent targetRef = refs.stream().filter(Referent::isTarget).findAny().get();
				return Integer.toString(targetRef.getEdgeCount());
			}
			
		}, SIZE{

			@Override
			public String apply(RoundEvaluationResult evalResult) {
				final Round round = evalResult.getRound();
				final List<Referent> refs = round.getReferents();
				assert refs.stream().filter(Referent::isTarget).count() == 1L;
				final Referent targetRef = refs.stream().filter(Referent::isTarget).findAny().get();
				return Double.toString(targetRef.getSize());
			}
			
		}, RED{

			@Override
			public String apply(RoundEvaluationResult evalResult) {
				final Round round = evalResult.getRound();
				final List<Referent> refs = round.getReferents();
				assert refs.stream().filter(Referent::isTarget).count() == 1L;
				final Referent targetRef = refs.stream().filter(Referent::isTarget).findAny().get();
				return Float.toString(targetRef.getRed());
			}
			
		}, GREEN{

			@Override
			public String apply(RoundEvaluationResult evalResult) {
				final Round round = evalResult.getRound();
				final List<Referent> refs = round.getReferents();
				assert refs.stream().filter(Referent::isTarget).count() == 1L;
				final Referent targetRef = refs.stream().filter(Referent::isTarget).findAny().get();
				return Float.toString(targetRef.getGreen());
			}
			
		}, BLUE{

			@Override
			public String apply(RoundEvaluationResult evalResult) {
				final Round round = evalResult.getRound();
				final List<Referent> refs = round.getReferents();
				assert refs.stream().filter(Referent::isTarget).count() == 1L;
				final Referent targetRef = refs.stream().filter(Referent::isTarget).findAny().get();
				return Float.toString(targetRef.getBlue());
			}
			
		}, HUE{

			@Override
			public String apply(RoundEvaluationResult evalResult) {
				final Round round = evalResult.getRound();
				final List<Referent> refs = round.getReferents();
				assert refs.stream().filter(Referent::isTarget).count() == 1L;
				final Referent targetRef = refs.stream().filter(Referent::isTarget).findAny().get();
				return Float.toString(targetRef.getHue());
			}
			
		}, POSITION_X{

			@Override
			public String apply(RoundEvaluationResult evalResult) {
				final Round round = evalResult.getRound();
				final List<Referent> refs = round.getReferents();
				assert refs.stream().filter(Referent::isTarget).count() == 1L;
				final Referent targetRef = refs.stream().filter(Referent::isTarget).findAny().get();
				return Double.toString(targetRef.getPositionX());
			}
			
		}, POSITION_Y{

			@Override
			public String apply(RoundEvaluationResult evalResult) {
				final Round round = evalResult.getRound();
				final List<Referent> refs = round.getReferents();
				assert refs.stream().filter(Referent::isTarget).count() == 1L;
				final Referent targetRef = refs.stream().filter(Referent::isTarget).findAny().get();
				return Double.toString(targetRef.getPositionY());
			}
			
		};

//		private static final List<Datum> ORDERING = createOrderingList();
//
//		private static List<Datum> createOrderingList() {
//			final List<Datum> result = Arrays.asList(CLASSIFICATION_START, DYAD, ROUND, ROUND_START_TIME, SCORE, RANK,
//					UTT_COUNT, ORIG_TOKEN_COUNT, REFERRING_TOKEN_COUNT, REFERRING_TOKEN_TYPES, OOV_COUNT);
//			assert result.size() == Datum.values().length;
//			return result;
//		}
	}

	private static final Collector<CharSequence, ?, String> TOKEN_JOINER = Collectors.joining(",");

	private static final CSVFormat FORMAT = CSVFormat.TDF.withHeader(Datum.class);

	private static final DateTimeFormatter TIMESTAMP_FORMATTER = DateTimeFormatter.ISO_OFFSET_DATE_TIME;

	/**
	 * Returns the rank of the target {@link Referent} in a round.
	 *
	 * @param nbestRefIter
	 *            An {@link Iterator} of <em>n</em>-best target referents.
	 * @return The rank of the true {@link Referent#isTarget() target} referent.
	 */
	private static int targetRank(final Iterator<Referent> nbestRefIter) {
		int rank = 0;
		while (nbestRefIter.hasNext()) {
			final Referent ref = nbestRefIter.next();
			rank++;
			if (ref.isTarget()) {
				return rank;
			}
		}
		throw new IllegalArgumentException("No target referent found.");
	}

	private final CSVPrinter printer;

	private RoundEvaluationResultTablularDataWriter(final CSVPrinter printer) {
		this.printer = printer;
	}

	RoundEvaluationResultTablularDataWriter(final Appendable out) throws IOException {
		this(FORMAT.print(out));
	}

	public void accept(final RoundEvaluationResult input) throws IOException {
		final Stream<String> row = Arrays.stream(Datum.values()).map(datum -> datum.apply(input));
		printer.printRecord((Iterable<String>) row::iterator);
	}

}