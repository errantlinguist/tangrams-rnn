/*
 * 	Copyright 2018 Todd Shore
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

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.function.ToDoubleBiFunction;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.batik.transcoder.TranscoderException;
import org.apache.batik.transcoder.TranscoderInput;
import org.apache.batik.transcoder.TranscoderOutput;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.fop.svg.PDFTranscoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.svg.SVGDocument;

import se.kth.speech.LaTeX;
import se.kth.speech.coin.tangrams.wac.data.Referent;
import se.kth.speech.coin.tangrams.wac.data.Round;
import se.kth.speech.coin.tangrams.wac.data.Session;
import se.kth.speech.coin.tangrams.wac.data.SessionSetReader;
import se.kth.speech.nlp.DocumentObservationData;
import se.kth.speech.nlp.NGramFactory;
import se.kth.speech.nlp.TfIdfScorer;
import se.kth.speech.svg.SVGDocuments;

/**
 * @author <a href="mailto:errantlinguist+github@gmail.com">Todd Shore</a>
 * @since Jan 5, 2018
 *
 */
public final class TfIdfKeywordVisualizationLaTeXWriter {

	public static final class PDFWritingException extends RuntimeException {

		/**
		 *
		 */
		private static final long serialVersionUID = 5335309159174049878L;

		private PDFWritingException(final Exception cause) {
			super(cause);
		}
	}

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
				final TfIdfScorer.TermFrequencyVariant[] possibleVals = TfIdfScorer.TermFrequencyVariant.values();
				return Option.builder(optName).longOpt("term-frequency")
						.desc(String.format(
								"The method of calculating term frequencies. Possible values: %s; Default value: %s\"",
								Arrays.toString(possibleVals), DEFAULT_TF_VARIANT))
						.hasArg().argName("name").build();
			}
		};

		private static final int DEFAULT_MAX_LENGTH = 4;

		private static final int DEFAULT_MIN_LENGTH = 2;

		private static final TfIdfScorer.TermFrequencyVariant DEFAULT_TF_VARIANT = TfIdfScorer.TermFrequencyVariant.NATURAL;

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
					if (maxLengthValue < minLengthValue) {
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

		private static TfIdfScorer.TermFrequencyVariant parseTermFrequencyVariant(final CommandLine cl) {
			final String name = cl.getOptionValue(Parameter.TERM_FREQUENCY.optName);
			return name == null ? DEFAULT_TF_VARIANT : TfIdfScorer.TermFrequencyVariant.valueOf(name);
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

	private static class ReferentImageTranscoder implements Function<VisualizableReferent, Path> {

		private final Map<VisualizableReferent, Path> cache;

		private int nextDocId = 1;

		private final Path outfileDir;

		private final Function<? super VisualizableReferent, ? extends SVGDocument> svgDocFactory;

		private ReferentImageTranscoder(
				final Function<? super VisualizableReferent, ? extends SVGDocument> svgDocFactory,
				final Path outfileDir) {
			this.svgDocFactory = svgDocFactory;
			this.outfileDir = outfileDir;

			cache = new HashMap<>();
		}

		@Override
		public Path apply(final VisualizableReferent ref) {
			return cache.computeIfAbsent(ref, this::create);
		}

		private Path create(final VisualizableReferent ref) {
			final SVGDocument doc = svgDocFactory.apply(ref);
			final int docId = nextDocId++;
			final String filename = "ref-" + docId + ".pdf";
			final Path result = outfileDir.resolve(filename);
			LOGGER.debug("Writing transcoded image PDF file to \"{}\".", result);
			try {
				writePDF(doc, result);
			} catch (TranscoderException | IOException e) {
				throw new PDFWritingException(e);
			}
			return result;
		}

	}

	private static final String LINE_DELIM;

	private static final Collector<CharSequence, ?, String> LINE_JOINER;

	private static final Logger LOGGER = LoggerFactory.getLogger(TfIdfKeywordVisualizationLaTeXWriter.class);

	private static final ThreadLocal<NumberFormat> SCORE_FORMAT = new ThreadLocal<NumberFormat>() {

		/*
		 * (non-Javadoc)
		 *
		 * @see java.lang.ThreadLocal#initialValue()
		 */
		@Override
		protected NumberFormat initialValue() {
			final NumberFormat result = NumberFormat.getNumberInstance(Locale.US);
			result.setMinimumFractionDigits(3);
			result.setMaximumFractionDigits(3);
			result.setRoundingMode(RoundingMode.HALF_UP);
			return result;
		}

	};

	private static final List<BiConsumer<VisualizableReferent, SVGDocument>> SVG_DOC_POSTPROCESSORS = createSVGDocPostProcessors();

	private static final List<String> TABLE_COL_DEFS;

	private static final Collector<CharSequence, ?, String> TABLE_COL_DELIM = Collectors.joining("\t&\t");

	private static final List<String> TABLE_COL_NAMES;

	private static final List<String> TABLE_PREFIX_COL_NAMES;

	private static final String TABLE_ROW_DELIM = " \\\\";

	private static final String TOKEN_DELIMITER;

	private static final Collector<CharSequence, ?, String> TOKEN_JOINER;

	private static final PDFTranscoder TRANSCODER = new PDFTranscoder();

	static {
		LINE_DELIM = System.lineSeparator();
		LINE_JOINER = Collectors.joining(LINE_DELIM);
	}

	static {
		TOKEN_DELIMITER = " ";
		TOKEN_JOINER = Collectors.joining(TOKEN_DELIMITER, "\\lingform{", "}");
	}

	static {
		TABLE_PREFIX_COL_NAMES = Arrays.asList("Dyad", "Rnds.", "Entity", "References");
		TABLE_COL_NAMES = Arrays
				.asList(Stream.concat(TABLE_PREFIX_COL_NAMES.stream(), Stream.of("$n$-gram", "TF-IDF", "Count"))
						.toArray(String[]::new));
		TABLE_COL_DEFS = Arrays.asList("l", "r", "c", "l", "l", "r", "r");
		assert TABLE_COL_DEFS.size() == TABLE_COL_NAMES
				.size() : "Table column name list and definition list are not the same size.";
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

				final TfIdfScorer.TermFrequencyVariant tfVariant = Parameter.parseTermFrequencyVariant(cl);
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

				final Map<Referent, VisualizableReferent> vizRefs = SessionReferentNgramDataManager
						.createVisualizableReferentMap(sessions);
				final Map<Session, Map<VisualizableReferent, DocumentObservationData<List<String>>>> sessionRefDocObsData = new SessionReferentNgramDataManager(
						Parameter.createNgramFactory(cl), onlyInstructor).createSessionReferentNgramCountMap(sessions,
								vizRefs);
				final Map<Entry<Session, VisualizableReferent>, DocumentObservationData<List<String>>> pairDocObsData = SessionReferentNgramDataManager
						.createGroupDocumentPairObservationCountMap(sessionRefDocObsData);
				LOGGER.info("Calculating TF-IDF scores for {} session-referent pairs.", pairDocObsData.size());
				final long tfIdfScorerConstructionStart = System.currentTimeMillis();
				final TfIdfScorer<List<String>, Entry<Session, VisualizableReferent>> tfIdfScorer = TfIdfScorer
						.create(pairDocObsData, tfVariant);
				LOGGER.info("Finished calculating TF-IDF scores after {} seconds.",
						(System.currentTimeMillis() - tfIdfScorerConstructionStart) / 1000.0);

				final String imgHeight = "2ex";
				LOGGER.info("Will include images using a height of \"{}\".", imgHeight);

				final long nbestRefs = 20;
				final long nbestNgrams = 2;
				LOGGER.info("Printing {} best referents and {} n-grams for each referent for each dyad.", nbestRefs,
						nbestNgrams);
				final TfIdfKeywordVisualizationLaTeXWriter keywordWriter = new TfIdfKeywordVisualizationLaTeXWriter(
						outdir, tfIdfScorer, nbestRefs, nbestNgrams, imgResDir, imgHeight);

				LOGGER.info("Writing rows.");
				final long writeStart = System.currentTimeMillis();
				final int rowsWritten = keywordWriter.write(pairDocObsData);
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
		final String colHeaderRow = TABLE_COL_NAMES.stream().collect(TABLE_COL_DELIM) + TABLE_ROW_DELIM;
		final String tablEnvPrefix = TABLE_COL_DEFS.stream()
				.collect(Collectors.joining(" ", "\\begin{tabular}[h]{|", "|}"));
		return Stream.of(tablEnvPrefix, "\\hline%", colHeaderRow, "\\hline%");
	}

	private static String createNextReferentNGramRow(final Stream<String> ngramRowCells) {
		final Stream<String> prefixCells = Stream.generate(() -> "").limit(TABLE_PREFIX_COL_NAMES.size());
		return Stream.concat(prefixCells, ngramRowCells).collect(TABLE_COL_DELIM) + TABLE_ROW_DELIM;
	}

	private static Stream<String> createNGramRowCells(final List<String> ngram, final int count, final double score) {
		final String ngramRepr = ngram.stream().collect(TOKEN_JOINER);
		return Stream.of(ngramRepr, mathMode(score), mathMode(count));
	}

	private static List<BiConsumer<VisualizableReferent, SVGDocument>> createSVGDocPostProcessors() {
		final BiConsumer<VisualizableReferent, SVGDocument> resizer = (ref, svgDoc) -> SVGDocuments
				.removeSize(svgDoc.getRootElement());
		return Arrays.asList(resizer);
	}

	private static String mathMode(final double value) {
		return "$" + SCORE_FORMAT.get().format(value) + "$";
	}

	private static String mathMode(final int value) {
		return "$" + Integer.toString(value) + "$";
	}

	private static String rowspan(final String cell, final int rowspan) {
		final String result;
		if (rowspan < 2) {
			result = cell;
		} else {
			final String prefix = String.format("\\multirow{%d}{*}{", rowspan);
			final String suffix = "}";
			result = prefix + cell + suffix;
		}
		return result;
	}

	private static String tryMathMode(final String value) {
		String result = value;
		try {
			new BigDecimal(result);
			result = "$" + result + "$";
		} catch (final NumberFormatException e) {
			// Just use the original string
		}
		return result;

	}

	/**
	 * @see <a href= "http://stackoverflow.com/q/32721467/1391325">StackOverflow</a>
	 * @param doc
	 * @param outfilePath
	 * @throws TranscoderException
	 * @throws IOException
	 */
	private static void writePDF(final Document doc, final Path outfilePath) throws TranscoderException, IOException {
		final ByteArrayOutputStream resultByteStream = new ByteArrayOutputStream();
		final TranscoderInput transcoderInput = new TranscoderInput(doc);
		final TranscoderOutput transcoderOutput = new TranscoderOutput(resultByteStream);

		TRANSCODER.transcode(transcoderInput, transcoderOutput);

		try (OutputStream os = Files.newOutputStream(outfilePath, StandardOpenOption.CREATE,
				StandardOpenOption.TRUNCATE_EXISTING)) {
			resultByteStream.writeTo(os);
		}
	}

	private final String imgHeight;

	private final Path outdir;

	private final TfIdfKeywordVisualizationRowFactory<Path, Stream<String>> rowFactory;

	public TfIdfKeywordVisualizationLaTeXWriter(final Path outdir,
			final ToDoubleBiFunction<? super List<String>, ? super Entry<Session, VisualizableReferent>> tfIdfScorer,
			final long nbestRefs, final long nbestNgrams, final Path imgResDir, final String imgHeight)
			throws IOException {
		this.outdir = Files.createDirectories(outdir);
		this.imgHeight = imgHeight;

		final SVGDocumentFactory svgDocFactory = new SVGDocumentFactory(imgResDir, SVG_DOC_POSTPROCESSORS);
		final Path imgResOutdir = outdir.resolve("tf-idf-imgs");
		try {
			Files.createDirectory(imgResOutdir);
		} catch (final FileAlreadyExistsException e) {
			LOGGER.debug("Directory \"{}\" already exists.", imgResOutdir);
		}
		final ReferentImageTranscoder refTableCellFactory = new ReferentImageTranscoder(svgDocFactory, imgResOutdir);
		final TfIdfKeywordVisualizationRowFactory.NGramRowFactory<Stream<String>> ngramRowFactory = TfIdfKeywordVisualizationLaTeXWriter::createNGramRowCells;
		rowFactory = new TfIdfKeywordVisualizationRowFactory<>(tfIdfScorer, nbestRefs, nbestNgrams, refTableCellFactory,
				ngramRowFactory);
	}

	public int write(
			final Map<Entry<Session, VisualizableReferent>, DocumentObservationData<List<String>>> sessionRefDocObsData)
			throws IOException {
		final Stream<ReferentNGramRowGrouping<Path, Stream<String>>> refNgramRows = rowFactory
				.apply(sessionRefDocObsData);
		final String[] rows = refNgramRows.map(this::createReferentNGramRows).flatMap(List::stream)
				.toArray(String[]::new);

		final Stream.Builder<String> fileLineStreamBuilder = Stream.builder();
		fileLineStreamBuilder.accept(createHeaderLines().collect(LINE_JOINER));
		Arrays.stream(rows).forEach(fileLineStreamBuilder);
		fileLineStreamBuilder.accept(createFooterLines().collect(LINE_JOINER));
		final Stream<String> fileLines = fileLineStreamBuilder.build();

		final Path latexFilePath = outdir.resolve("tf-idf.tex");
		LOGGER.debug("Writing LaTeX file to \"{}\".", latexFilePath);
		Files.write(latexFilePath, (Iterable<String>) fileLines::iterator, StandardOpenOption.CREATE,
				StandardOpenOption.TRUNCATE_EXISTING);
		return rows.length;
	}

	private String createFirstReferentNGramRow(final ReferentNGramRowGrouping<Path, Stream<String>> refNgramRowGrouping,
			final Stream<String> ngramRowCells, final int rowspan) {
		final Session session = refNgramRowGrouping.getSession();
		final List<Round> rounds = session.getRounds();

		final Path relOutfilePath = outdir.relativize(refNgramRowGrouping.getRefVizElem());
		LOGGER.debug("Creating LaTeX include statement for path \"{}\".", relOutfilePath);
		final String refVizIncludeStr = createGraphicsIncludeStatement(relOutfilePath);
		final int refCounts = refNgramRowGrouping.getDocumentOccurrenceCount();
		final Stream<String> prefixCells = Stream.of(tryMathMode(session.getName()), mathMode(rounds.size()),
				rowspan(refVizIncludeStr, rowspan), mathMode(refCounts));
		return Stream.concat(prefixCells, ngramRowCells).collect(TABLE_COL_DELIM) + TABLE_ROW_DELIM;
	}

	private String createGraphicsIncludeStatement(final Path outfilePath) {
		final String prefix = String.format("\\includegraphics[height=%s]{", imgHeight);
		final String pathStr = LaTeX.escapeReservedCharacters(outfilePath.toString());
		final String suffix = "}";
		return prefix + pathStr + suffix;
	}

	private List<String> createReferentNGramRows(
			final ReferentNGramRowGrouping<Path, Stream<String>> refNgramRowGrouping) {
		@SuppressWarnings("unchecked")
		final List<Stream<String>> ngramRowCells = Arrays
				.asList(refNgramRowGrouping.getNgramRows().toArray(Stream[]::new));
		final int rowspan = ngramRowCells.size();

		final List<String> result = new ArrayList<>(ngramRowCells.size());
		final Iterator<Stream<String>> ngramRowCellIter = ngramRowCells.iterator();
		if (ngramRowCellIter.hasNext()) {
			result.add(createFirstReferentNGramRow(refNgramRowGrouping, ngramRowCellIter.next(), rowspan));
			while (ngramRowCellIter.hasNext()) {
				result.add(createNextReferentNGramRow(ngramRowCellIter.next()));
			}
		}
		return result;
	}

}
