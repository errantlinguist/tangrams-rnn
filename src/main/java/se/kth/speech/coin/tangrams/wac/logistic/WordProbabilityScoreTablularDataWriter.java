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
import java.io.PrintStream;
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

import se.kth.speech.coin.tangrams.CLIParameters;
import se.kth.speech.coin.tangrams.wac.data.SessionSet;
import se.kth.speech.coin.tangrams.wac.data.SessionSetReader;
import se.kth.speech.coin.tangrams.wac.logistic.WordProbabilityScorer.ReferentWordScore;
import se.kth.speech.function.ThrowingSupplier;

/**
 * Trains words-as-classifiers model and prints cross-validation results. Is
 * thread-safe.
 *
 * @author <a href="mailto:errantlinguist+github@gmail.com">Todd Shore</a>
 * @since 2017-11-26
 *
 */
public final class WordProbabilityScoreTablularDataWriter { // NO_UCD (use
															// default)

	private enum Parameter implements Supplier<Option> {
		HELP("?") {
			@Override
			public Option get() {
				return Option.builder(optName).longOpt("help").desc("Prints this message.").build();
			}
		},
		OUTFILE("o") {
			@Override
			public Option get() {
				return Option.builder(optName).longOpt("outfile")
						.desc("The path to write the results file to.").hasArg().argName("path").type(File.class)
						.required().build();
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

	private static final CSVFormat FORMAT = CSVFormat.TDF.withHeader(WordProbabilityScoreDatum.class);

	private static final Logger LOGGER = LoggerFactory.getLogger(WordProbabilityScoreTablularDataWriter.class);

	private static final Options OPTIONS = createOptions();

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
				final ThrowingSupplier<PrintStream, IOException> outStreamSupplier = CLIParameters
						.parseOutpath((File) cl.getParsedOptionValue(Parameter.OUTFILE.optName));
				try (PrintStream outStream = outStreamSupplier.get()) {
					final WordProbabilityScoreTablularDataWriter resultWriter = new WordProbabilityScoreTablularDataWriter(
							outStream);
					final Supplier<LogisticModel> modelFactory = () -> new LogisticModel(modelParams, executor);
					final Function<LogisticModel, Function<SessionSet, Stream<RoundEvaluationResult<WordProbabilityScorer.ReferentWordScore[]>>>> evaluatorFactory = model -> model
							.createWordProbabilityScorer();
					final CrossValidator<RoundEvaluationResult<WordProbabilityScorer.ReferentWordScore[]>> crossValidator = new CrossValidator<>(
							modelParams, modelFactory, evaluatorFactory, TestSessions::isTestSession, executor);
					crossValidator.crossValidate(set, evalResult -> {
						try {
							resultWriter.accept(evalResult);
						} catch (final IOException e) {
							throw new UncheckedIOException(e);
						}
					});
				}
				LOGGER.info("Finished writing results of cross-validation.");
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

	private static final List<WordProbabilityScoreDatum> createDefaultDataToWriteList() {
		return Arrays.asList(WordProbabilityScoreDatum.values());
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

	private final List<WordProbabilityScoreDatum> dataToWrite;

	private final CSVPrinter printer;

	private final Lock writeLock;

	private WordProbabilityScoreTablularDataWriter(final CSVPrinter printer,
			final List<WordProbabilityScoreDatum> dataToWrite) {
		this.printer = printer;
		this.dataToWrite = dataToWrite;
		writeLock = new ReentrantLock();
	}

	WordProbabilityScoreTablularDataWriter(final Appendable out) throws IOException {
		this(out, createDefaultDataToWriteList());
	}

	WordProbabilityScoreTablularDataWriter(final Appendable out, final List<WordProbabilityScoreDatum> dataToWrite)
			throws IOException {
		this(FORMAT.print(out), dataToWrite);
	}

	public void accept(
			final CrossValidator.Result<RoundEvaluationResult<WordProbabilityScorer.ReferentWordScore[]>> input)
			throws IOException { // NO_UCD (use default)
		final RoundEvaluationResult<ReferentWordScore[]> evalResult = input.getEvalResult();
		final WordProbabilityScorer.ReferentWordScore[] refWordScores = evalResult.getClassificationResult();
		for (final ReferentWordScore refWordScore : refWordScores) {
			final List<String> row = Arrays
					.asList(dataToWrite.stream().map(datum -> datum.apply(input, refWordScore)).toArray(String[]::new));
			writeLock.lock();
			try {
				printer.printRecord(row);
			} finally {
				writeLock.unlock();
			}
		}
	}
}