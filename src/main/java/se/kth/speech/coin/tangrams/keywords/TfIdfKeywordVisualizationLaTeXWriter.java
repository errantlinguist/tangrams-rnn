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
package se.kth.speech.coin.tangrams.keywords;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.BiConsumer;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Node;
import org.w3c.dom.svg.SVGDocument;

import it.unimi.dsi.fastutil.objects.Object2IntMap;
import se.kth.speech.coin.tangrams.svg.SVGDocuments;
import se.kth.speech.coin.tangrams.wac.data.Referent;
import se.kth.speech.coin.tangrams.wac.data.Session;
import se.kth.speech.coin.tangrams.wac.data.SessionSetReader;

/**
 * @author <a href="mailto:tcshore@kth.se">Todd Shore</a>
 * @since Jan 5, 2018
 *
 */
public final class TfIdfKeywordVisualizationLaTeXWriter {

	private enum Parameter implements Supplier<Option> {
		HELP("?") {
			@Override
			public Option get() {
				return Option.builder(optName).longOpt("help").desc("Prints this message.").build();
			}
		},
		IMAGE_RESOURCE_DIR("d") {
			@Override
			public Option get() {
				return Option.builder(optName).longOpt("img-resource-dir")
						.desc("The directory to read image data from.").hasArg().argName("path").type(File.class)
						.required().build();
			}
		},
		MAX_NGRAM_LENGTH("max") {
			@Override
			public Option get() {
				return Option.builder(optName).longOpt("max-length")
						.desc(String.format("The maximum length of n-grams to calculate scores for; Default: %d",
								DEFAULT_MAX_LENGTH))
						.hasArg().argName("length").type(Number.class).build();
			}
		},
		MIN_NGRAM_LENGTH("min") {
			@Override
			public Option get() {
				return Option.builder(optName).longOpt("min-length")
						.desc(String.format("The minimum length of n-grams to calculate scores for; Default: %d",
								DEFAULT_MIN_LENGTH))
						.hasArg().argName("length").type(Number.class).build();
			}
		},
		ONLY_INSTRUCTOR("i") {
			@Override
			public Option get() {
				return Option.builder(optName).longOpt("only-instructor")
						.desc("If this flag is set, only instructor language will be used for TF-IDF calculation.")
						.build();
			}
		},
		OUTDIR("o") {
			@Override
			public Option get() {
				return Option.builder(optName).longOpt("outdir").desc("The directory to write the results to.").hasArg()
						.argName("path").type(File.class).required().build();
			}
		},
		REFERRING_TOKENS("t") {
			@Override
			public Option get() {
				return Option.builder(optName).longOpt("referring-tokens")
						.desc("The file to read utterance referring-language mappings from.").hasArg().argName("path")
						.type(File.class).required().build();
			}
		},
		TERM_FREQUENCY("tf") {
			@Override
			public Option get() {
				final TfIdfCalculator.TermFrequencyVariant[] possibleVals = TfIdfCalculator.TermFrequencyVariant
						.values();
				return Option.builder(optName).longOpt("term-frequency")
						.desc(String.format(
								"The method of calculating term frequencies. Possible values: %s; Default value: %s\"",
								Arrays.toString(possibleVals), DEFAULT_TF_VARIANT))
						.hasArg().argName("name").build();
			}
		};

		private static final int DEFAULT_MAX_LENGTH = 4;

		private static final int DEFAULT_MIN_LENGTH = 2;

		private static final TfIdfCalculator.TermFrequencyVariant DEFAULT_TF_VARIANT = TfIdfCalculator.TermFrequencyVariant.NATURAL;

		private static final Options OPTIONS = createOptions();

		private static NGramFactory createNgramFactory(final CommandLine cl) throws ParseException {
			final int minLengthValue;
			final int maxLengthValue;

			final Number minLength = (Number) cl.getParsedOptionValue(Parameter.MIN_NGRAM_LENGTH.optName);
			final Number maxLength = (Number) cl.getParsedOptionValue(Parameter.MAX_NGRAM_LENGTH.optName);
			if (minLength == null) {
				if (maxLength == null) {
					minLengthValue = DEFAULT_MIN_LENGTH;
					maxLengthValue = DEFAULT_MAX_LENGTH;
				} else {
					maxLengthValue = maxLength.intValue();
					minLengthValue = maxLengthValue < DEFAULT_MIN_LENGTH ? maxLengthValue : DEFAULT_MIN_LENGTH;
				}
			} else {
				minLengthValue = minLength.intValue();
				if (maxLength == null) {
					maxLengthValue = minLengthValue > DEFAULT_MAX_LENGTH ? minLengthValue : DEFAULT_MAX_LENGTH;
				} else {
					maxLengthValue = maxLength.intValue();
					if (maxLengthValue > minLengthValue) {
						throw new ParseException("Maximum n-gram length is less than the minimum.");
					}
				}
			}

			LOGGER.info("Will create n-grams from length {} to {} (inclusive).", minLengthValue, maxLengthValue);
			return new NGramFactory(minLengthValue, maxLengthValue);
		}

		private static Options createOptions() {
			final Options result = new Options();
			Arrays.stream(Parameter.values()).map(Parameter::get).forEach(result::addOption);
			return result;
		}

		private static TfIdfCalculator.TermFrequencyVariant parseTermFrequencyVariant(final CommandLine cl) {
			final String name = cl.getOptionValue(Parameter.TERM_FREQUENCY.optName);
			return name == null ? DEFAULT_TF_VARIANT : TfIdfCalculator.TermFrequencyVariant.valueOf(name);
		}

		private static void printHelp() {
			final HelpFormatter formatter = new HelpFormatter();
			formatter.printHelp(TfIdfKeywordVisualizationLaTeXWriter.class.getName() + " INPATHS...", OPTIONS);
		}

		protected final String optName;

		private Parameter(final String optName) {
			this.optName = optName;
		}

	}

	private static final String FOOTER;

	private static final String HEADER;

	private static final String LINE_DELIM;

	private static final Collector<CharSequence, ?, String> LINE_JOINER;

	private static final Logger LOGGER = LoggerFactory.getLogger(TfIdfKeywordVisualizationLaTeXWriter.class);

	private static final List<BiConsumer<VisualizableReferent, SVGDocument>> SVG_DOC_POSTPROCESSORS = createSVGDocPostProcessors();

	private static final Collector<CharSequence, ?, String> TABLE_COL_DELIM = Collectors.joining("\t&\t");

	private static final String TABLE_ROW_DELIM = " \\\\";

	private static final String TOKEN_DELIMITER;

	private static final Collector<CharSequence, ?, String> TOKEN_JOINER;

	static {
		LINE_DELIM = System.lineSeparator();
		LINE_JOINER = Collectors.joining(LINE_DELIM);
		HEADER = createHeaderLines().collect(LINE_JOINER);
		FOOTER = createFooterLines().collect(LINE_JOINER);
	}

	static {
		TOKEN_DELIMITER = " ";
		TOKEN_JOINER = Collectors.joining(TOKEN_DELIMITER, "\\lingform{", "}");
	}

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
				final Path imgResDir = ((File) cl.getParsedOptionValue(Parameter.IMAGE_RESOURCE_DIR.optName)).toPath();
				LOGGER.info("Will image resources from \"{}\".", imgResDir);

				final TfIdfCalculator.TermFrequencyVariant tfVariant = Parameter.parseTermFrequencyVariant(cl);
				LOGGER.info("Will use term-frequency variant {}.", tfVariant);

				final Path outdir = ((File) cl.getParsedOptionValue(Parameter.OUTDIR.optName)).toPath();
				LOGGER.info("Will write results to \"{}\".", outdir);

				final Path refTokenFilePath = ((File) cl.getParsedOptionValue(Parameter.REFERRING_TOKENS.optName))
						.toPath();
				final Collection<Session> sessions = new SessionSetReader(refTokenFilePath).apply(inpaths)
						.getSessions();
				LOGGER.info("Will extract keywords from {} session(s).", sessions.size());
				final boolean onlyInstructor = cl.hasOption(Parameter.ONLY_INSTRUCTOR.optName);
				LOGGER.info("Only use instructor language? {}", onlyInstructor);

				final NGramFactory ngramFactory = Parameter.createNgramFactory(cl);

				final Map<Referent, VisualizableReferent> vizRefs = SessionReferentNgramDataManager
						.createVisualizableReferentMap(sessions);
				final Map<String, Map<VisualizableReferent, Object2IntMap<List<String>>>> sessionRefNgramCounts = new SessionReferentNgramDataManager(
						ngramFactory, onlyInstructor).createSessionReferentNgramCountMap(sessions, vizRefs);
				final Map<Entry<String, VisualizableReferent>, Object2IntMap<List<String>>> pairNgramCounts = SessionReferentNgramDataManager
						.createSessionReferentPairNgramCountMap(sessionRefNgramCounts);
				LOGGER.info("Calculating TF-IDF scores for {} session-referent pairs.", pairNgramCounts.size());
				final long tfIdfCalculatorConstructionStart = System.currentTimeMillis();
				final TfIdfCalculator<List<String>, Entry<String, VisualizableReferent>> tfIdfCalculator = TfIdfCalculator
						.create(pairNgramCounts, tfVariant);
				LOGGER.info("Finished calculating TF-IDF scores after {} seconds.",
						(System.currentTimeMillis() - tfIdfCalculatorConstructionStart) / 1000.0);

				final long nbestRefs = 3;
				final long nbestNgrams = 3;
				LOGGER.info("Printing {} best referents and {} n-grams for each referent for each dyad.", nbestRefs,
						nbestNgrams);
				final TfIdfKeywordVisualizationLaTeXWriter keywordWriter = new TfIdfKeywordVisualizationLaTeXWriter(
						imgResDir, tfIdfCalculator, nbestRefs, nbestNgrams, outdir);

				LOGGER.info("Writing rows.");
				final long writeStart = System.currentTimeMillis();
				final int rowsWritten = keywordWriter.write(sessionRefNgramCounts);
				LOGGER.info("Wrote {} row(s) in {} seconds.", rowsWritten,
						(System.currentTimeMillis() - writeStart) / 1000.0);
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

	private static Stream<String> createFooterLines() {
		return Stream.of("\\hline%", "\\end{tabular}");
	}

	private static Stream<String> createHeaderLines() {
		final Stream<String> colHeaders = Stream.of("Dyad", "Referent", "$n$-gram");
		final String colHeaderRow = colHeaders.collect(TABLE_COL_DELIM) + TABLE_ROW_DELIM;
		return Stream.of("\\begin{tabular}{|l l l|}", "\\hline%", colHeaderRow, "\\hline%");
	}

	private static String createRow(final String dyadId, final Node refSvgRootElem, final List<String> ngram,
			final int count, final double score) {
		final String ngramRepr = ngram.stream().collect(TOKEN_JOINER);
		// TODO: Implement
		final String referentRepr = "";
		final Stream<String> rowCells = Stream.of(dyadId, referentRepr, ngramRepr);
		return rowCells.collect(TABLE_COL_DELIM) + TABLE_ROW_DELIM + LINE_DELIM;
	}

	private static List<BiConsumer<VisualizableReferent, SVGDocument>> createSVGDocPostProcessors() {
		final BiConsumer<VisualizableReferent, SVGDocument> resizer = (ref, svgDoc) -> SVGDocuments.setSize(svgDoc,
				"100%", "100%");
		return Arrays.asList(resizer);
	}

	private final Path outdir;

	private final TfIdfKeywordVisualizationRowFactory<String> rowFactory;

	public TfIdfKeywordVisualizationLaTeXWriter(final Path imgResDir,
			final TfIdfCalculator<List<String>, Entry<String, VisualizableReferent>> tfIdfCalculator,
			final long nbestRefs, final long nbestNgrams, final Path outdir) {
		this.outdir = outdir;

		final SVGDocumentFactory svgDocFactory = new SVGDocumentFactory(imgResDir, SVG_DOC_POSTPROCESSORS);
		final TfIdfKeywordVisualizationRowFactory.RowFactory<String> latexTableRowFactory = TfIdfKeywordVisualizationLaTeXWriter::createRow;
		rowFactory = new TfIdfKeywordVisualizationRowFactory<>(svgDocFactory, tfIdfCalculator, nbestRefs, nbestNgrams,
				latexTableRowFactory);
	}

	public int write(final Map<String, Map<VisualizableReferent, Object2IntMap<List<String>>>> sessionRefNgramCounts)
			throws IOException {
		final String[] rows = rowFactory.apply(sessionRefNgramCounts).toArray(String[]::new);
		// TODO: Write individual PDFS for each referent to file

		final Path outdir = Files.createDirectories(this.outdir);
		final Path latexFilePath = outdir.resolve("tf-idf.tex");

		final Stream.Builder<String> fileLineStreamBuilder = Stream.builder();
		fileLineStreamBuilder.accept(HEADER);
		Arrays.stream(rows).forEach(fileLineStreamBuilder);
		fileLineStreamBuilder.accept(FOOTER);
		final Stream<String> fileLines = fileLineStreamBuilder.build();
		Files.write(latexFilePath, (Iterable<String>) fileLines::iterator, StandardOpenOption.CREATE,
				StandardOpenOption.TRUNCATE_EXISTING);
		return rows.length;
	}

}
