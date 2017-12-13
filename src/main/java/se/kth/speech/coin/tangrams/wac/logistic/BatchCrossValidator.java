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

import java.io.BufferedWriter;
import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ForkJoinPool;
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
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import se.kth.speech.FileNames;
import se.kth.speech.HashedCollections;
import se.kth.speech.coin.tangrams.wac.data.SessionSet;
import se.kth.speech.coin.tangrams.wac.data.SessionSetReader;
import se.kth.speech.coin.tangrams.wac.logistic.RankScorer.RoundEvaluationResult;

public final class BatchCrossValidator { // NO_UCD (use default)

	private class BatchRunner implements Supplier<Entry<Path, BufferedWriter>> {

		private final int cvIterBatchSize;

		private final Entry<String, Map<ModelParameter, Object>> namedParamSet;

		private final Path outdirPath;

		private final ConcurrentMap<Path, BufferedWriter> outfileWriters;

		private BatchRunner(final Entry<String, Map<ModelParameter, Object>> namedParamSet, final Path outdirPath,
				final ConcurrentMap<Path, BufferedWriter> outfileWriters, final int cvIterBatchSize) {
			this.namedParamSet = namedParamSet;
			this.outdirPath = outdirPath;
			this.outfileWriters = outfileWriters;
			this.cvIterBatchSize = cvIterBatchSize;
		}

		@Override
		public Entry<Path, BufferedWriter> get() {
			final String name = namedParamSet.getKey();
			final Path outfilePath = outdirPath.resolve(FileNames.sanitize(name + ".tsv", "-"));
			LOGGER.info("Will write results of cross-validation test named \"{}\" to \"{}\".", name, outfilePath);
			final Map<ModelParameter, Object> modelParams = namedParamSet.getValue();
			final Supplier<LogisticModel> modelFactory = () -> new LogisticModel(modelParams, executor);
			final Function<LogisticModel, Function<SessionSet, Stream<RoundEvaluationResult>>> evaluatorFactory = model -> model
					.createRankScorer();
			final CrossValidator<RoundEvaluationResult> crossValidator = new CrossValidator<>(modelParams, modelFactory,
					executor, evaluatorFactory, cvIterBatchSize);
			BufferedWriter fileWriter;
			CrossValidationTablularDataWriter resultWriter;
			try {
				fileWriter = createFileWriter(outfilePath);
				resultWriter = new CrossValidationTablularDataWriter(fileWriter);
			} catch (final IOException openException) {
				throw new UncheckedIOException(
						String.format("A(n) %s occurred while opening the writer for file \"%s\" for param set \"%s\".",
								openException.getClass().getSimpleName(), outfilePath, name),
						openException);
			}
			final long startNanos = System.nanoTime();
			crossValidator.crossValidate(set, evalResult -> {
				try {
					resultWriter.accept(evalResult);
				} catch (final IOException writeException) {
					closeExceptionally(fileWriter, outfilePath);
					throw new UncheckedIOException(String.format(
							"A(n) %s occurred while writing the results of cross-validating while using param set \"%s\" to file \"%s\".",
							writeException.getClass().getSimpleName(), name, outfilePath), writeException);
				}
			});
			final long endNanos = System.nanoTime();
			final BigDecimal durationSecs = new BigDecimal(endNanos).subtract(new BigDecimal(startNanos))
					.divide(BILLION);
			LOGGER.info("Finished cross-validation batch \"{}\" in {} second(s).", name, durationSecs);
			return Pair.of(outfilePath, fileWriter);
		}

		private BufferedWriter createFileWriter(final Path outfilePath) throws IOException {
			final BufferedWriter result = Files.newBufferedWriter(outfilePath, OUTFILE_ENCODING,
					StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
			final BufferedWriter oldOutfileWriter = outfileWriters.put(outfilePath, result);
			assert oldOutfileWriter == null;
			return result;
		}

	}

	private enum Parameter implements Supplier<Option> {
		HELP("?") {
			@Override
			public Option get() {
				return Option.builder(optName).longOpt("help").desc("Prints this message.").build();
			}
		},
		MODEL_PARAMS("p") {
			@Override
			public Option get() {
				return Option.builder(optName).longOpt("model-params")
						.desc("The file to read model parameters from for each individual cross-validation test to run.")
						.hasArg().argName("path").type(File.class).required().build();
			}
		},
		OUTDIR("o") {
			@Override
			public Option get() {
				return Option.builder(optName).longOpt("outdir").desc("The directory to write the output file(s) to.")
						.hasArg().argName("path").type(File.class).required().build();
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
			formatter.printHelp(BatchCrossValidator.class.getName() + " INPATHS...", OPTIONS);
		}

		protected final String optName;

		private Parameter(final String optName) {
			this.optName = optName;
		}

	}

	private static final BigDecimal BILLION = new BigDecimal("1000000000");

	private static final Logger LOGGER = LoggerFactory.getLogger(BatchCrossValidator.class);

	private static final Charset OUTFILE_ENCODING = StandardCharsets.UTF_8;

	public static void main(final CommandLine cl) throws ParseException, IOException { // NO_UCD
																						// (use
																						// private)
		if (cl.hasOption(Parameter.HELP.optName)) {
			Parameter.printHelp();
		} else {
			final Path[] inpaths = cl.getArgList().stream().map(Paths::get).toArray(Path[]::new);
			if (inpaths.length < 1) {
				throw new ParseException("No input paths specified.");
			} else {
				LOGGER.info("Will read sessions from {}.", Arrays.toString(inpaths));

				final Path outdirPath = ((File) cl.getParsedOptionValue(Parameter.OUTDIR.optName)).toPath();
				LOGGER.info("Will write results to \"{}\".", outdirPath);

				final Path refTokenFilePath = ((File) cl.getParsedOptionValue(Parameter.REFERRING_TOKENS.optName))
						.toPath();
				final SessionSet set = new SessionSetReader(refTokenFilePath).apply(inpaths);
				LOGGER.info("Will run cross-validation using {} session(s).", set.size());

				final Path batchModelParamFile = ((File) cl.getParsedOptionValue(Parameter.MODEL_PARAMS.optName))
						.toPath();
				final Map<String, Map<ModelParameter, Object>> namedParamSets = new ModelParameterTabularDataReader()
						.apply(batchModelParamFile);
				LOGGER.info("Will run {} cross-validation batch(es).", namedParamSets.size());

				final ForkJoinPool executor = ForkJoinPool.commonPool();
				// NOTE: No need to explicitly shut down common pool
				LOGGER.info("Will run cross-validation using a(n) {} instance with a parallelism level of {}.",
						executor.getClass().getSimpleName(), executor.getParallelism());
				final BatchCrossValidator batchCrossValidator = new BatchCrossValidator(set, executor);
				batchCrossValidator.accept(namedParamSets, Files.createDirectories(outdirPath));
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

	private static void close(final Closeable closable, final Path outfilePath) {
		try {
			closable.close();
		} catch (final IOException e) {
			LOGGER.error(String.format("A(n) %s occurred while trying to close the writer for file \"%s\".",
					e.getClass().getSimpleName(), outfilePath), e);
		}
	}

	private static void closeAll(final Map<Path, ? extends Closeable> outfileWriters) {
		for (final Map.Entry<Path, ? extends Closeable> outfileWriter : outfileWriters.entrySet()) {
			final Path outfilePath = outfileWriter.getKey();
			final Closeable writer = outfileWriter.getValue();
			close(writer, outfilePath);
		}
	}

	private static void closeExceptionally(final Closeable closable, final Path outfilePath) {
		try {
			closable.close();
		} catch (final IOException closeException) {
			LOGGER.error(String.format(
					"A(n) %s occurred while closing the writer for file \"%s\" during handling of another error.",
					closeException.getClass().getSimpleName(), outfilePath), closeException);
		}
	}

	private final ForkJoinPool executor;

	private final SessionSet set;

	public BatchCrossValidator(final SessionSet set, final ForkJoinPool executor) { // NO_UCD
																					// (use
																					// private)
		this.set = set;
		this.executor = executor;
	}

	public void accept(final Map<String, Map<ModelParameter, Object>> namedParamSets, final Path outdirPath) { // NO_UCD
																												// (use
																												// private)
		final ConcurrentMap<Path, BufferedWriter> outfileWriters = new ConcurrentHashMap<>(
				HashedCollections.capacity(namedParamSets.size()));
		final int cvIterBatchSize = calculateCvIterBatchSize(namedParamSets.size());
		LOGGER.info("Max size of each parallel cross-validation batch is {}.", cvIterBatchSize);

		final Stream<BatchRunner> batchRunners = namedParamSets.entrySet().stream()
				.map(namedParamSet -> new BatchRunner(namedParamSet, outdirPath, outfileWriters, cvIterBatchSize));
		try {
			batchRunners.forEach(batchRunner -> {
				final Entry<Path, BufferedWriter> fileWriter = batchRunner.get();
				// Close the outfile writer after successfully
				// finishing the relevant batch job
				final Path outfilePath = fileWriter.getKey();
				LOGGER.info("Finished writing cross-validation batch results file \"{}\".", outfilePath);
				final BufferedWriter writer = fileWriter.getValue();
				try {
					close(writer, outfilePath);
				} finally {
					// Remove the writer from the map of all file
					// writers
					final BufferedWriter oldWriter = outfileWriters.remove(outfilePath);
					assert writer.equals(oldWriter);
				}
			});
			LOGGER.info("Finished {} batch job(s) successfully.", namedParamSets.size());

		} finally {
			// For any writers which were not successfully closed after
			// successful finish of the relevant batch job, try to close them
			closeAll(outfileWriters);
		}
	}

	private int calculateCvIterBatchSize(final int paramSetSize) {
		// NOTE: It's better to run multiple model param tests in parallel
		// rather than have multiple iterations in the same batch run in
		// parallel because the former writes to separate files while the latter
		// writes to the same file, thus causing thread contention.
		// final float maxParallelismPerBatch = executor.getParallelism() /
		// (float) paramSetSize;
		// return
		// Math.max(Math.toIntExact(Math.round(Math.floor(maxParallelismPerBatch
		// / MIN_CROSS_VALIDATION_PARALLELISM))), 1);
		return 1;
	}

}
