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
import java.io.File;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.io.StringWriter;
import java.io.Writer;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Supplier;
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
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Node;
import org.w3c.dom.svg.SVGDocument;

import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import j2html.Config;
import j2html.TagCreator;
import j2html.tags.ContainerTag;
import j2html.tags.UnescapedText;
import se.kth.speech.HashedCollections;
import se.kth.speech.coin.tangrams.CLIParameters;
import se.kth.speech.coin.tangrams.content.SVGDocuments;
import se.kth.speech.coin.tangrams.wac.data.Referent;
import se.kth.speech.coin.tangrams.wac.data.Round;
import se.kth.speech.coin.tangrams.wac.data.Session;
import se.kth.speech.coin.tangrams.wac.data.SessionSetReader;
import se.kth.speech.coin.tangrams.wac.data.Utterance;
import se.kth.speech.function.ThrowingSupplier;

/**
 * @author <a href="mailto:tcshore@kth.se">Todd Shore</a>
 * @since Dec 1, 2017
 *
 */
public final class TfIdfKeywordVisualizationHTMLWriter {

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
						.desc("The maximum length of n-grams to calculate scores for..").hasArg().argName("length")
						.type(Number.class).build();
			}
		},
		MIN_NGRAM_LENGTH("min") {
			@Override
			public Option get() {
				return Option.builder(optName).longOpt("min-length")
						.desc("The minimum length of n-grams to calculate scores for..").hasArg().argName("length")
						.type(Number.class).build();
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
			final Number maxLength = (Number) cl.getParsedOptionValue(Parameter.MIN_NGRAM_LENGTH.optName);
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
			formatter.printHelp(TfIdfKeywordVisualizationHTMLWriter.class.getName() + " INPATHS...", OPTIONS);
		}

		protected final String optName;

		private Parameter(final String optName) {
			this.optName = optName;
		}

	}

	private static final int EST_UNIQUE_REFS_PER_SESSION = 50;

	private static final Logger LOGGER = LoggerFactory.getLogger(TfIdfKeywordVisualizationHTMLWriter.class);

	private static final String TOKEN_DELIMITER;

	private static final Collector<CharSequence, ?, String> TOKEN_JOINER;

	static {
		TOKEN_DELIMITER = " ";
		TOKEN_JOINER = Collectors.joining(TOKEN_DELIMITER);
	}

	private static final List<BiConsumer<VisualizableReferent, SVGDocument>> SVG_DOC_POSTPROCESSORS = createSVGDocPostProcessors();

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

				final Map<Referent, VisualizableReferent> vizRefs = createVisualizableReferentMap(sessions);
				final Map<String, Map<VisualizableReferent, Object2IntMap<List<String>>>> sessionRefNgramCounts = createSessionReferentNgramCountMap(
						sessions, vizRefs, ngramFactory, onlyInstructor);
				final Map<Entry<String, VisualizableReferent>, Object2IntMap<List<String>>> pairNgramCounts = createSessionReferentPairNgramCountMap(
						sessionRefNgramCounts);
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
				final TfIdfKeywordVisualizationHTMLWriter keywordWriter = new TfIdfKeywordVisualizationHTMLWriter(
						imgResDir, sessionRefNgramCounts, tfIdfCalculator, nbestRefs, nbestNgrams);

				int rowsWritten = 0;
				LOGGER.info("Writing rows.");
				final long writeStart = System.currentTimeMillis();
				try (BufferedWriter outputWriter = new BufferedWriter(new OutputStreamWriter(outStreamGetter.get()))) {
					rowsWritten = keywordWriter.write(outputWriter);
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

	private static Object2IntMap<List<String>> createNgramCountMap(final Round round, final NGramFactory ngramFactory,
			final boolean onlyInstructor) {
		@SuppressWarnings("unchecked")
		final List<List<String>> ngrams = Arrays
				.asList(createNgrams(round, ngramFactory, onlyInstructor).toArray(List[]::new));
		final Object2IntOpenHashMap<List<String>> result = new Object2IntOpenHashMap<List<String>>(ngrams.size());
		ngrams.forEach(ngram -> incrementCount(ngram, result));
		result.trim();
		return result;
	}

	private static Stream<List<String>> createNgrams(final Round round, final NGramFactory ngramFactory,
			final boolean onlyInstructor) {
		final Stream<Utterance> utts = round.getUtts().stream();
		final Stream<Utterance> instructorUtts = utts.filter(Utterance::isInstructor);
		final Stream<List<String>> uttTokenSeqs = instructorUtts.map(Utterance::getReferringTokens);
		return uttTokenSeqs.map(ngramFactory).flatMap(List::stream);
	}

	private static ContainerTag createRow(final String dyadId, final Node refSvgRootElem, final List<String> ngram,
			final int count, final double score) {
		final UnescapedText svgTag = TagCreator.rawHtml(createXMLString(refSvgRootElem));
		final String ngramRepr = ngram.stream().collect(TOKEN_JOINER);
		final ContainerTag svgCell = TagCreator.td(svgTag);
		return TagCreator.tr(TagCreator.td(dyadId), svgCell, TagCreator.td(ngramRepr),
				TagCreator.td(Double.toString(score)), TagCreator.td(Integer.toString(count)));
	}

	private static Map<String, Map<VisualizableReferent, Object2IntMap<List<String>>>> createSessionReferentNgramCountMap(
			final Collection<Session> sessions, final Map<Referent, VisualizableReferent> vizRefs,
			final NGramFactory ngramFactory, final boolean onlyInstructor) {
		final Map<String, Map<VisualizableReferent, Object2IntMap<List<String>>>> result = new HashMap<>(
				sessions.size());
		sessions.forEach(session -> {
			final String sessionName = session.getName();
			final Map<VisualizableReferent, Object2IntMap<List<String>>> refNgramCounts = result.computeIfAbsent(
					sessionName, key -> new HashMap<>(HashedCollections.capacity(EST_UNIQUE_REFS_PER_SESSION)));

			session.getRounds().forEach(round -> {
				// Use the same n-gram list for each referent
				final Object2IntMap<List<String>> ngramCounts = createNgramCountMap(round, ngramFactory,
						onlyInstructor);

				getTargetRefs(round).map(vizRefs::get).forEach(vizRef -> {
					final Object2IntMap<List<String>> oldTableValue = refNgramCounts.put(vizRef, ngramCounts);
					assert oldTableValue == null;
				});
			});
		});
		return result;
	}

	private static Map<Entry<String, VisualizableReferent>, Object2IntMap<List<String>>> createSessionReferentPairNgramCountMap(
			final Map<String, Map<VisualizableReferent, Object2IntMap<List<String>>>> sessionRefNgramCounts) {
		final Map<Entry<String, VisualizableReferent>, Object2IntMap<List<String>>> result = new HashMap<>(
				HashedCollections.capacity(sessionRefNgramCounts.size() * EST_UNIQUE_REFS_PER_SESSION));
		sessionRefNgramCounts.forEach((sessionName, refNgramCounts) -> {
			refNgramCounts.forEach((ref, ngramCounts) -> {
				final Entry<String, VisualizableReferent> pair = Pair.of(sessionName, ref);
				result.put(pair, ngramCounts);
			});
		});
		assert result.size() >= sessionRefNgramCounts.size();
		return result;
	}

	private static List<BiConsumer<VisualizableReferent, SVGDocument>> createSVGDocPostProcessors() {
		final BiConsumer<VisualizableReferent, SVGDocument> resizer = (ref, svgDoc) -> SVGDocuments.setSize(svgDoc,
				"100%", "100px");
		return Arrays.asList(resizer);
	}

	private static Map<Referent, VisualizableReferent> createVisualizableReferentMap(
			final Collection<Session> sessions) {
		final Stream<Referent> refs = sessions.stream().map(Session::getRounds).flatMap(List::stream)
				.flatMap(TfIdfKeywordVisualizationHTMLWriter::getTargetRefs);
		return refs.distinct().collect(Collectors.toMap(Function.identity(), VisualizableReferent::fetch));
	}

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

	private static Stream<Referent> getTargetRefs(final Round round) {
		return round.getReferents().stream().filter(Referent::isTarget);
	}

	private static <K> void incrementCount(final K key, final Object2IntMap<? super K> counts) {
		final int oldValue = counts.getInt(key);
		final int oldValue2 = counts.put(key, oldValue + 1);
		assert oldValue == oldValue2;
	}

	private final TfIdfKeywordVisualizationRowFactory<ContainerTag> rowFactory;

	private final Map<String, Map<VisualizableReferent, Object2IntMap<List<String>>>> sessionNgramCounts;

	public TfIdfKeywordVisualizationHTMLWriter(final Path imgResDir,
			final Map<String, Map<VisualizableReferent, Object2IntMap<List<String>>>> sessionNgramCounts,
			final TfIdfCalculator<List<String>, Entry<String, VisualizableReferent>> tfIdfCalculator,
			final long nbestRefs, final long nbestNgrams) {
		this.sessionNgramCounts = sessionNgramCounts;

		final SVGDocumentFactory svgDocFactory = new SVGDocumentFactory(imgResDir, SVG_DOC_POSTPROCESSORS);
		final TfIdfKeywordVisualizationRowFactory.RowFactory<ContainerTag> htmlTableRowFactory = TfIdfKeywordVisualizationHTMLWriter::createRow;
		rowFactory = new TfIdfKeywordVisualizationRowFactory<>(svgDocFactory, tfIdfCalculator, nbestRefs, nbestNgrams,
				htmlTableRowFactory);
	}

	public int write(final Writer writer) throws IOException {
		final ContainerTag[] rows = rowFactory.apply(sessionNgramCounts).toArray(ContainerTag[]::new);

		final ContainerTag thead = TagCreator.thead(TagCreator.tr(TagCreator.td("Dyad"), TagCreator.td("Image"),
				TagCreator.td("N-gram"), TagCreator.td("Score"), TagCreator.td("Count")));
		final ContainerTag tbody = TagCreator.tbody(rows);
		final ContainerTag table = TagCreator.table(thead, tbody);
		final ContainerTag html = TagCreator.html(createHTMLHeadTag(), TagCreator.body(table));
		final String docStr = createHTMLDocumentString(html);
		writer.write(docStr);
		return rows.length;
	}

}
