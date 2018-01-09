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

import java.io.BufferedWriter;
import java.io.Closeable;
import java.io.File;
import java.io.Flushable;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.io.StringWriter;
import java.io.Writer;
import java.math.RoundingMode;
import java.nio.file.Path;
import java.nio.file.Paths;
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
import java.util.function.ToDoubleFunction;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

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
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectSet;
import j2html.Config;
import j2html.TagCreator;
import j2html.tags.ContainerTag;
import j2html.tags.UnescapedText;
import se.kth.speech.coin.tangrams.CLIParameters;
import se.kth.speech.coin.tangrams.wac.data.Referent;
import se.kth.speech.coin.tangrams.wac.data.Round;
import se.kth.speech.coin.tangrams.wac.data.Session;
import se.kth.speech.coin.tangrams.wac.data.SessionSetReader;
import se.kth.speech.function.ThrowingSupplier;
import se.kth.speech.nlp.DocumentObservationData;
import se.kth.speech.nlp.NGramFactory;
import se.kth.speech.nlp.TfIdfScorer;
import se.kth.speech.svg.SVGDocuments;

/**
 * @author <a href="mailto:errantlinguist+github@gmail.com">Todd Shore</a>
 * @since Dec 1, 2017
 *
 */
public final class TfIdfKeywordVisualizationHTMLWriter implements Closeable, Flushable {

	private enum Parameter implements Supplier<Option> {
		// CORPUS_DIR("c") {
		// @Override
		// public Option get() {
		// return Option.builder(optName).longOpt("corpus-dir")
		// .desc("The directory to language-model corpus data
		// from.").hasArg().argName("path")
		// .type(File.class).build();
		// }
		// },
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

		private static final int DEFAULT_MAX_LENGTH = 3;

		private static final int DEFAULT_MIN_LENGTH = 3;

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
			formatter.printHelp(TfIdfKeywordVisualizationHTMLWriter.class.getName() + " INPATHS...", OPTIONS);
		}

		protected final String optName;

		private Parameter(final String optName) {
			this.optName = optName;
		}

	}

	private static class ReferentHtmlSvgElementFactory implements Function<VisualizableReferent, UnescapedText> {

		private static String createXMLString(final Node node) {
			// https://stackoverflow.com/a/10356325/1391325
			final DOMSource domSource = new DOMSource(node);
			final StringWriter writer = new StringWriter();
			final StreamResult result = new StreamResult(writer);
			final TransformerFactory tf = TransformerFactory.newInstance();
			Transformer transformer;
			try {
				transformer = tf.newTransformer();
				transformer.setOutputProperty(OutputKeys.INDENT, "yes");
				transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
				transformer.transform(domSource, result);
				return writer.toString();
			} catch (final TransformerException e) {
				throw new RuntimeException(e);
			}
		}

		private final Map<VisualizableReferent, UnescapedText> cache;

		private final Function<? super VisualizableReferent, ? extends SVGDocument> svgDocFactory;

		private ReferentHtmlSvgElementFactory(
				final Function<? super VisualizableReferent, ? extends SVGDocument> svgDocFactory) {
			this.svgDocFactory = svgDocFactory;
			cache = new HashMap<>();
		}

		@Override
		public UnescapedText apply(final VisualizableReferent ref) {
			return cache.computeIfAbsent(ref, this::create);
		}

		private UnescapedText create(final VisualizableReferent ref) {
			final SVGDocument doc = svgDocFactory.apply(ref);
			return TagCreator.rawHtml(createXMLString(doc.getRootElement()));
		}
	}

	private static final ThreadLocal<NumberFormat> DECIMAL_FORMAT = new ThreadLocal<NumberFormat>() {

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

	private static final Logger LOGGER = LoggerFactory.getLogger(TfIdfKeywordVisualizationHTMLWriter.class);

	private static final List<BiConsumer<VisualizableReferent, SVGDocument>> SVG_DOC_POSTPROCESSORS = createSVGDocPostProcessors();

	private static final List<String> TABLE_COL_NAMES = Arrays.asList("Dyad", "Game rounds",
			"Coref chain length upper bound", "Expected coref chain length", "Coref prob", "Image", "References",
			"N-gram", "Score", "Count");

	private static final String TOKEN_DELIMITER;

	private static final Collector<CharSequence, ?, String> TOKEN_JOINER;

	static {
		TOKEN_DELIMITER = " ";
		TOKEN_JOINER = Collectors.joining(TOKEN_DELIMITER);
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

				final ThrowingSupplier<PrintStream, IOException> outStreamGetter = CLIParameters
						.parseOutpath((File) cl.getParsedOptionValue(Parameter.OUTPATH.optName));
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
				final Map<Session, Map<VisualizableReferent, DocumentObservationData<List<String>>>> sessionRefDocObsData = new SessionReferentNgramDataManager(
						ngramFactory, onlyInstructor).createSessionReferentNgramCountMap(sessions, vizRefs);
				final Map<Entry<Session, VisualizableReferent>, DocumentObservationData<List<String>>> pairDocObsData = SessionReferentNgramDataManager
						.createGroupDocumentPairObservationCountMap(sessionRefDocObsData);
				LOGGER.info("Calculating TF-IDF scores for {} session-referent pairs.", pairDocObsData.size());
				final long tfIdfScorerConstructionStart = System.currentTimeMillis();
				final TfIdfScorer<List<String>, Entry<Session, VisualizableReferent>> tfIdfScorer = TfIdfScorer
						.create(pairDocObsData, tfVariant);
				LOGGER.info("Finished calculating TF-IDF scores after {} seconds.",
						(System.currentTimeMillis() - tfIdfScorerConstructionStart) / 1000.0);

				// final ToDoubleFunction<List<String>> ngramLanguageScorer =
				// createNGramLanguageScorer(cl,
				// sessionRefDocObsData);
				// final ToDoubleBiFunction<List<String>, Entry<String,
				// VisualizableReferent>> rescoredTfIdfScorer = (
				// ngram, doc) -> {
				// final double tfIdfScore = tfIdfScorer.applyAsDouble(ngram,
				// doc);
				// // LOGGER.info("TF-IDF score is {}.", tfIdfScore);
				//// final double ngramProb =
				// ngramLanguageScorer.applyAsDouble(ngram);
				// // LOGGER.info("N-gram probability is {}.", ngramProb);
				//// return tfIdfScore + Math.log10(ngramProb);
				// return tfIdfScore;
				// };

				final long nbestRefs = 20;
				final long nbestNgrams = 1;
				LOGGER.info("Printing {} best referents and {} n-grams for each referent for each dyad.", nbestRefs,
						nbestNgrams);

				int rowsWritten = 0;
				LOGGER.info("Writing rows.");
				final long writeStart = System.currentTimeMillis();
				try (final TfIdfKeywordVisualizationHTMLWriter keywordWriter = new TfIdfKeywordVisualizationHTMLWriter(
						new BufferedWriter(new OutputStreamWriter(outStreamGetter.get())), tfIdfScorer, nbestRefs,
						nbestNgrams, imgResDir)) {
					rowsWritten = keywordWriter.write(pairDocObsData);
				}
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

	private static String createHTMLDocumentString(final ContainerTag html) {
		final String result;
		synchronized (Config.class) {
			final boolean wasCloseEmptyTags = Config.closeEmptyTags;
			Config.closeEmptyTags = true;
			result = TagCreator.document().render() + "\n" + html.renderFormatted();
			Config.closeEmptyTags = wasCloseEmptyTags;
		}
		return result;
	}

	private static ContainerTag createHTMLHeadTag() {
		return TagCreator.head(TagCreator.meta().attr("charset", "utf-8"),
				TagCreator.meta().attr("http-equiv", "X-UA-Compatible").attr("content", "IE=edge"),
				TagCreator.meta().attr("name", "viewport").attr("content", "width=device-width, initial-scale=1"),
				TagCreator.title("TF-IDF scores"));
	}

	private static ContainerTag createNextReferentNGramRow(final Stream<ContainerTag> ngramRowCells) {
		return TagCreator.tr(ngramRowCells.toArray(ContainerTag[]::new));
	}

	private static ToDoubleFunction<List<String>> createNGramLanguageScorer(final CommandLine cl,
			final Map<?, ? extends Map<?, ? extends Object2IntMap<? extends List<String>>>> sessionRefDocObsData) {
		// final ToDoubleFunction<List<String>> result;
		// final File corpusDir = (File)
		// cl.getParsedOptionValue(Parameter.CORPUS_DIR.optName);
		// if (corpusDir == null) {
		// ngramProbScorer = ngram -> 1.0;
		// } else {
		// LOGGER.info("Reading corpus data from \"{}\".", corpusDir);
		final long readingStart = System.currentTimeMillis();
		// final Stream<List<String>> corpusSents =
		// BrownCorpusReading.readPlainSentences(corpusDir.toPath());
		// final Stream<List<List<String>>> corpusSentNgrams =
		// corpusSents.map(ngramFactory);
		// This converts all the sentence n-grams into one stream, which is okay
		// at this
		// point
		// final Stream<List<String>> ngrams =
		// corpusSentNgrams.flatMap(List::stream);
		// sessionRefDocObsData.values().stream().map(Map::values).map(Object2IntMap::object2IntEntrySet);
		// ngramProbScorer = new ProbabilityScorer<>(ngrams);
		// result = new
		// ProbabilityScorer<>(createNGramTotalCountMap(sessionRefDocObsData));
		// LOGGER.info("Finished calculating n-gram probabilities after {}
		// seconds.",
		// (System.currentTimeMillis() - readingStart) / 1000.0);
		// }
		return ngram -> 1.0;
	}

	private static Stream<ContainerTag> createNGramRowCells(final List<String> ngram, final int count,
			final double score) {
		final String ngramRepr = ngram.stream().collect(TOKEN_JOINER);
		return Stream.of(TagCreator.td(ngramRepr), TagCreator.td(Double.toString(score)),
				TagCreator.td(Integer.toString(count)));
	}

	private static Object2IntMap<List<String>> createNGramTotalCountMap(
			final Map<?, ? extends Map<?, ? extends DocumentObservationData<? extends List<String>>>> sessionRefDocObsData) {
		final Object2IntOpenHashMap<List<String>> result = new Object2IntOpenHashMap<>();
		final Stream<DocumentObservationData<? extends List<String>>> refDocObsData = sessionRefDocObsData.values()
				.stream().map(Map::values).flatMap(Collection::stream);
		final Stream<Object2IntMap.Entry<? extends List<String>>> ngramCounts = refDocObsData
				.map(DocumentObservationData::getObservationCounts).map(Object2IntMap::object2IntEntrySet)
				.flatMap(ObjectSet::stream);
		ngramCounts.forEach(ngramCount -> incrementCount(ngramCount.getKey(), ngramCount.getIntValue(), result));
		return result;
	}

	private static List<BiConsumer<VisualizableReferent, SVGDocument>> createSVGDocPostProcessors() {
		final BiConsumer<VisualizableReferent, SVGDocument> resizer = (ref, svgDoc) -> SVGDocuments
				.setSize(svgDoc.getRootElement(), "100%", "5em");
		return Arrays.asList(resizer);
	}

	private static ContainerTag createTableColumnHeaderRow() {
		return TagCreator.tr(TABLE_COL_NAMES.stream().map(TagCreator::th).toArray(ContainerTag[]::new));
	}

	private static <K> void incrementCount(final K key, final int addend, final Object2IntMap<? super K> counts) {
		final int augend = counts.getInt(key);
		final int oldValue = counts.put(key, augend + addend);
		assert oldValue == augend;
	}

	private static ContainerTag rowspan(final ContainerTag tag, final int rowspan) {
		return tag.attr("rowspan", rowspan);
	}

	private final TfIdfKeywordVisualizationRowFactory<UnescapedText, Stream<ContainerTag>> refNgramRowFactory;

	private final Writer writer;

	public TfIdfKeywordVisualizationHTMLWriter(final Writer writer,
			final ToDoubleBiFunction<? super List<String>, ? super Entry<Session, VisualizableReferent>> tfIdfScorer,
			final long nbestRefs, final long nbestNgrams, final Path imgResDir) {
		this.writer = writer;

		final SVGDocumentFactory svgDocFactory = new SVGDocumentFactory(imgResDir, SVG_DOC_POSTPROCESSORS);
		final ReferentHtmlSvgElementFactory refSvgElemFactory = new ReferentHtmlSvgElementFactory(svgDocFactory);
		final TfIdfKeywordVisualizationRowFactory.NGramRowFactory<Stream<ContainerTag>> ngramRowFactory = TfIdfKeywordVisualizationHTMLWriter::createNGramRowCells;
		refNgramRowFactory = new TfIdfKeywordVisualizationRowFactory<>(tfIdfScorer, nbestRefs, nbestNgrams,
				refSvgElemFactory, ngramRowFactory);
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see java.io.Closeable#close()
	 */
	@Override
	public void close() throws IOException {
		writer.close();
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see java.io.Flushable#flush()
	 */
	@Override
	public void flush() throws IOException {
		writer.flush();
	}

	public int write(
			final Map<Entry<Session,VisualizableReferent>, DocumentObservationData<List<String>>> sessionRefDocObsData)
			throws IOException {
		final Stream<ReferentNGramRowGrouping<UnescapedText, Stream<ContainerTag>>> refNgramRows = refNgramRowFactory
				.apply(sessionRefDocObsData);
		final ContainerTag[] rows = refNgramRows.map(this::createReferentNGramRows).flatMap(List::stream)
				.toArray(ContainerTag[]::new);

		final ContainerTag thead = TagCreator.thead(createTableColumnHeaderRow());
		final ContainerTag tbody = TagCreator.tbody(rows);
		final ContainerTag table = TagCreator.table(thead, tbody);
		final ContainerTag html = TagCreator.html(createHTMLHeadTag(), TagCreator.body(table));
		final String docStr = createHTMLDocumentString(html);
		writer.write(docStr);
		return rows.length;
	}

	private ContainerTag createFirstReferentNGramRow(
			final ReferentNGramRowGrouping<UnescapedText, Stream<ContainerTag>> refNgramRowGrouping,
			final Stream<ContainerTag> ngramRowCells, final int rowspan) {
		final Session session = refNgramRowGrouping.getSession();
		final ContainerTag dyadCell = TagCreator.td(session.getName());
		final List<Round> rounds = session.getRounds();
		final ContainerTag roundCountCell = TagCreator.td(Integer.toString(rounds.size()));
		final ReferenceData refData = new ReferenceData(rounds);
		final ContainerTag maxPossibleCorefLengthCell = TagCreator
				.td(Long.toString(refData.coreferenceChainLengthUpperBound()));
		// ContainerTag referenceMaxLikelihoodCell =
		// TagCreator.td(Double.toString(refData.maximumLikelihoodOfBeingReferent()));
		final ContainerTag expectedCorefSeqLengthCell = TagCreator
				.td(Double.toString(refData.expectedCorefSeqLength()));
		final ContainerTag corefProbCell = TagCreator
				.td(DECIMAL_FORMAT.get().format(refData.probabilityOfBeingCoreferentFromStart()));

		final ContainerTag refCell = TagCreator.td(refNgramRowGrouping.getRefVizElem());
		final ContainerTag refOccurrenceCountCell = TagCreator
				.td(Integer.toString(refNgramRowGrouping.getDocumentOccurrenceCount()));
		final Stream<ContainerTag> prefixCells = Stream.of(dyadCell, roundCountCell, maxPossibleCorefLengthCell,
				expectedCorefSeqLengthCell, corefProbCell, refCell, refOccurrenceCountCell)
				.map(cell -> rowspan(cell, rowspan));
		return TagCreator.tr(Stream.concat(prefixCells, ngramRowCells).toArray(ContainerTag[]::new));
	}

	private List<ContainerTag> createReferentNGramRows(
			final ReferentNGramRowGrouping<UnescapedText, Stream<ContainerTag>> refNgramRowGrouping) {
		@SuppressWarnings("unchecked")
		final List<Stream<ContainerTag>> ngramRowCells = Arrays
				.asList(refNgramRowGrouping.getNgramRows().toArray(Stream[]::new));
		final int rowspan = ngramRowCells.size();

		final List<ContainerTag> result = new ArrayList<>(ngramRowCells.size());
		final Iterator<Stream<ContainerTag>> ngramRowCellIter = ngramRowCells.iterator();
		if (ngramRowCellIter.hasNext()) {
			result.add(createFirstReferentNGramRow(refNgramRowGrouping, ngramRowCellIter.next(), rowspan));
			while (ngramRowCellIter.hasNext()) {
				result.add(createNextReferentNGramRow(ngramRowCellIter.next()));
			}
		}
		return result;
	}

}
