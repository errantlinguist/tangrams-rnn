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
package se.kth.speech.coin.tangrams.wac.data;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.ListIterator;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import se.kth.speech.coin.tangrams.CLIParameters;
import se.kth.speech.function.ThrowingSupplier;

/**
 * @author <a href="mailto:errantlinguist+github@gmail.com">Todd Shore</a>
 * @since 23 Nov 2017
 *
 */
public final class DialogueReferentDescriptionWriter { // NO_UCD (use default)

	private enum Parameter implements Supplier<Option> {
		HELP("?") {
			@Override
			public Option get() {
				return Option.builder(optName).longOpt("help").desc("Prints this message.").build();
			}
		},
		OUTPATH("o") {
			@Override
			public Option get() {
				return Option.builder(optName).longOpt("outpath")
						.desc("The file to write the results to; If this option is not supplied, the standard output stream will be used.")
						.hasArg().argName("path").type(File.class).build();
			}
		},
		REFERRING_TOKENS("t") {
			@Override
			public Option get() {
				return Option.builder(optName).longOpt("referring-tokens")
						.desc("The file to read utterance referring-language mappings from.").hasArg().argName("path")
						.type(File.class).required().build();
			}
		};

		private static final Options OPTIONS = createOptions();

		private static Options createOptions() {
			final Options result = new Options();
			Arrays.stream(Parameter.values()).map(Parameter::get).forEach(result::addOption);
			return result;
		}

		private static void printHelp() {
			final HelpFormatter formatter = new HelpFormatter();
			formatter.printHelp(DialogueReferentDescriptionWriter.class.getName() + " INPATHS...", OPTIONS);
		}

		protected final String optName;

		private Parameter(final String optName) {
			this.optName = optName;
		}

	}

	private enum ReferentDatumExtractor implements Function<Referent, Object> {
		SIZE {

			@Override
			public Object apply(final Referent ref) {
				return ref.getSize();
			}

		},
		SHAPE {

			@Override
			public Object apply(final Referent ref) {
				return ref.getShape();
			}

		},
		RED {

			@Override
			public Object apply(final Referent ref) {
				return ref.getRedLinear();
			}

		},
		GREEN {

			@Override
			public Object apply(final Referent ref) {
				return ref.getGreenLinear();
			}

		},
		BLUE {

			@Override
			public Object apply(final Referent ref) {
				return ref.getBlueLinear();
			}

		},
		HUE {

			@Override
			public Object apply(final Referent ref) {
				return ref.getHue();
			}

		},
		POS_X {

			@Override
			public Object apply(final Referent ref) {
				return ref.getPositionX();
			}

		},
		POS_Y {

			@Override
			public Object apply(final Referent ref) {
				return ref.getPositionY();
			}

		},
		MID_X {

			@Override
			public Object apply(final Referent ref) {
				return ref.getMidX();
			}

		},
		MID_Y {

			@Override
			public Object apply(final Referent ref) {
				return ref.getMidY();
			}

		},
		EDGE_COUNT {

			@Override
			public Object apply(final Referent ref) {
				return ref.getEdgeCount();
			}

		};

		private static final List<ReferentDatumExtractor> ORDERING = Collections.unmodifiableList(createOrderingList());

		private static Stream<String> createColumnNames() {
			return ORDERING.stream().map(ReferentDatumExtractor::toString);
		}

		private static final List<ReferentDatumExtractor> createOrderingList() {
			final List<ReferentDatumExtractor> result = Arrays.asList(SHAPE, EDGE_COUNT, SIZE, RED, GREEN, BLUE, HUE,
					POS_X, POS_Y, MID_X, MID_Y);
			assert result.size() == ReferentDatumExtractor.values().length;
			return result;
		}

		private static Stream<String> createRowCells(final Referent referent) {
			return ORDERING.stream().map(extractor -> extractor.apply(referent)).map(Object::toString);
		}

	}

	private enum RoundDatumExtractor implements Function<RoundDatumExtractor.Context, Object> {
		ROUND {

			@Override
			public Object apply(final RoundDatumExtractor.Context ctx) {
				return ctx.getRoundId();
			}

		},
		SCORE {

			@Override
			public Object apply(final RoundDatumExtractor.Context ctx) {
				return ctx.getRound().getScore();
			}

		},
		TIME {

			@Override
			public Object apply(final RoundDatumExtractor.Context ctx) {
				return ctx.getRound().getTime();
			}

		},
		IS_NEGATIVE {

			@Override
			public Object apply(final RoundDatumExtractor.Context ctx) {
				return ctx.getRound().isNegative();
			}

		},
		DYAD {

			@Override
			public Object apply(final RoundDatumExtractor.Context ctx) {
				return ctx.getDyadId();
			}

		},
		DIALOGUE {

			@Override
			public Object apply(final RoundDatumExtractor.Context ctx) {
				return UTT_DIAG_REPR_FACTORY.apply(ctx.getRound().getUtts().iterator());
			}

		},
		REFERRING_TOKENS {
			@Override
			public Object apply(final RoundDatumExtractor.Context ctx) {
				return ctx.getRound().getUtts().stream().map(Utterance::getReferringTokens).flatMap(List::stream)
						.collect(TOKEN_JOINER);
			}
		};

		private static class Context {

			private final Round round;

			private final int roundId;

			private final String dyadId;

			private Context(final Round round, final int roundId, final String dyadId) {
				this.round = round;
				this.roundId = roundId;
				this.dyadId = dyadId;
			}

			/**
			 * @return the dyadId
			 */
			private String getDyadId() {
				return dyadId;
			}

			/**
			 * @return the round
			 */
			private Round getRound() {
				return round;
			}

			/**
			 * @return the roundId
			 */
			private int getRoundId() {
				return roundId;
			}
		}

		private static final Collector<CharSequence, ?, String> TOKEN_JOINER = Collectors.joining(" ");

		private static final List<RoundDatumExtractor> ORDERING;

		private static final List<RoundDatumExtractor> PRE_REFERENT_COLS;

		private static final List<RoundDatumExtractor> POST_REFERENT_COLS;

		static {
			ORDERING = Collections.unmodifiableList(createOrderingList());
			POST_REFERENT_COLS = Arrays.asList(RoundDatumExtractor.DIALOGUE, RoundDatumExtractor.REFERRING_TOKENS);
			PRE_REFERENT_COLS = Arrays.asList(ORDERING.stream().filter(datum -> !POST_REFERENT_COLS.contains(datum))
					.toArray(RoundDatumExtractor[]::new));
		}

		private static Stream<String> createColumnNames() {
			final Stream<String> preRefCols = PRE_REFERENT_COLS.stream().map(RoundDatumExtractor::toString);
			final Stream<String> refCols = ReferentDatumExtractor.createColumnNames();
			final Stream<String> postRefCols = POST_REFERENT_COLS.stream().map(RoundDatumExtractor::toString);
			return Stream.concat(Stream.concat(preRefCols, refCols), postRefCols);
		}

		private static List<RoundDatumExtractor> createOrderingList() {
			final List<RoundDatumExtractor> result = Arrays.asList(DYAD, ROUND, SCORE, TIME, IS_NEGATIVE, DIALOGUE,
					REFERRING_TOKENS);
			assert result.size() == RoundDatumExtractor.values().length;
			return result;
		}

		private static Stream<String> createRowCells(final Context ctx) {
			final Stream<String> preReferentCells = PRE_REFERENT_COLS.stream().map(datum -> datum.apply(ctx))
					.map(Object::toString);
			final Referent targetRef;
			final Referent[] targetRefs = ctx.getRound().getReferents().stream().filter(Referent::isTarget)
					.toArray(Referent[]::new);
			if (targetRefs.length != 1) {
				throw new IllegalArgumentException(
						String.format("Not exactly one target referent found: %s", Arrays.toString(targetRefs)));
			} else {
				targetRef = targetRefs[0];
			}
			final Stream<String> referentCells = ReferentDatumExtractor.createRowCells(targetRef);
			final Stream<String> postReferentCells = POST_REFERENT_COLS.stream().map(datum -> datum.apply(ctx))
					.map(Object::toString);
			return Stream.concat(Stream.concat(preReferentCells, referentCells), postReferentCells);
		}
	}

	private static final Logger LOGGER = LoggerFactory.getLogger(DialogueReferentDescriptionWriter.class);

	private static final UtteranceDialogueRepresentationStringFactory UTT_DIAG_REPR_FACTORY = new UtteranceDialogueRepresentationStringFactory();

	public static void main(final CommandLine cl) throws IOException, ParseException { // NO_UCD (use private)
		if (cl.hasOption(Parameter.HELP.optName)) {
			Parameter.printHelp();
		} else {
			final Path[] inpaths = cl.getArgList().stream().map(Paths::get).toArray(Path[]::new);
			if (inpaths.length < 1) {
				throw new ParseException("No input paths specified.");
			} else {
				LOGGER.info("Will read sessions from {}.", Arrays.toString(inpaths));
				final ThrowingSupplier<PrintStream, IOException> outStreamGetter = CLIParameters
						.parseOutpath((File) cl.getParsedOptionValue(Parameter.OUTPATH.optName));
				final Path refTokenFilePath = ((File) cl.getParsedOptionValue(Parameter.REFERRING_TOKENS.optName))
						.toPath();
				final SessionSet set = new SessionSetReader(refTokenFilePath)
						.apply(inpaths);
				LOGGER.info("Read {} session(s).", set.size());

				try (CSVPrinter printer = CSVFormat.TDF
						.withHeader(RoundDatumExtractor.createColumnNames().toArray(String[]::new))
						.print(outStreamGetter.get())) {
					set.getSessions().stream().sorted(Comparator.comparing(Session::getName))
							.forEachOrdered(session -> {
								final String dyadId = session.getName();
								LOGGER.info("Writing dyad \"{}\".", dyadId);
								for (final ListIterator<Round> roundIter = session.getRounds().listIterator(); roundIter
										.hasNext();) {
									final Round round = roundIter.next();
									// At this point, any potential pre-game
									// dialogues have
									// been trimmed away, so the IDs here are
									// 1-indexed
									final int roundId = roundIter.nextIndex();
									final RoundDatumExtractor.Context ctx = new RoundDatumExtractor.Context(round,
											roundId, dyadId);
									final Stream<String> rowCells = RoundDatumExtractor.createRowCells(ctx);
									try {
										// https://stackoverflow.com/a/20130475/1391325
										printer.printRecord((Iterable<String>) rowCells::iterator);
									} catch (final IOException e) {
										throw new UncheckedIOException(e);
									}
								}
							});
				}
			}
		}
	}

	public static void main(final String[] args) throws IOException {
		final CommandLineParser parser = new DefaultParser();
		try {
			final CommandLine cl = parser.parse(Parameter.OPTIONS, args);
			main(cl);
		} catch (final ParseException e) {
			System.out.println(String.format("An error occurred while parsing the command-line arguments: %s", e));
			Parameter.printHelp();
		}
	}
}
