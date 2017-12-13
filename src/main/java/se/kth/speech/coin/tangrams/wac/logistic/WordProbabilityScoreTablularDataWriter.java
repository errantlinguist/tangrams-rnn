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

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Function;
import java.util.function.Supplier;
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

import se.kth.speech.coin.tangrams.wac.data.Round;
import se.kth.speech.coin.tangrams.wac.data.SessionSet;
import se.kth.speech.coin.tangrams.wac.data.SessionSetReader;
import se.kth.speech.coin.tangrams.wac.logistic.WordProbabilityScorer.ReferentWordScore;

/**
 * Prints cross-validation results. Is thread-safe.
 *
 * @author <a href="mailto:tcshore@kth.se">Todd Shore</a>
 * @since 2017-11-26
 *
 */
public final class WordProbabilityScoreTablularDataWriter { // NO_UCD (use
															// default)

	// @formatter:off
	public enum Datum {
		START_TIME {
			@Override
			public String apply(final CrossValidator.Result<RoundEvaluationResult<WordProbabilityScorer.ReferentWordScore[]>> cvResult, final WordProbabilityScorer.ReferentWordScore refWordScore, final int tokenSeqOrdinality) {
				final RoundEvaluationResult<WordProbabilityScorer.ReferentWordScore[]> evalResult = cvResult.getEvalResult();
				final long start = evalResult.getStartNanos();
				return Long.toString(start);
			}
		},
		END_TIME {
			@Override
			public String apply(final CrossValidator.Result<RoundEvaluationResult<WordProbabilityScorer.ReferentWordScore[]>> cvResult, final WordProbabilityScorer.ReferentWordScore refWordScore, final int tokenSeqOrdinality) {
				final RoundEvaluationResult<WordProbabilityScorer.ReferentWordScore[]> evalResult = cvResult.getEvalResult();
				final long end = evalResult.getEndNanos();
				return Long.toString(end);
			}
		},
		CROSS_VALIDATION_ITER {

			@Override
			public String apply(final CrossValidator.Result<RoundEvaluationResult<WordProbabilityScorer.ReferentWordScore[]>> cvResult, final WordProbabilityScorer.ReferentWordScore refWordScore, final int tokenSeqOrdinality) {
				return Integer.toString(cvResult.getCrossValidationIteration());
			}

		},
		DYAD {

			@Override
			public String apply(final CrossValidator.Result<RoundEvaluationResult<WordProbabilityScorer.ReferentWordScore[]>> cvResult, final WordProbabilityScorer.ReferentWordScore refWordScore, final int tokenSeqOrdinality) {
				final RoundEvaluationResult<WordProbabilityScorer.ReferentWordScore[]> evalResult = cvResult.getEvalResult();
				return evalResult.getSessionId();
			}

		},
		ROUND {

			@Override
			public String apply(final CrossValidator.Result<RoundEvaluationResult<WordProbabilityScorer.ReferentWordScore[]>> cvResult, final WordProbabilityScorer.ReferentWordScore refWordScore, final int tokenSeqOrdinality) {
				final RoundEvaluationResult<WordProbabilityScorer.ReferentWordScore[]> evalResult = cvResult.getEvalResult();
				return Integer.toString(evalResult.getRoundId());
			}

		},
		GAME_SCORE {

			@Override
			public String apply(final CrossValidator.Result<RoundEvaluationResult<WordProbabilityScorer.ReferentWordScore[]>> cvResult, final WordProbabilityScorer.ReferentWordScore refWordScore, final int tokenSeqOrdinality) {
				final RoundEvaluationResult<WordProbabilityScorer.ReferentWordScore[]> evalResult = cvResult.getEvalResult();
				final Round round = evalResult.getRound();
				return Integer.toString(round.getScore());
			}

		},
		TOKEN_SEQ_ORDINALITY {

			@Override
			public String apply(final CrossValidator.Result<RoundEvaluationResult<WordProbabilityScorer.ReferentWordScore[]>> cvResult, final WordProbabilityScorer.ReferentWordScore refWordScore, final int tokenSeqOrdinality) {
				return Integer.toString(tokenSeqOrdinality);
			}
		},
		WORD {

			@Override
			public String apply(final CrossValidator.Result<RoundEvaluationResult<WordProbabilityScorer.ReferentWordScore[]>> cvResult, final WordProbabilityScorer.ReferentWordScore refWordScore, final int tokenSeqOrdinality) {
				return refWordScore.getWord();
			}
		},
		WORD_OBS_COUNT {

			@Override
			public String apply(final CrossValidator.Result<RoundEvaluationResult<WordProbabilityScorer.ReferentWordScore[]>> cvResult, final WordProbabilityScorer.ReferentWordScore refWordScore, final int tokenSeqOrdinality) {
				return Long.toString(refWordScore.getWordObsCount());
			}
		},
		IS_OOV {

			@Override
			public String apply(final CrossValidator.Result<RoundEvaluationResult<WordProbabilityScorer.ReferentWordScore[]>> cvResult, final WordProbabilityScorer.ReferentWordScore refWordScore, final int tokenSeqOrdinality) {
				return Boolean.toString(refWordScore.isOov());
			}
		},
		PROBABILITY {

			@Override
			public String apply(final CrossValidator.Result<RoundEvaluationResult<WordProbabilityScorer.ReferentWordScore[]>> cvResult, final WordProbabilityScorer.ReferentWordScore refWordScore, final int tokenSeqOrdinality) {
				return Double.toString(refWordScore.getScore());
			}
		},
		IS_TARGET {

			@Override
			public String apply(final CrossValidator.Result<RoundEvaluationResult<WordProbabilityScorer.ReferentWordScore[]>> cvResult, final WordProbabilityScorer.ReferentWordScore refWordScore, final int tokenSeqOrdinality) {
				return Boolean.toString(refWordScore.getRef().isTarget());
			}
		},
		ROUND_START_TIME {

			@Override
			public String apply(final CrossValidator.Result<RoundEvaluationResult<WordProbabilityScorer.ReferentWordScore[]>> cvResult, final WordProbabilityScorer.ReferentWordScore refWordScore, final int tokenSeqOrdinality) {
				final RoundEvaluationResult<WordProbabilityScorer.ReferentWordScore[]> evalResult = cvResult.getEvalResult();
				final Round round = evalResult.getRound();
				return Float.toString(round.getTime());
			}

		},
		SHAPE {

			@Override
			public String apply(final CrossValidator.Result<RoundEvaluationResult<WordProbabilityScorer.ReferentWordScore[]>> cvResult, final WordProbabilityScorer.ReferentWordScore refWordScore, final int tokenSeqOrdinality) {
				return refWordScore.getRef().getShape();
			}

		},
		EDGE_COUNT {

			@Override
			public String apply(final CrossValidator.Result<RoundEvaluationResult<WordProbabilityScorer.ReferentWordScore[]>> cvResult, final WordProbabilityScorer.ReferentWordScore refWordScore, final int tokenSeqOrdinality) {
				return Integer.toString(refWordScore.getRef().getEdgeCount());
			}

		},
		SIZE {

			@Override
			public String apply(final CrossValidator.Result<RoundEvaluationResult<WordProbabilityScorer.ReferentWordScore[]>> cvResult, final WordProbabilityScorer.ReferentWordScore refWordScore, final int tokenSeqOrdinality) {
				return Double.toString(refWordScore.getRef().getSize());
			}

		},
		RED {

			@Override
			public String apply(final CrossValidator.Result<RoundEvaluationResult<WordProbabilityScorer.ReferentWordScore[]>> cvResult, final WordProbabilityScorer.ReferentWordScore refWordScore, final int tokenSeqOrdinality) {
				return Float.toString(refWordScore.getRef().getRed());
			}

		},
		GREEN {

			@Override
			public String apply(final CrossValidator.Result<RoundEvaluationResult<WordProbabilityScorer.ReferentWordScore[]>> cvResult, final WordProbabilityScorer.ReferentWordScore refWordScore, final int tokenSeqOrdinality) {
				return Float.toString(refWordScore.getRef().getGreen());
			}

		},
		BLUE {

			@Override
			public String apply(final CrossValidator.Result<RoundEvaluationResult<WordProbabilityScorer.ReferentWordScore[]>> cvResult, final WordProbabilityScorer.ReferentWordScore refWordScore, final int tokenSeqOrdinality) {
				return Float.toString(refWordScore.getRef().getBlue());
			}

		},
		HUE {

			@Override
			public String apply(final CrossValidator.Result<RoundEvaluationResult<WordProbabilityScorer.ReferentWordScore[]>> cvResult, final WordProbabilityScorer.ReferentWordScore refWordScore, final int tokenSeqOrdinality) {
				return Float.toString(refWordScore.getRef().getHue());
			}

		},
		POSITION_X {

			@Override
			public String apply(final CrossValidator.Result<RoundEvaluationResult<WordProbabilityScorer.ReferentWordScore[]>> cvResult, final WordProbabilityScorer.ReferentWordScore refWordScore, final int tokenSeqOrdinality) {
				return Double.toString(refWordScore.getRef().getPositionX());
			}

		},
		POSITION_Y {

			@Override
			public String apply(final CrossValidator.Result<RoundEvaluationResult<WordProbabilityScorer.ReferentWordScore[]>> cvResult, final WordProbabilityScorer.ReferentWordScore refWordScore, final int tokenSeqOrdinality) {
				return Double.toString(refWordScore.getRef().getPositionY());
			}

		},
		MID_X {

			@Override
			public String apply(final CrossValidator.Result<RoundEvaluationResult<WordProbabilityScorer.ReferentWordScore[]>> cvResult, final WordProbabilityScorer.ReferentWordScore refWordScore, final int tokenSeqOrdinality) {
				return Double.toString(refWordScore.getRef().getMidX());
			}

		},
		MID_Y {

			@Override
			public String apply(final CrossValidator.Result<RoundEvaluationResult<WordProbabilityScorer.ReferentWordScore[]>> cvResult, final WordProbabilityScorer.ReferentWordScore refWordScore, final int tokenSeqOrdinality) {
				return Double.toString(refWordScore.getRef().getMidY());
			}

		},
		DISCOUNT {

			@Override
			public String apply(final CrossValidator.Result<RoundEvaluationResult<WordProbabilityScorer.ReferentWordScore[]>> cvResult, final WordProbabilityScorer.ReferentWordScore refWordScore, final int tokenSeqOrdinality) {
				final Map<ModelParameter, Object> modelParams = cvResult.getModelParams();
				return modelParams.get(ModelParameter.DISCOUNT).toString();
			}

		},
		ONLY_INSTRUCTOR {

			@Override
			public String apply(final CrossValidator.Result<RoundEvaluationResult<WordProbabilityScorer.ReferentWordScore[]>> cvResult, final WordProbabilityScorer.ReferentWordScore refWordScore, final int tokenSeqOrdinality) {
				final Map<ModelParameter, Object> modelParams = cvResult.getModelParams();
				return modelParams.get(ModelParameter.ONLY_INSTRUCTOR).toString();
			}

		},
		RANDOM_SEED {

			@Override
			public String apply(final CrossValidator.Result<RoundEvaluationResult<WordProbabilityScorer.ReferentWordScore[]>> cvResult, final WordProbabilityScorer.ReferentWordScore refWordScore, final int tokenSeqOrdinality) {
				final Map<ModelParameter, Object> modelParams = cvResult.getModelParams();
				return modelParams.get(ModelParameter.RANDOM_SEED).toString();
			}

		},
		TRAINING_SET_SIZE_DISCOUNT {

			@Override
			public String apply(final CrossValidator.Result<RoundEvaluationResult<WordProbabilityScorer.ReferentWordScore[]>> cvResult, final WordProbabilityScorer.ReferentWordScore refWordScore, final int tokenSeqOrdinality) {
				final Map<ModelParameter, Object> modelParams = cvResult.getModelParams();
				return modelParams.get(ModelParameter.TRAINING_SET_SIZE_DISCOUNT).toString();
			}

		},
		UPDATE_WEIGHT {

			@Override
			public String apply(final CrossValidator.Result<RoundEvaluationResult<WordProbabilityScorer.ReferentWordScore[]>> cvResult, final WordProbabilityScorer.ReferentWordScore refWordScore, final int tokenSeqOrdinality) {
				final Map<ModelParameter, Object> modelParams = cvResult.getModelParams();
				return modelParams.get(ModelParameter.UPDATE_WEIGHT).toString();
			}

		},
		WEIGHT_BY_FREQ {

			@Override
			public String apply(final CrossValidator.Result<RoundEvaluationResult<WordProbabilityScorer.ReferentWordScore[]>> cvResult, final WordProbabilityScorer.ReferentWordScore refWordScore, final int tokenSeqOrdinality) {
				final Map<ModelParameter, Object> modelParams = cvResult.getModelParams();
				return modelParams.get(ModelParameter.WEIGHT_BY_FREQ).toString();
			}

		};

		public abstract String apply(final CrossValidator.Result<RoundEvaluationResult<WordProbabilityScorer.ReferentWordScore[]>> cvResult, final WordProbabilityScorer.ReferentWordScore refWordScore, int tokenSeqOrdinality);

	}
	// @formatter:on

	private enum Parameter implements Supplier<Option> {
		HELP("?") {
			@Override
			public Option get() {
				return Option.builder(optName).longOpt("help").desc("Prints this message.").build();
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

		protected final String optName;

		private Parameter(final String optName) {
			this.optName = optName;
		}

	}

	private static final CSVFormat FORMAT = CSVFormat.TDF.withHeader(Datum.class);

	private static final Options OPTIONS = createOptions();

	private static final Logger LOGGER = LoggerFactory.getLogger(WordProbabilityScoreTablularDataWriter.class);

	public static void main(final CommandLine cl) throws ParseException, IOException { // NO_UCD
		// (use
		// private)
		if (cl.hasOption(Parameter.HELP.optName)) {
			printHelp();
		} else {
			final Path[] inpaths = cl.getArgList().stream().map(Paths::get).toArray(Path[]::new);
			if (inpaths.length < 1) {
				throw new ParseException("No input paths specified.");
			} else {
				LOGGER.info("Will read sessions from {}.", Arrays.toString(inpaths));
				final Map<ModelParameter, Object> modelParams = ModelParameter.createParamValueMap(cl);
				final Path refTokenFilePath = ((File) cl.getParsedOptionValue(Parameter.REFERRING_TOKENS.optName))
						.toPath();
				final SessionSet set = new SessionSetReader(refTokenFilePath).apply(inpaths);
				LOGGER.info("Will run cross-validation using {} session(s).", set.size());
				final ForkJoinPool executor = ForkJoinPool.commonPool();
				// NOTE: No need to explicitly shut down common pool
				LOGGER.info("Will run cross-validation using a(n) {} instance with a parallelism level of {}.",
						executor.getClass().getSimpleName(), executor.getParallelism());
				final WordProbabilityScoreTablularDataWriter resultWriter = new WordProbabilityScoreTablularDataWriter(
						System.out);
				final Supplier<LogisticModel> modelFactory = () -> new LogisticModel(modelParams, executor);
				final Function<LogisticModel, Function<SessionSet, Stream<RoundEvaluationResult<WordProbabilityScorer.ReferentWordScore[]>>>> evaluatorFactory = model -> model
						.createWordProbabilityScorer();
				final CrossValidator<RoundEvaluationResult<WordProbabilityScorer.ReferentWordScore[]>> crossValidator = new CrossValidator<>(
						modelParams, modelFactory, evaluatorFactory, executor);
				crossValidator.crossValidate(set, evalResult -> {
					try {
						resultWriter.accept(evalResult);
					} catch (final IOException e) {
						throw new UncheckedIOException(e);
					}
				});
			}
		}
	}

	public static void main(final String[] args) throws IOException {
		final CommandLineParser parser = new DefaultParser();
		try {
			final CommandLine cl = parser.parse(OPTIONS, args);
			main(cl);
		} catch (final ParseException e) {
			System.out.println(String.format("An error occurred while parsing the command-line arguments: %s", e));
			printHelp();
		}
	}

	private static final List<Datum> createDefaultDataToWriteList() {
		return Arrays.asList(Datum.values());
	}

	private static Options createOptions() {
		final Options result = new Options();
		Arrays.stream(Parameter.values()).map(Parameter::get).forEach(result::addOption);
		Arrays.stream(ModelParameter.values()).map(ModelParameter::createCLIOptionBuilder).map(Option.Builder::build)
				.forEach(result::addOption);
		return result;
	}

	private static void printHelp() {
		final HelpFormatter formatter = new HelpFormatter();
		formatter.printHelp(WordProbabilityScoreTablularDataWriter.class.getName() + " INPATHS...", OPTIONS);
	}

	private final List<Datum> dataToWrite;

	private final CSVPrinter printer;

	private final Lock writeLock;

	private WordProbabilityScoreTablularDataWriter(final CSVPrinter printer, final List<Datum> dataToWrite) {
		this.printer = printer;
		this.dataToWrite = dataToWrite;
		writeLock = new ReentrantLock();
	}

	WordProbabilityScoreTablularDataWriter(final Appendable out) throws IOException {
		this(out, createDefaultDataToWriteList());
	}

	WordProbabilityScoreTablularDataWriter(final Appendable out, final List<Datum> dataToWrite) throws IOException {
		this(FORMAT.print(out), dataToWrite);
	}

	public void accept(
			final CrossValidator.Result<RoundEvaluationResult<WordProbabilityScorer.ReferentWordScore[]>> input)
			throws IOException { // NO_UCD (use default)
		final RoundEvaluationResult<ReferentWordScore[]> evalResult = input.getEvalResult();
		final WordProbabilityScorer.ReferentWordScore[] refWordScores = evalResult.getClassificationResult();
		int nextTokenSeqOrdinality = 1;
		for (final ReferentWordScore refWordScore : refWordScores) {
			final int tokenSeqOrdinality = nextTokenSeqOrdinality++;
			final List<String> row = Arrays.asList(dataToWrite.stream()
					.map(datum -> datum.apply(input, refWordScore, tokenSeqOrdinality)).toArray(String[]::new));
			writeLock.lock();
			try {
				printer.printRecord(row);
			} finally {
				writeLock.unlock();
			}
		}
	}
}