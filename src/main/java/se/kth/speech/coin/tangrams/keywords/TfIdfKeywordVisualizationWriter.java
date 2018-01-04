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
import java.io.PrintStream;
import java.net.URI;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NavigableMap;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.function.ToDoubleFunction;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.swing.JFrame;
import javax.swing.WindowConstants;

import org.apache.batik.anim.dom.SVGOMPathElement;
import org.apache.batik.swing.JSVGCanvas;
import org.apache.batik.swing.svg.SVGDocumentLoaderAdapter;
import org.apache.batik.swing.svg.SVGDocumentLoaderEvent;
import org.apache.batik.util.SVGConstants;
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
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.svg.SVGAnimatedRect;
import org.w3c.dom.svg.SVGDocument;
import org.w3c.dom.svg.SVGRect;
import org.w3c.dom.svg.SVGSVGElement;

import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import se.kth.speech.coin.tangrams.CLIParameters;
import se.kth.speech.coin.tangrams.wac.data.Referent;
import se.kth.speech.coin.tangrams.wac.data.Round;
import se.kth.speech.coin.tangrams.wac.data.Session;
import se.kth.speech.coin.tangrams.wac.data.SessionSetReader;
import se.kth.speech.coin.tangrams.wac.data.Utterance;
import se.kth.speech.coin.tangrams.wac.logistic.Weighted;
import se.kth.speech.function.ThrowingSupplier;
import weka.core.tokenizers.NGramTokenizer;

/**
 * @author <a href="mailto:tcshore@kth.se">Todd Shore</a>
 * @since Dec 1, 2017
 *
 */
public final class TfIdfKeywordVisualizationWriter {

	private enum Parameter implements Supplier<Option> {
		HELP("?") {
			@Override
			public Option get() {
				return Option.builder(optName).longOpt("help").desc("Prints this message.").build();
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
		IMAGE_RESOURCE_DIR("r") {
			@Override
			public Option get() {
				return Option.builder(optName).longOpt("img-resource-dir")
						.desc("The directory to read image data from.").hasArg().argName("path").type(File.class)
						.required().build();
			}
		},
		OUTPATH("o") {
			@Override
			public Option get() {
				return Option.builder(optName).longOpt("outpath").desc(
						"The file to write the results to; If this option is not supplied, the standard output stream will be used.")
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

		private static final TfIdfCalculator.TermFrequencyVariant DEFAULT_TF_VARIANT = TfIdfCalculator.TermFrequencyVariant.NATURAL;

		private static final Options OPTIONS = createOptions();

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
			formatter.printHelp(TfIdfKeywordVisualizationWriter.class.getName() + " INPATHS...", OPTIONS);
		}

		protected final String optName;

		private Parameter(final String optName) {
			this.optName = optName;
		}

	}

	private static class VisualizableReferent {

		private static final Map<Referent, VisualizableReferent> INSTANCES = new ConcurrentHashMap<>();

		private static VisualizableReferent fetch(final Referent ref) {
			return INSTANCES.computeIfAbsent(ref, VisualizableReferent::new);
		}

		private final float blue;

		private final float green;

		private final float hue;

		private final float red;

		private final String shape;

		private VisualizableReferent(final Referent ref) {
			blue = ref.getBlue();
			green = ref.getGreen();
			hue = ref.getHue();
			red = ref.getRed();
			shape = ref.getShape();
		}

		/*
		 * (non-Javadoc)
		 *
		 * @see java.lang.Object#equals(java.lang.Object)
		 */
		@Override
		public boolean equals(final Object obj) {
			if (this == obj) {
				return true;
			}
			if (obj == null) {
				return false;
			}
			if (!(obj instanceof VisualizableReferent)) {
				return false;
			}
			final VisualizableReferent other = (VisualizableReferent) obj;
			if (Float.floatToIntBits(blue) != Float.floatToIntBits(other.blue)) {
				return false;
			}
			if (Float.floatToIntBits(green) != Float.floatToIntBits(other.green)) {
				return false;
			}
			if (Float.floatToIntBits(hue) != Float.floatToIntBits(other.hue)) {
				return false;
			}
			if (Float.floatToIntBits(red) != Float.floatToIntBits(other.red)) {
				return false;
			}
			if (shape == null) {
				if (other.shape != null) {
					return false;
				}
			} else if (!shape.equals(other.shape)) {
				return false;
			}
			return true;
		}

		/**
		 * @return the blue
		 */
		public float getBlue() {
			return blue;
		}

		/**
		 * @return the green
		 */
		public float getGreen() {
			return green;
		}

		/**
		 * @return the red
		 */
		public float getRed() {
			return red;
		}

		/**
		 * @return the shape
		 */
		public String getShape() {
			return shape;
		}

		/*
		 * (non-Javadoc)
		 *
		 * @see java.lang.Object#hashCode()
		 */
		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + Float.floatToIntBits(blue);
			result = prime * result + Float.floatToIntBits(green);
			result = prime * result + Float.floatToIntBits(hue);
			result = prime * result + Float.floatToIntBits(red);
			result = prime * result + (shape == null ? 0 : shape.hashCode());
			return result;
		}

		/*
		 * (non-Javadoc)
		 *
		 * @see java.lang.Object#toString()
		 */
		@Override
		public String toString() {
			final StringBuilder builder = new StringBuilder(256);
			builder.append("Referent [edgeCount=");
			builder.append(", blue=");
			builder.append(blue);
			builder.append(", green=");
			builder.append(green);
			builder.append(", hue=");
			builder.append(hue);
			builder.append(", red=");
			builder.append(red);
			builder.append(", shape=");
			builder.append(shape);
			builder.append("]");
			return builder.toString();
		}

	}

	private static final String[] COL_HEADERS = new String[] { "SESSION", "NGRAM", "TF-IDF", "NGRAM_LENGTH",
			"NORMALIZED_TF-IDF" };

	private static final Logger LOGGER = LoggerFactory.getLogger(TfIdfKeywordVisualizationWriter.class);

	private static final String TOKEN_DELIMITER;

	private static final Collector<CharSequence, ?, String> TOKEN_JOINER;

	static {
		TOKEN_DELIMITER = " ";
		TOKEN_JOINER = Collectors.joining(TOKEN_DELIMITER);
	}

	private static Map<Session, List<List<String>>> createSessionNgramMap(final Collection<Session> sessions) {
		final Map<Session, List<List<String>>> result = new HashMap<>(
				Math.toIntExact(Math.round(Math.ceil(sessions.size() * 1.25))));
		sessions.forEach(session -> result.put(session, createNgrams(session).collect(Collectors.toList())));
		return result;
	}

	private static Stream<List<String>> createNgrams(final Session session) {
		return session.getRounds().stream().flatMap(TfIdfKeywordVisualizationWriter::createNgrams);
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

				final ThrowingSupplier<PrintStream, IOException> outStreamGetter = CLIParameters
						.parseOutpath((File) cl.getParsedOptionValue(Parameter.OUTPATH.optName));
				final Path refTokenFilePath = ((File) cl.getParsedOptionValue(Parameter.REFERRING_TOKENS.optName))
						.toPath();
				final Collection<Session> sessions = new SessionSetReader(refTokenFilePath).apply(inpaths)
						.getSessions();
				LOGGER.info("Will extract keywords from {} session(s).", sessions.size());
				final boolean onlyInstructor = cl.hasOption(Parameter.ONLY_INSTRUCTOR.optName);
				LOGGER.info("Only use instructor language? {}", onlyInstructor);

				final Map<Session, List<List<String>>> sessionNgrams = createSessionNgramMap(
						new SessionSetReader(refTokenFilePath).apply(inpaths).getSessions());
				final Map<Session, ReferentNGramCounts> sessionRefNgramCounts = createReferentNgramCountMap(
						sessionNgrams);

				final TfIdfCalculator<List<String>> tfIdfCalculator = TfIdfCalculator.create(sessionNgrams,
						onlyInstructor, tfVariant);
				final TfIdfKeywordVisualizationWriter keywordWriter = new TfIdfKeywordVisualizationWriter(imgResDir,
						sessionRefNgramCounts, tfIdfCalculator);

				int rowsWritten = 0;
				try (CSVPrinter printer = CSVFormat.TDF.withHeader(COL_HEADERS).print(outStreamGetter.get())) {
					rowsWritten = keywordWriter.write(printer);
				}
				LOGGER.info("Wrote {} row(s).", rowsWritten);
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

	private static final Collector<Entry<Session, List<List<String>>>, ?, Map<Session, ReferentNGramCounts>> REF_NGRAM_COUNT_MAP_COLLECTOR = Collectors
			.toMap(Entry::getKey, entry -> {
				final Session session = entry.getKey();
				List<List<String>> ngrams = entry.getValue();
				Map<VisualizableReferent, Object2IntMap<List<String>>> refNgramCounts = createReferentNgramCountMap(
						session, ngrams);
				return new ReferentNGramCounts(ngrams, refNgramCounts);
			});

	private static Map<Session, ReferentNGramCounts> createReferentNgramCountMap(
			final Map<Session, List<List<String>>> sessionNgrams) {
		return sessionNgrams.entrySet().stream().collect(REF_NGRAM_COUNT_MAP_COLLECTOR);
	}

	private static Map<VisualizableReferent, Object2IntMap<List<String>>> createReferentNgramCountMap(
			final Session session, List<List<String>> ngrams) {
		final List<Round> rounds = session.getRounds();
		final Map<VisualizableReferent, Object2IntMap<List<String>>> result = new HashMap<>();
		for (final Round round : rounds) {
			final VisualizableReferent[] refs = round.getReferents().stream().filter(Referent::isTarget)
					.map(VisualizableReferent::fetch).toArray(VisualizableReferent[]::new);
			for (VisualizableReferent ref : refs) {
				final Object2IntMap<List<String>> ngramCounts = result.computeIfAbsent(ref, key -> {
					final Object2IntOpenHashMap<List<String>> newCountMap = new Object2IntOpenHashMap<>();
					newCountMap.defaultReturnValue(0);
					return newCountMap;
				});
				for (List<String> ngram : ngrams) {
					incrementCount(ngram, ngramCounts);
				}
			}
		}
		return result;
	}

	private static final Function<List<String>, List<List<String>>> NGRAM_FACTORY = new NGramFactory();

	private static Stream<List<String>> createNgrams(final Round round) {
		final Stream<List<String>> uttTokenSeqs = round.getUtts().stream().map(Utterance::getReferringTokens);
		return uttTokenSeqs.map(NGRAM_FACTORY).flatMap(List::stream);
	}

	private static Comparator<Weighted<? extends List<?>>> createScoredNgramComparator() {
		final Comparator<Weighted<? extends List<?>>> ngramLengthAscending = Comparator
				.comparingInt(scoredNgram -> scoredNgram.getWrapped().size());
		final Comparator<Weighted<? extends List<?>>> normalizedWeightAscending = Comparator
				.comparingDouble(TfIdfKeywordVisualizationWriter::normalizeWeight);
		return normalizedWeightAscending.reversed().thenComparing(ngramLengthAscending.reversed());
	}

	private static Set<Referent> getTargetRefs(final Iterable<Round> rounds) {
		final Set<Referent> result = new HashSet<>(2);
		final IntSet refIds = new IntOpenHashSet(1);
		for (final Round round : rounds) {
			final ListIterator<Referent> refIter = round.getReferents().listIterator();
			while (refIter.hasNext()) {
				final Referent ref = refIter.next();
				final int refId = refIter.nextIndex();
				if (ref.isTarget()) {
					result.add(ref);
					refIds.add(refId);
				}
			}
		}
		assert refIds.size() == 1;
		return result;
	}

	private static <K> void incrementCount(final K key, final Object2IntMap<? super K> counts) {
		final int oldValue = counts.getInt(key);
		final int oldValue2 = counts.put(key, oldValue + 1);
		assert oldValue == oldValue2;
	}

	private static double normalizeWeight(final Weighted<? extends Collection<?>> weightedColl) {
		final double weight = weightedColl.getWeight();
		final Collection<?> wrapped = weightedColl.getWrapped();
		return weight / wrapped.size();
	}

	private static class ReferentNGramCounts {

		/**
		 * @return the refNgramCounts
		 */
		public Map<VisualizableReferent, Object2IntMap<List<String>>> getRefNgramCounts() {
			return refNgramCounts;
		}

		/**
		 * @return the allNgrams
		 */
		public List<List<String>> getAllNgrams() {
			return allNgrams;
		}

		private final Map<VisualizableReferent, Object2IntMap<List<String>>> refNgramCounts;

		private final List<List<String>> allNgrams;

		private ReferentNGramCounts(final List<List<String>> allNgrams,
				final Map<VisualizableReferent, Object2IntMap<List<String>>> refNgramCounts) {
			this.allNgrams = allNgrams;
			this.refNgramCounts = refNgramCounts;
		}

	}

	private final Map<Session, ReferentNGramCounts> sessionRefNgramCounts;

	private final TfIdfCalculator<List<String>> tfidfCalculator;

	public TfIdfKeywordVisualizationWriter(final Path imgResDir,
			final Map<Session, ReferentNGramCounts> sessionRefNgramCounts,
			final TfIdfCalculator<List<String>> tfidfCalculator) {
		this.imgResDir = imgResDir;
		this.sessionRefNgramCounts = sessionRefNgramCounts;
		this.tfidfCalculator = tfidfCalculator;
	}

	private static class RefWeightedNGramCountMapComparator
			implements Comparator<Entry<VisualizableReferent, Object2IntMap<List<String>>>> {

		private final ToDoubleFunction<List<String>> ngramWeighter;

		private double score(Object2IntMap<List<String>> ngramCounts) {
			return ngramCounts.object2IntEntrySet().stream().mapToDouble(entry -> {
				final List<String> ngram = entry.getKey();
				final double weight = ngramWeighter.applyAsDouble(ngram);
				final int count = entry.getIntValue();
				return weight * count;
			}).sum();
		}

		private RefWeightedNGramCountMapComparator(final ToDoubleFunction<List<String>> ngramWeighter) {
			this.ngramWeighter = ngramWeighter;
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see java.util.Comparator#compare(java.lang.Object, java.lang.Object)
		 */
		@Override
		public int compare(Entry<VisualizableReferent, Object2IntMap<List<String>>> o1,
				Entry<VisualizableReferent, Object2IntMap<List<String>>> o2) {
			double score1 = score(o1.getValue());
			double score2 = score(o2.getValue());
			return Double.compare(score1, score2);
		}

	}

	public int write(final CSVPrinter printer) throws IOException {
		int result = 0;
		for (final Entry<Session, ReferentNGramCounts> entry : sessionRefNgramCounts.entrySet()) {
			final Session session = entry.getKey();
			final ReferentNGramCounts refData = entry.getValue();
			Map<VisualizableReferent, Object2IntMap<List<String>>> refNgramCounts = refData.getRefNgramCounts();
			final Comparator<Entry<VisualizableReferent, Object2IntMap<List<String>>>> refWeightedNgramCountComparator = new RefWeightedNGramCountMapComparator(
					word -> tfidfCalculator.applyAsDouble(word, session)).reversed();
			Stream<Entry<VisualizableReferent, Object2IntMap<List<String>>>> refsSortedByNgramWeight = refNgramCounts
					.entrySet().stream().sorted(refWeightedNgramCountComparator);
			refsSortedByNgramWeight.forEach(ref -> createRows(ref));

			// final Stream<Weighted<List<String>>> scoredNgrams =
			// refData.getAllNgrams().stream().distinct()
			// .map(ngram -> new Weighted<>(ngram,
			// ngramWeighter.applyAsDouble(ngram)))
			// .sorted(SCORED_NGRAM_COMPARATOR);

			// final Stream<Stream<String>> cellValues =
			// scoredNgrams.map(scoredNgram -> createRow(session, scoredNgram));
			// final List<String[]> rows = Arrays
			// .asList(cellValues.map(stream ->
			// stream.toArray(String[]::new)).toArray(String[][]::new));
			// printer.printRecords(rows);
			// result += rows.size();
		}
		return result;
	}

	private final Path imgResDir;

	private String createShapeResourceLocator(String shape) {
		Path imgFilePath = imgResDir.resolve(shape + ".svg");
		URI shapeResLoc = imgFilePath.toUri();
		return shapeResLoc.toString();
	}

	private void createRows(Entry<VisualizableReferent, Object2IntMap<List<String>>> refNgramCounts) {
		JSVGCanvas svgCanvas = new JSVGCanvas();
		VisualizableReferent ref = refNgramCounts.getKey();
		String shape = ref.getShape();
		final String uriString = createShapeResourceLocator(shape);
		LOGGER.debug("Loading image from \"{}\".", uriString);
		svgCanvas.setURI(uriString);
		svgCanvas.setDocumentState(JSVGCanvas.ALWAYS_DYNAMIC);
		svgCanvas.addSVGDocumentLoaderListener(new SVGDocumentLoaderAdapter() {

			/*
			 * (non-Javadoc)
			 *
			 * @see org.apache.batik.swing.svg.SVGDocumentLoaderAdapter#
			 * documentLoadingCompleted(org.apache.batik.swing.svg. SVGDocumentLoaderEvent)
			 */
			@Override
			public void documentLoadingCompleted(final SVGDocumentLoaderEvent e) {
				final SVGDocument doc = e.getSVGDocument();

				final NodeList pathNodes = doc.getElementsByTagName("path");
				for (int pathNodeIdx = 0; pathNodeIdx < pathNodes.getLength(); ++pathNodeIdx) {
					final SVGOMPathElement pathNode = (SVGOMPathElement) pathNodes.item(pathNodeIdx);
					// CSSStyleDeclaration style = pathNode.getStyle();
					// System.out.println(style);
					final NamedNodeMap pathNodeAttrs = pathNode.getAttributes();
					final Node styleAttrNode = pathNodeAttrs.getNamedItem("style");
					final String styleStr = styleAttrNode.getTextContent();
					// System.out.println(styleStr);
					styleAttrNode.setTextContent(styleStr + ";fill:purple");

					// final Node transformAttr =
					// pathNodeAttrs.getNamedItem("transform");
					// final String transformStr =
					// transformAttr.getTextContent();
					// final String scaledTransformStr = transformStr + "
					// scale(1.0)";
					// transformAttr.setTextContent(scaledTransformStr);
				}
				final NodeList svgNodes = doc.getElementsByTagName("svg");
				for (int svgNodeIdx = 0; svgNodeIdx < svgNodes.getLength(); ++svgNodeIdx) {
					final SVGSVGElement svgNode = (SVGSVGElement) svgNodes.item(svgNodeIdx);
					final SVGAnimatedRect viewBox = svgNode.getViewBox();
					final SVGRect viewBoxVal = viewBox.getBaseVal();
					final float newWidth = viewBoxVal.getWidth() * 2;
					viewBoxVal.setWidth(newWidth);
					final float newHeight = viewBoxVal.getHeight() * 2;
					viewBoxVal.setHeight(newHeight);
					// svgNode.createSVGTransform().setScale(2.0f, 2.0f);
					System.out.println(svgNode);
					final NamedNodeMap svgAttrs = svgNode.getAttributes();
					final Node widthAttrNode = svgAttrs.getNamedItem("width");
					final String width = widthAttrNode.getTextContent();
					System.out.println("old width:" + width);
					// widthAttrNode.setTextContent("100%");
					// widthAttrNode.setTextContent("1000mm");
					// widthAttrNode.setTextContent(newWidth + "mm");
					System.out.println("new width:" + widthAttrNode.getTextContent());
					final Node heightAttrNode = svgAttrs.getNamedItem("height");
					final String height = heightAttrNode.getTextContent();
					System.out.println("old height:" + height);
					// heightAttrNode.setTextContent("100%");
					// heightAttrNode.setTextContent("2000mm");
					// heightAttrNode.setTextContent(newHeight + "mm");
					// svgAttrs.removeNamedItem("height");
					System.out.println("new height:" + heightAttrNode.getTextContent());
					// Node viewBoxAttr = svgAttrs.getNamedItem("viewBox");
					// String viewBoxAttrStr = viewBoxAttr.getTextContent();
					// viewBoxAttr.setTextContent("0 0 " + width + " " +
					// height);
				}

				final SVGSVGElement rootElem = doc.getRootElement();
				rootElem.createSVGTransform().setScale(2.0f, 2.0f);
				// rootElem.trans
				// rootElem.forceRedraw();

				// System.out.println("currentScale:" +
				// rootElem.getCurrentScale());
				// rootElem.
				// rootElem.createSVGTransform()
				// rootElem.getHeight();
				// rootElem.setCurrentScale(2.0f);

				// EventQueue.invokeLater(()-> {
				// JFrame conv = new JFrame("Converted");
				// JSVGCanvas convCanvas = new JSVGCanvas();
				// conv.add(convCanvas);
				// convCanvas.setDocumentState(JSVGComponent.ALWAYS_DYNAMIC);
				// convCanvas.setSVGDocument(doc);
				// convCanvas.addSVGDocumentLoaderListener(new
				// SVGDocumentLoaderAdapter(){
				//
				// });
				// conv.pack();
				//// conv.setLocation(null);
				// conv.setVisible(true);
				// });

				// try {
				// BufferedImage img = convertSVGToPNG(doc);
				// EventQueue.invokeLater(() -> {
				// JFrame c = new JFrame("Converted");
				// c.add(new JLabel(new ImageIcon(img)));
				// c.pack();
				// c.setLocationByPlatform(true);
				// c.setVisible(true);
				// });
				// } catch (IOException e1) {
				// throw new UncheckedIOException(e1);
				// } catch (TranscoderException e1) {
				// throw new RuntimeException(e1);
				// }

				// canvas.setSVGDocument(doc);
				// f.invalidate();
				// canvas.repaint();
			}

		});
		final JFrame frame = new JFrame("Image viewer");
		frame.add(svgCanvas);
		frame.pack();
		// f.setLocationByPlatform(true);
		frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
		frame.setVisible(true);
//		final TikzGraphics2D t = new TikzGraphics2D();
//		LOGGER.info("Painting component.");
//		t.paintComponent(frame);
//		LOGGER.info("Finished painting component.");
		frame.dispose();
	}

	// private Stream<String> createRow(final Session session, final
	// Weighted<List<String>> scoredNgram) {
	// final List<String> ngram = scoredNgram.getWrapped();
	// final double weight = scoredNgram.getWeight();
	// final String ngramRepr = ngram.stream().collect(TOKEN_JOINER);
	// final Object2IntMap<List<String>> ngramCounts =
	// sessionRefNgramCounts.get(session).get(ngram);
	// return Stream.of(session.getName(), ngramRepr, weight, ngram.size(),
	// normalizeWeight(scoredNgram))
	// .map(Object::toString);
	// }

}
