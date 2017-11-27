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
import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Executor;
import java.util.concurrent.ForkJoinPool;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import se.kth.speech.FileNames;
import se.kth.speech.HashedCollections;
import se.kth.speech.coin.tangrams.wac.data.DialogueReferentDescriptionWriter;
import se.kth.speech.coin.tangrams.wac.data.Session;
import se.kth.speech.coin.tangrams.wac.data.SessionSet;
import se.kth.speech.coin.tangrams.wac.data.SessionSetReader;

public class BatchCrossValidator {

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
				return Option.builder(optName).longOpt("model-params").desc(
						"The file to read model parameters from for each individual cross-validation test to run.")
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
			formatter.printHelp(DialogueReferentDescriptionWriter.class.getSimpleName() + " INPATHS...", OPTIONS);
		}

		protected final String optName;

		private Parameter(final String optName) {
			this.optName = optName;
		}

	}

	final static class Exception extends RuntimeException {

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

	private static final Logger LOGGER = LoggerFactory.getLogger(BatchCrossValidator.class);

	private static final Charset OUTFILE_ENCODING = StandardCharsets.UTF_8;

	public static void main(final CommandLine cl) throws ParseException, IOException {
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
				final Map<String, Map<ModelParameter, Object>> modelParamSets = new ModelParameterTabularDataReader()
						.apply(batchModelParamFile);
				LOGGER.info("Will run {} cross-validation batch(es).", modelParamSets.size());

				final ForkJoinPool executor = ForkJoinPool.commonPool();
				LOGGER.info("Will run cross-validation using a(n) {} instance with a parallelism level of {}.",
						executor.getClass().getSimpleName(), executor.getParallelism());
				final BatchCrossValidator batchCrossValidator = new BatchCrossValidator(set, executor);
				batchCrossValidator.accept(modelParamSets, Files.createDirectories(outdirPath));
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

	private final SessionSet set;

	private final Executor executor;

	public BatchCrossValidator(final SessionSet set, final Executor executor) {
		this.set = set;
		this.executor = executor;
	}

	public void accept(final Map<String, Map<ModelParameter, Object>> modelParamSets, final Path outdirPath)
			throws IOException {
		final Iterator<Entry<String, Map<ModelParameter, Object>>> sortedParamSetIter = modelParamSets.entrySet()
				.stream().sorted(Comparator.comparing(Entry::getKey)).iterator();
		final ConcurrentMap<Path, BufferedWriter> outfileWriters = new ConcurrentHashMap<>(
				HashedCollections.capacity(modelParamSets.size()));
		try {
			while (sortedParamSetIter.hasNext()) {
				final Entry<String, Map<ModelParameter, Object>> sortedParamSet = sortedParamSetIter.next();
				final String name = sortedParamSet.getKey();
				final Path outfilePath = outdirPath.resolve(FileNames.sanitize(name + ".tsv", "-"));
				LOGGER.info("Will write results of cross-validation test named \"{}\" to \"{}\"", name, outfilePath);
				final Map<ModelParameter, Object> modelParams = sortedParamSet.getValue();
				final Supplier<LogisticModel> modelFactory = () -> new LogisticModel(modelParams, executor);
				final CrossValidator crossValidator = new CrossValidator(modelParams, modelFactory, executor);
				final BufferedWriter newOutfileWriter = Files.newBufferedWriter(outfilePath, OUTFILE_ENCODING,
						StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
				final BufferedWriter oldOutfileWriter = outfileWriters.put(outfilePath, newOutfileWriter);
				assert oldOutfileWriter == null;
				final CrossValidationTablularDataWriter resultWriter = new CrossValidationTablularDataWriter(
						newOutfileWriter);
				crossValidator.crossValidate(set, evalResult -> {
					try {
						resultWriter.accept(evalResult);
					} catch (final IOException e) {
						throw new UncheckedIOException(String.format(
								"A(n) %s occurred while writing the results of cross-validating while using param set \"%s\" to file \"%s\".",
								e.getClass().getSimpleName(), name, outfilePath), e);
					}
				});
			}
		} finally {
			for (final Map.Entry<Path, BufferedWriter> outfileWriter : outfileWriters.entrySet()) {
				final Path outfilePath = outfileWriter.getKey();
				final BufferedWriter writer = outfileWriter.getValue();
				try {
					writer.close();
				} catch (final IOException e) {
					LOGGER.error(String.format("A(n) %s occurred while trying to close the writer for file \"%s\".",
							e.getClass().getSimpleName(), outfilePath), e);
				}
			}
		}
	}

}
