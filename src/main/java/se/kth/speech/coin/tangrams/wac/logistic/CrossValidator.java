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
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.ForkJoinPool;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import se.kth.speech.coin.tangrams.wac.data.Session;
import se.kth.speech.coin.tangrams.wac.data.SessionSet;
import se.kth.speech.coin.tangrams.wac.data.SessionSetReader;

public final class CrossValidator { // NO_UCD (use default)

	public static final class Exception extends RuntimeException { // NO_UCD
																	// (use
																	// private)

		/**
		 *
		 */
		private static final long serialVersionUID = -1636897113752283942L;

		private static final String createMessage(final SessionSet training, final Session testing,
				final java.lang.Exception cause) {
			final Set<String> trainingSessionNames = training.getSessions().stream().map(Session::getName)
					.collect(Collectors.toCollection(() -> new TreeSet<>()));
			return String.format(
					"A(n) %s occurred while cross-validating with a training set of %d session(s) and testing on session \"%s\". Training sets: %s",
					cause, training.size(), testing.getName(), trainingSessionNames);
		}

		private Exception(final SessionSet training, final Session testing, final java.lang.Exception cause) {
			super(createMessage(training, testing, cause), cause);
		}

	}

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

	private static final Options OPTIONS = createOptions();

	private static final Logger LOGGER = LoggerFactory.getLogger(CrossValidator.class);

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
				final CrossValidationTablularDataWriter resultWriter = new CrossValidationTablularDataWriter(
						System.out);
				final Supplier<LogisticModel> modelFactory = () -> new LogisticModel(modelParams, executor);
				final CrossValidator crossValidator = new CrossValidator(modelParams, modelFactory, executor);
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

	private static Options createOptions() {
		final Options result = new Options();
		Arrays.stream(Parameter.values()).map(Parameter::get).forEach(result::addOption);
		Arrays.stream(ModelParameter.values()).map(ModelParameter::createCLIOptionBuilder).map(Option.Builder::build)
				.forEach(result::addOption);
		return result;
	}

	private static void printHelp() {
		final HelpFormatter formatter = new HelpFormatter();
		formatter.printHelp(CrossValidator.class.getName() + " INPATHS...", OPTIONS);
	}

	private final Executor executor;

	private final Supplier<LogisticModel> modelFactory;

	private final Map<ModelParameter, Object> modelParams;

	private final int cvIterBatchSize;

	public CrossValidator(final Map<ModelParameter, Object> modelParams, final Supplier<LogisticModel> modelFactory,
			final Executor executor) {
		this(modelParams, modelFactory, executor, 1);
	}

	public CrossValidator(final Map<ModelParameter, Object> modelParams, final Supplier<LogisticModel> modelFactory, // NO_UCD
																														// (use
																														// default)
			final Executor executor, final int cvIterBatchSize) {
		this.modelParams = modelParams;
		this.modelFactory = modelFactory;
		this.executor = executor;
		this.cvIterBatchSize = cvIterBatchSize;
	}

	/**
	 * Performs cross validation on a {@link SessionSet}.
	 *
	 * @param set
	 *            The {@link SessionSet} to perform cross-validation on.
	 * @param resultHandler
	 *            A {@link Consumer} of individual
	 *            {@link CrossValidationRoundEvaluationResult} objects
	 *            representing the results of testing on a single round in a
	 *            single session.
	 */
	public void crossValidate(final SessionSet set, // NO_UCD (use default)
			final Consumer<? super CrossValidationRoundEvaluationResult> resultHandler) {
		final CompletableFuture<Void> allFinished = CompletableFuture
				.allOf(crossValidateAsynchronously(set, resultHandler).toArray(CompletableFuture[]::new));
		allFinished.join();
	}

	/**
	 * Performs cross validation on a {@link SessionSet} asynchronously.
	 *
	 * @param set
	 *            The {@link SessionSet} to perform cross-validation on.
	 * @param resultHandler
	 *            A {@link Consumer} of individual
	 *            {@link CrossValidationRoundEvaluationResult} objects
	 *            representing the results of testing on a single round in a
	 *            single session.
	 * @return A {@link Stream} of <em>n</em> {@link CompletableFuture}, each of
	 *         which representing the completion of one of <em>n</em>
	 *         cross-validation test iterations, as defined by
	 *         {@link ModelParameter#CROSS_VALIDATION_ITER_COUNT}.
	 */
	public Stream<CompletableFuture<Void>> crossValidateAsynchronously(final SessionSet set, // NO_UCD
																								// (use
																								// private)
			final Consumer<? super CrossValidationRoundEvaluationResult> resultHandler) {
		final int cvIterCount = (Integer) modelParams.get(ModelParameter.CROSS_VALIDATION_ITER_COUNT);
		// Pass the same Random instance to each cross-validation iteration so
		// that each iteration is potentially different
		final long randomSeed = (Long) modelParams.get(ModelParameter.RANDOM_SEED);
		final Random random = new Random(randomSeed);
		final Stream.Builder<CompletableFuture<Void>> cvIterationJobs = Stream.builder();

		final int[][] cvIterBatches = createBatchCrossValidationIterIdArrays(cvIterCount);
		for (final int[] cvIters : cvIterBatches) {
			for (final int cvIter : cvIters) {
				cvIterationJobs.add(CompletableFuture.runAsync(() -> {
					set.crossValidate((training, testing) -> {
						try {
							final LogisticModel model = modelFactory.get();
							model.train(training);
							final LogisticModel.Scorer scorer = model.createScorer();
							final Stream<RoundEvaluationResult> roundEvalResults = scorer.eval(new SessionSet(testing));
							roundEvalResults.map(evalResult -> new CrossValidationRoundEvaluationResult(cvIter,
									evalResult, modelParams)).forEach(resultHandler);
						} catch (final ClassificationException e) {
							throw new Exception(training, testing, e);
						}
					}, modelParams, random);
				}, executor));
			}
		}
		return cvIterationJobs.build();

	}

	private int calculateBatchCount(final int cvIterCount) {
		final double nonIntegralValue = cvIterCount / cvIterBatchSize;
		return nonIntegralValue >= 1.0 ? Math.toIntExact(Math.round(Math.ceil(nonIntegralValue))) : 1;
	}

	private int[][] createBatchCrossValidationIterIdArrays(final int cvIterCount) {
		final int cvBatchCount = calculateBatchCount(cvIterCount);
		final int[][] result = new int[cvBatchCount][];
		// Cross-validation iteration IDs are 1-indexed
		int nextCvIterId = 1;
		int remainingIters = cvIterCount;
		for (int batchId = 0; batchId < result.length; ++batchId) {
			final int batchSize = Math.min(cvIterBatchSize, remainingIters);
			final int[] batchCvIterIdArray = new int[batchSize];
			for (int batchCvIterIdArrayIdx = 0; batchCvIterIdArrayIdx < batchCvIterIdArray.length; ++batchCvIterIdArrayIdx) {
				batchCvIterIdArray[batchCvIterIdArrayIdx] = nextCvIterId++;
			}
			result[batchId] = batchCvIterIdArray;
			remainingIters -= batchSize;
		}

		assert remainingIters == 0;
		assert Arrays.stream(result).mapToInt(array -> array.length).sum() == cvIterCount;
		return result;
	}

}
