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
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Set;
import java.util.TreeSet;
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
import org.apache.commons.math3.stat.descriptive.moment.Mean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import se.kth.speech.coin.tangrams.wac.data.DialogueReferentDescriptionWriter;
import se.kth.speech.coin.tangrams.wac.data.Parameters;
import se.kth.speech.coin.tangrams.wac.data.Session;
import se.kth.speech.coin.tangrams.wac.data.SessionSet;
import se.kth.speech.coin.tangrams.wac.data.SessionSetReader;

public class CrossValidator {

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

	private static final Logger LOGGER = LoggerFactory.getLogger(CrossValidator.class);

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

	public static void main(final CommandLine cl) throws ParseException, IOException {
		if (cl.hasOption(Parameter.HELP.optName)) {
			Parameter.printHelp();
		} else {
			final Path[] inpaths = cl.getArgList().stream().map(Paths::get).toArray(Path[]::new);
			if (inpaths.length < 1) {
				throw new IllegalArgumentException(
						String.format("Usage: %s INPATH", DialogueReferentDescriptionWriter.class.getSimpleName()));
			} else {
				LOGGER.info("Will read sessions from {}.", Arrays.toString(inpaths));
				final Path refTokenFilePath = ((File) cl.getParsedOptionValue(Parameter.REFERRING_TOKENS.optName))
						.toPath();
				final SessionSet set = new SessionSetReader(refTokenFilePath).apply(inpaths);
				LOGGER.info("Will run cross-validation using {} session(s).", set.size());
				final ForkJoinPool executor = ForkJoinPool.commonPool();
				LOGGER.info("Will run cross-validation using a(n) {} instance with a parallelism level of {}.",
						executor.getClass().getSimpleName(), executor.getParallelism());
				final Supplier<LogisticModel> modelFactory = () -> new LogisticModel(executor);
				final CrossValidator crossValidator = new CrossValidator(modelFactory);
				LOGGER.info("Cross-validating using default parameters.");
				System.out.println("TIME" + "\t" + Parameters.getHeader() + "\t" + "SCORE");
				crossValidator.run(set);
				Parameters.ONLY_GIVER = true;
				LOGGER.info("Cross-validating using only instructor language.");
				crossValidator.run(set);
				Parameters.ONLY_GIVER = false;
				Parameters.ONLY_REFLANG = true;
				LOGGER.info("Cross-validating using only referring language.");
				crossValidator.run(set);
				Parameters.ONLY_GIVER = true;
				LOGGER.info("Cross-validating using only referring instructor language.");
				crossValidator.run(set);
				Parameters.UPDATE_MODEL = true;
				Parameters.UPDATE_WEIGHT = 1;
				LOGGER.info(
						"Cross-validating using model which updates itself with intraction data using a weight of {} for the new data.",
						Parameters.UPDATE_WEIGHT);
				crossValidator.run(set);
				Parameters.UPDATE_WEIGHT = 5;
				LOGGER.info(
						"Cross-validating using model which updates itself with intraction data using a weight of {} for the new data.",
						Parameters.UPDATE_WEIGHT);
				crossValidator.run(set);
			}
		}
	}

	private final Supplier<LogisticModel> modelFactory;

	public CrossValidator(final Supplier<LogisticModel> modelFactory) {
		this.modelFactory = modelFactory;
	}

	/**
	 * Performs cross validation on a SessionSet and returns the mean rank
	 */
	public double crossValidate(final SessionSet set) {
		final Mean crossMean = new Mean();
		set.crossValidate((training, testing) -> {
			try {
				final LogisticModel model = modelFactory.get();
				model.train(training);
				final double meanRank = model.eval(new SessionSet(testing));
				crossMean.increment(meanRank);
			} catch (final ClassificationException e) {
				throw new Exception(training, testing, e);
			}
		});
		return crossMean.getResult();
	}

	private void run(final SessionSet set) {
		long t = System.currentTimeMillis();
		final double score = crossValidate(set);
		t = (System.currentTimeMillis() - t) / 1000;
		System.out.println(t + "\t" + Parameters.getSetting() + "\t" + score);
	}

}
