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
import java.io.UncheckedIOException;
import java.io.Writer;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;
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
import org.w3c.dom.svg.SVGSVGElement;

import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import it.unimi.dsi.fastutil.objects.Object2DoubleMap;
import it.unimi.dsi.fastutil.objects.Object2DoubleOpenHashMap;
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
import se.kth.speech.coin.tangrams.wac.logistic.Weighted;
import se.kth.speech.function.ThrowingSupplier;

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
			formatter.printHelp(TfIdfKeywordVisualizationWriter.class.getName() + " INPATHS...", OPTIONS);
		}

		protected final String optName;

		private Parameter(final String optName) {
			this.optName = optName;
		}

	}

	private static class ReferentNGramCounts {

		private final List<List<String>> allNgrams;

		private final Map<VisualizableReferent, Object2IntMap<List<String>>> refNgramCounts;

		private ReferentNGramCounts(final List<List<String>> allNgrams,
				final Map<VisualizableReferent, Object2IntMap<List<String>>> refNgramCounts) {
			this.allNgrams = allNgrams;
			this.refNgramCounts = refNgramCounts;
		}

		/**
		 * @return the allNgrams
		 */
		public List<List<String>> getAllNgrams() {
			return allNgrams;
		}

		/**
		 * @return the refNgramCounts
		 */
		public Map<VisualizableReferent, Object2IntMap<List<String>>> getRefNgramCounts() {
			return refNgramCounts;
		}

	}

	private static class VisualizableReferent {

		private static final Map<Referent, VisualizableReferent> INSTANCES = new ConcurrentHashMap<>();

		private static VisualizableReferent fetch(final Referent ref) {
			return INSTANCES.computeIfAbsent(ref, VisualizableReferent::new);
		}

		private final int blue;

		private final int green;

		private final float hue;

		private final int red;

		private final String shape;

		private VisualizableReferent(final Referent ref) {
			blue = ref.getBlueInt();
			green = ref.getGreenInt();
			hue = ref.getHue();
			red = ref.getRedInt();
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
		public int getBlue() {
			return blue;
		}

		/**
		 * @return the green
		 */
		public int getGreen() {
			return green;
		}

		/**
		 * @return the red
		 */
		public int getRed() {
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

	private static class WeightedNGramCountMapScorer implements ToDoubleFunction<Object2IntMap<List<String>>> {

		private final ToDoubleFunction<List<String>> ngramScorer;

		// private final Object2DoubleMap<Object2IntMap<List<String>>>
		// resultCache;

		private WeightedNGramCountMapScorer(final ToDoubleFunction<List<String>> ngramScorer) {
			this.ngramScorer = ngramScorer;
			// this.resultCache = new Object2DoubleOpenHashMap<>(50);
			// resultCache.defaultReturnValue(-1.0);
		}

		/*
		 * (non-Javadoc)
		 *
		 * @see java.util.function.ToDoubleFunction#applyAsDouble(java.lang.Object)
		 */
		@Override
		public double applyAsDouble(final Object2IntMap<List<String>> ngramCounts) {
			// return resultCache.computeDoubleIfAbsent(ngramCounts,
			// this::score);
			return score(ngramCounts);
		}

		private double score(final Object2IntMap<List<String>> ngramCounts) {
			final double score = ngramCounts.object2IntEntrySet().stream().mapToDouble(entry -> {
				final List<String> ngram = entry.getKey();
				final double weight = ngramScorer.applyAsDouble(ngram);
				final int count = entry.getIntValue();
				return weight * count;
			}).sum();
			// TODO: Normalize the score as a log function of the ngram order
			// (i.e. length). Maybe score / (len(order) + log(order)) ?
			assert score >= 0.0;
			return score;
		}

	}

	private static final Logger LOGGER = LoggerFactory.getLogger(TfIdfKeywordVisualizationWriter.class);

	private static final Collector<Entry<Session, List<List<String>>>, ?, Map<Session, ReferentNGramCounts>> REF_NGRAM_COUNT_MAP_COLLECTOR = Collectors
			.toMap(Entry::getKey, entry -> {
				final Session session = entry.getKey();
				final List<List<String>> ngrams = entry.getValue();
				final Map<VisualizableReferent, Object2IntMap<List<String>>> refNgramCounts = createReferentNgramCountMap(
						session, ngrams);
				return new ReferentNGramCounts(ngrams, refNgramCounts);
			});

	private static final Comparator<String> SESSION_NAME_COMPARATOR = new Comparator<String>() {

		@Override
		public int compare(final String o1, final String o2) {
			// NOTE: This comparison only works for session names which
			// represent integer values
			final int i1 = Integer.parseInt(o1);
			final int i2 = Integer.parseInt(o2);
			return Integer.compare(i1, i2);
		}

	};

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
				final Map<Session, List<List<String>>> sessionNgrams = createSessionNgramMap(
						new SessionSetReader(refTokenFilePath).apply(inpaths).getSessions(), ngramFactory);
				final Map<Session, ReferentNGramCounts> sessionRefNgramCounts = createReferentNgramCountMap(
						sessionNgrams);

				LOGGER.info("Calculating TF-IDF scores.");
				final long tfIdfCalculatorConstructionStart = System.currentTimeMillis();
				final TfIdfCalculator<Session, List<String>> tfIdfCalculator = TfIdfCalculator.create(sessionNgrams,
						onlyInstructor, tfVariant);
				LOGGER.info("Finished calculating TF-IDF scores after {} seconds.",
						(System.currentTimeMillis() - tfIdfCalculatorConstructionStart) / 1000.0);

				final long nbestRefs = 3;
				final long nbestNgrams = 3;
				LOGGER.info("Printing {} best referents and {} n-grams for each referent for each dyad.", nbestRefs,
						nbestNgrams);
				final TfIdfKeywordVisualizationWriter keywordWriter = new TfIdfKeywordVisualizationWriter(imgResDir,
						sessionRefNgramCounts, tfIdfCalculator, nbestRefs, nbestNgrams);

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

	private static String createColorHexCode(final int r, final int g, final int b) {
		// https://stackoverflow.com/a/3607942/1391325
		return String.format("#%02x%02x%02x", r, g, b);
	}

	private static String createColorHexCode(final VisualizableReferent ref) {
		return createColorHexCode(ref.getRed(), ref.getGreen(), ref.getBlue());
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

	private static Object2DoubleMap<Object2IntMap.Entry<List<String>>> createNgramCountScoreMap(
			final Object2IntMap<List<String>> ngramCounts, final ToDoubleFunction<List<String>> ngramScorer) {
		final Object2DoubleMap<Object2IntMap.Entry<List<String>>> result = new Object2DoubleOpenHashMap<>(
				ngramCounts.size());
		result.defaultReturnValue(-1.0);
		for (final Object2IntMap.Entry<List<String>> ngramCount : ngramCounts.object2IntEntrySet()) {
			final double weightedScore = ngramScorer.applyAsDouble(ngramCount.getKey()) * ngramCount.getIntValue();
			result.put(ngramCount, weightedScore);
		}
		return result;
	}

	private static Stream<List<String>> createNgrams(final Round round, final NGramFactory ngramFactory) {
		final Stream<List<String>> uttTokenSeqs = round.getUtts().stream().map(Utterance::getReferringTokens);
		return uttTokenSeqs.map(ngramFactory).flatMap(List::stream);
	}

	private static Stream<List<String>> createNgrams(final Session session, final NGramFactory ngramFactory) {
		return session.getRounds().stream().flatMap(round -> createNgrams(round, ngramFactory));
	}

	private static Map<Session, ReferentNGramCounts> createReferentNgramCountMap(
			final Map<Session, List<List<String>>> sessionNgrams) {
		return sessionNgrams.entrySet().stream().collect(REF_NGRAM_COUNT_MAP_COLLECTOR);
	}

	private static Map<VisualizableReferent, Object2IntMap<List<String>>> createReferentNgramCountMap(
			final Session session, final List<List<String>> ngrams) {
		final List<Round> rounds = session.getRounds();
		final Map<VisualizableReferent, Object2IntMap<List<String>>> result = new HashMap<>();
		for (final Round round : rounds) {
			final VisualizableReferent[] refs = round.getReferents().stream().filter(Referent::isTarget)
					.map(VisualizableReferent::fetch).toArray(VisualizableReferent[]::new);
			for (final VisualizableReferent ref : refs) {
				final Object2IntMap<List<String>> ngramCounts = result.computeIfAbsent(ref, key -> {
					final Object2IntOpenHashMap<List<String>> newCountMap = new Object2IntOpenHashMap<>();
					newCountMap.defaultReturnValue(0);
					return newCountMap;
				});
				for (final List<String> ngram : ngrams) {
					incrementCount(ngram, ngramCounts);
				}
			}
		}
		return result;
	}

	private static Map<VisualizableReferent, SVGSVGElement> createRefSVGRootElementMap(
			final Collection<Session> sessions, final Path imgResDir) {
		final Iterable<VisualizableReferent> uniqueRefs = sessions.stream().map(Session::getRounds)
				.flatMap(List::stream).map(Round::getReferents).flatMap(List::stream).map(VisualizableReferent::new)
				.distinct()::iterator;
		final Map<VisualizableReferent, SVGSVGElement> result = new HashMap<>(
				HashedCollections.capacity(sessions.size()));
		// int nextDocId = 1;
		for (final VisualizableReferent ref : uniqueRefs) {
			final SVGDocument doc = createSVGDocument(ref, imgResDir);
			final SVGSVGElement rootElem = doc.getRootElement();
			// rootElem.setId("svg-" + Integer.toString(nextDocId++));
			result.put(ref, rootElem);
		}
		return result;
	}

	private static Comparator<Weighted<? extends List<?>>> createScoredNgramComparator() {
		final Comparator<Weighted<? extends List<?>>> ngramLengthAscending = Comparator
				.comparingInt(scoredNgram -> scoredNgram.getWrapped().size());
		final Comparator<Weighted<? extends List<?>>> normalizedWeightAscending = Comparator
				.comparingDouble(TfIdfKeywordVisualizationWriter::normalizeWeight);
		return normalizedWeightAscending.reversed().thenComparing(ngramLengthAscending.reversed());
	}

	private static Map<Session, List<List<String>>> createSessionNgramMap(final Collection<Session> sessions,
			final NGramFactory ngramFactory) {
		final Map<Session, List<List<String>>> result = new HashMap<>(
				Math.toIntExact(Math.round(Math.ceil(sessions.size() * 1.25))));
		sessions.forEach(
				session -> result.put(session, createNgrams(session, ngramFactory).collect(Collectors.toList())));
		return result;
	}

	private static SVGDocument createSVGDocument(final VisualizableReferent ref, final Path imgResDir)
			throws UncheckedIOException {
		final Path imgFilePath = imgResDir.resolve(ref.getShape() + ".svg");
		LOGGER.debug("Loading image from \"{}\".", imgFilePath);
		final SVGDocument result;
		try {
			result = SVGDocuments.read(imgFilePath);
		} catch (final IOException e) {
			throw new UncheckedIOException(e);
		}
		SVGDocuments.setPathStyles(result, "fill", createColorHexCode(ref));
		SVGDocuments.setSize(result, "100%", "100px");
		return result;
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

	private static double normalizeNgramScore(final Weighted<? extends Collection<?>> weightedColl) {
		final double weight = weightedColl.getWeight();
		final Collection<?> wrapped = weightedColl.getWrapped();
		return weight / wrapped.size();
	}

	private static double normalizeWeight(final Weighted<? extends Collection<?>> weightedColl) {
		final double weight = weightedColl.getWeight();
		final Collection<?> wrapped = weightedColl.getWrapped();
		return weight / wrapped.size();
	}

	private final long nbestNgrams;

	private final long nbestRefs;

	private final Map<VisualizableReferent, SVGSVGElement> refSvgRootElems;

	private final Map<Session, ReferentNGramCounts> sessionRefNgramCounts;

	private final TfIdfCalculator<Session, List<String>> tfidfCalculator;

	public TfIdfKeywordVisualizationWriter(final Path imgResDir,
			final Map<Session, ReferentNGramCounts> sessionRefNgramCounts,
			final TfIdfCalculator<Session, List<String>> tfidfCalculator, final long nbestRefs, final long nbestNgrams) {
		this.sessionRefNgramCounts = sessionRefNgramCounts;
		refSvgRootElems = createRefSVGRootElementMap(sessionRefNgramCounts.keySet(), imgResDir);
		this.tfidfCalculator = tfidfCalculator;
		this.nbestRefs = nbestRefs;
		this.nbestNgrams = nbestNgrams;
	}

	public int write(final Writer writer) throws IOException {
		final Iterable<Entry<Session, ReferentNGramCounts>> sortedEntries = sessionRefNgramCounts.entrySet().stream()
				.sorted(Comparator.comparing(entry -> entry.getKey().getName(), SESSION_NAME_COMPARATOR))::iterator;
		final Stream.Builder<ContainerTag> rowArrayBuiler = Stream.builder();
		for (final Entry<Session, ReferentNGramCounts> entry : sortedEntries) {
			final Session session = entry.getKey();
			final ReferentNGramCounts refData = entry.getValue();
			final Map<VisualizableReferent, Object2IntMap<List<String>>> refNgramCounts = refData.getRefNgramCounts();
			final ToDoubleFunction<List<String>> ngramScorer = word -> tfidfCalculator.applyAsDouble(word, session);
			final ToDoubleFunction<Object2IntMap<List<String>>> ngramCountScorer = new WeightedNGramCountMapScorer(
					ngramScorer);
			final Comparator<Entry<VisualizableReferent, Object2IntMap<List<String>>>> bestScoringRefWeightedNgramCountComparator = Comparator
					.comparing(Entry::getValue, Comparator.comparingDouble(ngramCountScorer).reversed());
			final Iterable<Entry<VisualizableReferent, Object2IntMap<List<String>>>> refsSortedByNgramWeight = (Iterable<Entry<VisualizableReferent, Object2IntMap<List<String>>>>) refNgramCounts
					.entrySet().stream().limit(nbestRefs).sorted(bestScoringRefWeightedNgramCountComparator)::iterator;

			final String dyadId = session.getName();

			for (final Entry<VisualizableReferent, Object2IntMap<List<String>>> weightedRef : refsSortedByNgramWeight) {
				createRows(dyadId, weightedRef, ngramScorer).forEach(rowArrayBuiler);
			}
		}

		final ContainerTag thead = TagCreator.thead(TagCreator.tr(TagCreator.td("Dyad"), TagCreator.td("Image"),
				TagCreator.td("N-gram"), TagCreator.td("Score"), TagCreator.td("Count")));
		final ContainerTag[] rows = rowArrayBuiler.build().toArray(ContainerTag[]::new);
		final ContainerTag tbody = TagCreator.tbody(rows);
		final ContainerTag table = TagCreator.table(thead, tbody);
		final ContainerTag html = TagCreator.html(createHTMLHeadTag(), TagCreator.body(table));
		final String docStr = createHTMLDocumentString(html);
		writer.write(docStr);
		return rows.length;
	}

	private List<ContainerTag> createRows(final String dyadId,
			final Entry<VisualizableReferent, Object2IntMap<List<String>>> refNgramCounts,
			final ToDoubleFunction<List<String>> ngramScorer) throws IOException {
		final VisualizableReferent ref = refNgramCounts.getKey();
		final SVGSVGElement refSvgRootElem = refSvgRootElems.get(ref);
		final UnescapedText svgTag = TagCreator.rawHtml(createXMLString(refSvgRootElem));

		final Object2IntMap<List<String>> ngramCounts = refNgramCounts.getValue();
		final Object2DoubleMap<Object2IntMap.Entry<List<String>>> ngramCountScores = createNgramCountScoreMap(
				ngramCounts, ngramScorer);
		final Comparator<Object2DoubleMap.Entry<it.unimi.dsi.fastutil.objects.Object2IntMap.Entry<List<String>>>> highestScoreFirstEntryComparator = Comparator
				.comparingDouble(entry -> -entry.getDoubleValue());
		final Iterable<Object2DoubleMap.Entry<it.unimi.dsi.fastutil.objects.Object2IntMap.Entry<List<String>>>> sortedNgramCountScores = ngramCountScores
				.object2DoubleEntrySet().stream().sorted(highestScoreFirstEntryComparator).limit(nbestNgrams)::iterator;

		final List<ContainerTag> rows = new ArrayList<>(ngramCountScores.size());
		for (final Object2DoubleMap.Entry<it.unimi.dsi.fastutil.objects.Object2IntMap.Entry<List<String>>> ngramCountScore : sortedNgramCountScores) {
			final ContainerTag svgCell = TagCreator.td(svgTag);

			final Object2IntMap.Entry<List<String>> ngramCount = ngramCountScore.getKey();
			final List<String> ngram = ngramCount.getKey();
			final int count = ngramCount.getIntValue();
			final double score = ngramCountScore.getDoubleValue();
			final ContainerTag row = TagCreator.tr(TagCreator.td(dyadId), svgCell,
					TagCreator.td(ngram.stream().collect(TOKEN_JOINER)), TagCreator.td(Double.toString(score)),
					TagCreator.td(Integer.toString(count)));
			rows.add(row);
		}

		return rows;
	}

}
