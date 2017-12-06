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
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
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
import se.kth.speech.coin.tangrams.wac.data.Referent;
import se.kth.speech.coin.tangrams.wac.data.Round;
import se.kth.speech.coin.tangrams.wac.data.Session;
import se.kth.speech.coin.tangrams.wac.data.SessionSet;
import se.kth.speech.coin.tangrams.wac.data.SessionSetReader;
import se.kth.speech.function.ThrowingSupplier;
import weka.classifiers.functions.Logistic;

/**
 * @author <a href="mailto:tcshore@kth.se">Todd Shore</a>
 * @since 5 Dec 2017
 *
 */
public final class RoundReferentFeatureConfidenceWriter {

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
				return Option.builder(optName).longOpt("outdir").desc("The file to write the results to.").hasArg()
						.argName("path").type(File.class).build();
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
			formatter.printHelp(RoundReferentFeatureConfidenceWriter.class.getName() + " INPATHS...", OPTIONS);
		}

		protected final String optName;

		private Parameter(final String optName) {
			this.optName = optName;
		}

	}

	private static final Logger LOGGER = LoggerFactory.getLogger(RoundReferentFeatureConfidenceWriter.class);

	public static void main(final CommandLine cl) throws IOException, ClassificationException, ParseException {
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
				final SessionSet set = new SessionSetReader(refTokenFilePath).apply(inpaths);
				final RoundReferentFeatureConfidenceWriter writer = new RoundReferentFeatureConfidenceWriter();
				writer.accept(set, outStreamGetter);
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

	private final List<RoundReferentFeatureDescription> dataToWrite;

	public RoundReferentFeatureConfidenceWriter() {
		this(Arrays.asList(RoundReferentFeatureDescription.values()));
	}

	public RoundReferentFeatureConfidenceWriter(final List<RoundReferentFeatureDescription> dataToWrite) {
		this.dataToWrite = dataToWrite;
	}

	public void accept(final SessionSet set, final ThrowingSupplier<PrintStream, IOException> outStreamGetter)
			throws IOException {
		final Collection<Session> sessions = set.getSessions();
		LOGGER.info("Writing classification confidence scores for {} session(s).", sessions.size());
		final Map<ModelParameter, Object> modelParams = ModelParameter.createDefaultParamValueMap();
		final LogisticModel model = new LogisticModel(modelParams);
		model.train(set);
		final LogisticModel.Scorer scorer = model.createScorer();

		final boolean onlyInstructor = (Boolean) modelParams.get(ModelParameter.ONLY_INSTRUCTOR);

		try (CSVPrinter printer = CSVFormat.TDF.withHeader(createColumnNames().toArray(String[]::new))
				.print(outStreamGetter.get())) {
			for (final Session session : sessions) {
				final String dyadId = session.getName();
				final List<Round> rounds = session.getRounds();

				for (final ListIterator<Round> roundIter = rounds.listIterator(); roundIter.hasNext();) {
					final Round round = roundIter.next();
					// Rounds are 1-indexed
					final String roundId = Integer.toString(roundIter.nextIndex());
					final String[] refTokens = round.getReferringTokens(onlyInstructor).toArray(String[]::new);
					final List<Referent> refs = round.getReferents();
					for (final String refToken : refTokens) {
						Logistic wordClassifier = model.getWordClassifier(refToken);
						final boolean isOov;
						if (isOov = wordClassifier == null) {
							wordClassifier = model.getDiscountClassifier();
						}
						for (final ListIterator<Referent> refIter = refs.listIterator(); refIter.hasNext();) {
							final Referent ref = refIter.next();
							// Entities are 1-indexed
							final String refId = Integer.toString(refIter.nextIndex());
							final double refConf = scorer.score(wordClassifier, ref);
							final RoundReferentFeatureDescription.Input descInput = new RoundReferentFeatureDescription.Input(
									dyadId, roundId, round, refId, ref, refToken, isOov, refConf);
							final Stream<String> rowCells = createRowCells(descInput);
							printer.printRecord((Iterable<String>) rowCells::iterator);
						}
					}
				}
			}
		}
		LOGGER.info("Finished writing.");
	}

	private Stream<String> createColumnNames() {
		return dataToWrite.stream().map(RoundReferentFeatureDescription::toString);
	}

	private Stream<String> createRowCells(final RoundReferentFeatureDescription.Input descInput) {
		return dataToWrite.stream().map(datum -> datum.apply(descInput));
	}

}
