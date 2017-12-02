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
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map.Entry;
import java.util.NavigableMap;
import java.util.TreeMap;
import java.util.function.Supplier;
import java.util.function.ToDoubleFunction;
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
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import se.kth.speech.coin.tangrams.wac.data.Round;
import se.kth.speech.coin.tangrams.wac.data.Session;
import se.kth.speech.coin.tangrams.wac.data.SessionSetReader;
import se.kth.speech.coin.tangrams.wac.data.Utterance;
import se.kth.speech.coin.tangrams.wac.logistic.Weighted;
import weka.core.tokenizers.NGramTokenizer;

/**
 * @author <a href="mailto:tcshore@kth.se">Todd Shore</a>
 * @since Dec 1, 2017
 *
 */
public final class TfIdfKeywordWriter {

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
			formatter.printHelp(TfIdfKeywordWriter.class.getName() + " INPATHS...", OPTIONS);
		}

		protected final String optName;

		private Parameter(final String optName) {
			this.optName = optName;
		}

	}

	private static final Logger LOGGER = LoggerFactory.getLogger(TfIdfKeywordWriter.class);

	private static final String TOKEN_DELIMITER;

	private static final Collector<CharSequence, ?, String> TOKEN_JOINER;

	static {
		TOKEN_DELIMITER = " ";
		TOKEN_JOINER = Collectors.joining(TOKEN_DELIMITER);
	}

	private static final String[] COL_HEADERS = new String[] { "SESSION", "NGRAM", "TF-IDF", "NGRAM_LENGTH",
			"NORMALIZED_TF-IDF" };

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
				final Path refTokenFilePath = ((File) cl.getParsedOptionValue(Parameter.REFERRING_TOKENS.optName))
						.toPath();
				final NavigableMap<Session, List<List<String>>> sessionNgrams = new TreeMap<>(
						Comparator.comparing(Session::getName));

				new SessionSetReader(refTokenFilePath).apply(inpaths).getSessions().forEach(
						session -> sessionNgrams.put(session, createNgrams(session).collect(Collectors.toList())));
				LOGGER.info("Will extract keywords from {} session(s).", sessionNgrams.size());
				final TfIdfCalculator<List<String>> tfidfCalculator = TfIdfCalculator.create(sessionNgrams, false);

				LOGGER.info("Writing to standard output stream.");
				try (CSVPrinter printer = CSVFormat.TDF.withHeader(COL_HEADERS).print(System.out)) {
					for (final Entry<Session, List<List<String>>> entry : sessionNgrams.entrySet()) {
						final Session session = entry.getKey();
						final List<List<String>> ngrams = entry.getValue();
						final ToDoubleFunction<List<String>> ngramWeighter = word -> tfidfCalculator.applyAsDouble(word,
								session);
						final Stream<Weighted<List<String>>> scoredNgrams = ngrams.stream().distinct()
								.map(ngram -> new Weighted<>(ngram, ngramWeighter.applyAsDouble(ngram)))
								.sorted(Comparator.reverseOrder());

						final String sessionName = session.getName();
						final Stream<Stream<String>> cellValues = scoredNgrams
								.map(scoredNgram -> createRow(sessionName, scoredNgram));
						final Stream<List<String>> rows = cellValues.map(stream -> stream.collect(Collectors.toList()));
						printer.printRecords((Iterable<List<String>>) rows::iterator);
					}
				}
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

	private static Stream<List<String>> createNgrams(final List<String> tokenSeq) {
		final NGramTokenizer tokenizer = new NGramTokenizer();
		tokenizer.setDelimiters(TOKEN_DELIMITER);
		tokenizer.setNGramMinSize(1);
		tokenizer.setNGramMaxSize(tokenSeq.size());
		final String inputStr = tokenSeq.stream().collect(TOKEN_JOINER);
		tokenizer.tokenize(inputStr);
		final Stream.Builder<List<String>> resultBuilder = Stream.builder();
		while (tokenizer.hasMoreElements()) {
			final String nextStr = tokenizer.nextElement();
			final List<String> ngram = Arrays.asList(nextStr.split(TOKEN_DELIMITER));
			resultBuilder.accept(ngram);
		}
		return resultBuilder.build();
	}

	private static Stream<List<String>> createNgrams(final Session session) {
		final Stream<List<String>> uttTokenSeqs = session.getRounds().stream().map(Round::getUtts).flatMap(List::stream)
				.map(Utterance::getReferringTokens);
		return uttTokenSeqs.flatMap(TfIdfKeywordWriter::createNgrams);
	}

	private static Stream<String> createRow(final String sessionName, final Weighted<List<String>> scoredNgram) {
		final List<String> ngram = scoredNgram.getWrapped();
		final double weight = scoredNgram.getWeight();
		return Stream.of(sessionName, ngram.stream().collect(TOKEN_JOINER), weight, ngram.size(), weight / ngram.size())
				.map(Object::toString);
	}

	private TfIdfKeywordWriter() {
	}

}
