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
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Map.Entry;
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

import se.kth.speech.Lists;
import se.kth.speech.coin.tangrams.CLIParameters;
import se.kth.speech.function.ThrowingSupplier;

/**
 * Extracts coreference sequences of a given uniform length from files created
 * by {@link WordProbabilityScoreTablularDataWriter}.
 *
 * @author <a href="mailto:errantlinguist+github@gmail.com">Todd Shore</a>
 * @since 5 Dec 2017
 *
 */
public final class WordProbabiltyScoreSequenceWriter {

	private enum Parameter implements Supplier<Option> {
		ENCODING("e") {
			@Override
			public Option get() {
				return Option.builder(optName).longOpt("encoding").desc("The encoding of the file to read.").hasArg()
						.argName("codec").build();
			}
		},
		HELP("?") {
			@Override
			public Option get() {
				return Option.builder(optName).longOpt("help").desc("Prints this message.").build();
			}
		},
		OUTPATH("o") {
			@Override
			public Option get() {
				return Option.builder(optName).longOpt("outfile").desc("The file to write the results to.").hasArg()
						.argName("path").type(File.class).build();
			}
		},
		SEQUENCE_LENGTH("l") {
			@Override
			public Option get() {
				return Option.builder(optName).longOpt("seq-length").desc("The length of the sequences to make.")
						.hasArg().argName("length").type(Number.class).required().build();
			}
		};

		private static final Options OPTIONS = createOptions();

		private static Options createOptions() {
			final Options result = new Options();
			Arrays.stream(Parameter.values()).map(Parameter::get).forEach(result::addOption);
			return result;
		}

		private static Charset parseEncoding(final CommandLine cl, final Charset defaultValue) {
			final String encodingName = cl.getOptionValue(Parameter.ENCODING.optName);
			final Charset result = encodingName == null ? defaultValue : Charset.forName(encodingName);
			return result;
		}

		private static void printHelp() {
			final HelpFormatter formatter = new HelpFormatter();
			formatter.printHelp(WordProbabiltyScoreSequenceWriter.class.getName() + " INPATH", OPTIONS);
		}

		protected final String optName;

		private Parameter(final String optName) {
			this.optName = optName;
		}

	}

	private static class ReferentTokenSequences {

		private final List<List<SequenceDatapoint>> refTokenRows;

		private ReferentTokenSequences() {
			refTokenRows = new ArrayList<>(20);
		}

		@Override
		public boolean equals(final Object obj) {
			if (this == obj) {
				return true;
			}
			if (obj == null) {
				return false;
			}
			if (!(obj instanceof ReferentTokenSequences)) {
				return false;
			}
			final ReferentTokenSequences other = (ReferentTokenSequences) obj;
			if (refTokenRows == null) {
				if (other.refTokenRows != null) {
					return false;
				}
			} else if (!refTokenRows.equals(other.refTokenRows)) {
				return false;
			}
			return true;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + (refTokenRows == null ? 0 : refTokenRows.hashCode());
			return result;
		}

		private void add(final int refIdx, final int tokenSeqIdx, final SequenceDatapoint row) {
			Lists.ensureIndexSuppliedValues(refTokenRows, refIdx, ArrayList::new);
			final List<SequenceDatapoint> tokenSeq = refTokenRows.get(refIdx);
			Lists.ensureIndexNullValues(tokenSeq, tokenSeqIdx);
			final SequenceDatapoint oldRow = tokenSeq.set(tokenSeqIdx, row);
			if (oldRow != null) {
				throw new IllegalArgumentException(
						String.format("Token row already added for entity ID %d and sequence ordinality %d.", refIdx,
								tokenSeqIdx + 1));
			}
		}
	}

	private static class SequenceDatapoint {

		private boolean isInstructor;

		private boolean isTarget;

		private double score;

		private int tokSeqOrdinality;

		private String word;

		private SequenceDatapoint(final int tokSeqOrdinality, final String word, final boolean isInstructor,
				final boolean isTarget, final double score) {
			this.tokSeqOrdinality = tokSeqOrdinality;
			this.word = word;
			this.isInstructor = isInstructor;
			this.isTarget = isTarget;
			this.score = score;
		}

		private SequenceDatapoint(final SequenceDatapoint copyee) {
			this(copyee.tokSeqOrdinality, copyee.word, copyee.isInstructor, copyee.isTarget, copyee.score);
		}

		/**
		 * @return the score
		 */
		private double getScore() {
			return score;
		}

		/**
		 * @return the tokSeqOrdinality
		 */
		private int getTokSeqOrdinality() {
			return tokSeqOrdinality;
		}

		/**
		 * @return the word
		 */
		private String getWord() {
			return word;
		}

		/**
		 * @return the isInstructor
		 */
		private boolean isInstructor() {
			return isInstructor;
		}

		/**
		 * @return the isTarget
		 */
		private boolean isTarget() {
			return isTarget;
		}

		/**
		 * @param isInstructor
		 *            the isInstructor to set
		 */
		private void setInstructor(final boolean isInstructor) {
			this.isInstructor = isInstructor;
		}

		/**
		 * @param score
		 *            the score to set
		 */
		private void setScore(final double score) {
			this.score = score;
		}

		/**
		 * @param isTarget
		 *            the isTarget to set
		 */
		private void setTarget(final boolean isTarget) {
			this.isTarget = isTarget;
		}

		/**
		 * @param tokSeqOrdinality
		 *            the tokSeqOrdinality to set
		 */
		private void setTokSeqOrdinality(final int tokSeqOrdinality) {
			this.tokSeqOrdinality = tokSeqOrdinality;
		}

		/**
		 * @param word
		 *            the word to set
		 */
		private void setWord(final String word) {
			this.word = word;
		}
	}

	private static class UniqueTokenSequenceKey implements Comparable<UniqueTokenSequenceKey> {

		private final int cvId;

		private final String dyad;

		private final float uttEnd;

		private final float uttStart;

		private UniqueTokenSequenceKey(final int cvId, final String dyad, final float uttStart, final float uttEnd) {
			this.cvId = cvId;
			this.dyad = dyad;
			this.uttStart = uttStart;
			this.uttEnd = uttEnd;
		}

		@Override
		public int compareTo(final UniqueTokenSequenceKey o) {
			int result = 0;
			if ((result = Integer.compare(cvId, o.cvId)) == 0) {
				if ((result = dyad.compareTo(o.dyad)) == 0) {
					if ((result = Float.compare(uttStart, o.uttStart)) == 0) {
						result = Float.compare(uttEnd, o.uttEnd);
					}
				}
			}
			return result;
		}

		@Override
		public boolean equals(final Object obj) {
			if (this == obj) {
				return true;
			}
			if (obj == null) {
				return false;
			}
			if (!(obj instanceof UniqueTokenSequenceKey)) {
				return false;
			}
			final UniqueTokenSequenceKey other = (UniqueTokenSequenceKey) obj;
			if (cvId != other.cvId) {
				return false;
			}
			if (dyad == null) {
				if (other.dyad != null) {
					return false;
				}
			} else if (!dyad.equals(other.dyad)) {
				return false;
			}
			if (Float.floatToIntBits(uttEnd) != Float.floatToIntBits(other.uttEnd)) {
				return false;
			}
			if (Float.floatToIntBits(uttStart) != Float.floatToIntBits(other.uttStart)) {
				return false;
			}
			return true;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + cvId;
			result = prime * result + (dyad == null ? 0 : dyad.hashCode());
			result = prime * result + Float.floatToIntBits(uttEnd);
			result = prime * result + Float.floatToIntBits(uttStart);
			return result;
		}

		@Override
		public String toString() {
			final StringBuilder builder = new StringBuilder(128);
			builder.append("UniqueTokenSequenceKey [cvId=");
			builder.append(cvId);
			builder.append(", dyad=");
			builder.append(dyad);
			builder.append(", uttEnd=");
			builder.append(uttEnd);
			builder.append(", uttStart=");
			builder.append(uttStart);
			builder.append("]");
			return builder.toString();
		}
	}

	private static final String COL_DELIM = "\t";

	private static final String[] COL_HEADERS = new String[] { "CROSS_VALIDATION_ITER", "DYAD", "UTT_START_TIME",
			"UTT_END_TIME", "SPLIT_SEQ_NO", "TOKEN_SEQ_ORDINALITY", "WORD", "IS_INSTRUCTOR", "IS_OOV", "ENTITY",
			"IS_TARGET", "PROBABILITY" };

	private static final Logger LOGGER = LoggerFactory.getLogger(WordProbabiltyScoreSequenceWriter.class);

	public static void main(final CommandLine cl) throws IOException, ClassificationException, ParseException {
		if (cl.hasOption(Parameter.HELP.optName)) {
			Parameter.printHelp();
		} else {
			final Path[] inpaths = cl.getArgList().stream().map(Paths::get).toArray(Path[]::new);
			if (inpaths.length < 1) {
				throw new ParseException("No input paths specified.");
			} else if (inpaths.length > 1) {
				throw new ParseException("More than one input path specified.");
			} else {
				final Path inpath = inpaths[0];
				final Charset encoding = Parameter.parseEncoding(cl, StandardCharsets.UTF_8);
				LOGGER.info("Will read data from \"{}\" with encoding \"{}\".", inpath, encoding);
				final int seqLen = ((Number) cl.getParsedOptionValue(Parameter.SEQUENCE_LENGTH.optName)).intValue();
				LOGGER.info("Will create sequences of length {}.", seqLen);
				final ThrowingSupplier<PrintStream, IOException> outStreamGetter = CLIParameters
						.parseOutpath((File) cl.getParsedOptionValue(Parameter.OUTPATH.optName));
				final Map<UniqueTokenSequenceKey, ReferentTokenSequences> rows = readRows(inpath, encoding);

				final WordProbabiltyScoreSequenceWriter writer = new WordProbabiltyScoreSequenceWriter(seqLen);
				writer.accept(rows, outStreamGetter);
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

	private static Map<WordProbabilityScoreDatum, Integer> createColumnIndexMap(final String header) {
		final String[] colNames = header.split(COL_DELIM);
		final Map<WordProbabilityScoreDatum, Integer> result = new EnumMap<>(WordProbabilityScoreDatum.class);
		for (int i = 0; i < colNames.length; ++i) {
			final String colName = colNames[i];
			try {
				final WordProbabilityScoreDatum datum = WordProbabilityScoreDatum.valueOf(colName);
				result.put(datum, i);
			} catch (final IllegalArgumentException e) {
				// Do nothing
			}
		}
		return result;
	}

	private static SequenceDatapoint parseSequenceDatapoint(final String[] row,
			final Map<WordProbabilityScoreDatum, Integer> colIdxs) {
		final int tokenSeqOrdinality = Integer
				.parseInt(row[colIdxs.get(WordProbabilityScoreDatum.TOKEN_SEQ_ORDINALITY)]);
		final String word = row[colIdxs.get(WordProbabilityScoreDatum.WORD)].intern();
		final boolean isInstructor = Boolean.parseBoolean(row[colIdxs.get(WordProbabilityScoreDatum.IS_INSTRUCTOR)]);
		final boolean isTarget = Boolean.parseBoolean(row[colIdxs.get(WordProbabilityScoreDatum.IS_TARGET)]);
		final double score = Double.parseDouble(row[colIdxs.get(WordProbabilityScoreDatum.PROBABILITY)]);
		return new SequenceDatapoint(tokenSeqOrdinality, word, isInstructor, isTarget, score);
	}

	private static Map<UniqueTokenSequenceKey, ReferentTokenSequences> readRows(final Path inpath,
			final Charset encoding) throws IOException {
		LOGGER.info("Reading \"{}\" with encoding \"{}\".", inpath, encoding);

		final Iterator<String> lineIter = Files.lines(inpath, encoding).iterator();
		final Map<WordProbabilityScoreDatum, Integer> colIdxs = createColumnIndexMap(lineIter.next());
		final Map<UniqueTokenSequenceKey, ReferentTokenSequences> result = new HashMap<>(3000);
		while (lineIter.hasNext()) {
			final String line = lineIter.next();
			final String[] row = line.split(COL_DELIM);
			final SequenceDatapoint datapoint = parseSequenceDatapoint(row, colIdxs);
			final int cvId = Integer.parseInt(row[colIdxs.get(WordProbabilityScoreDatum.CROSS_VALIDATION_ITER)]);
			final String dyad = row[colIdxs.get(WordProbabilityScoreDatum.DYAD)].intern();
			final float uttStart = Float.parseFloat(row[colIdxs.get(WordProbabilityScoreDatum.UTT_START_TIME)]);
			final float uttEnd = Float.parseFloat(row[colIdxs.get(WordProbabilityScoreDatum.UTT_END_TIME)]);
			final UniqueTokenSequenceKey mapKey = new UniqueTokenSequenceKey(cvId, dyad, uttStart, uttEnd);
			final ReferentTokenSequences seqRows = result.computeIfAbsent(mapKey, key -> new ReferentTokenSequences());
			final int refIdx = Integer.parseInt(row[colIdxs.get(WordProbabilityScoreDatum.ENTITY)]) - 1;

			seqRows.add(refIdx, datapoint.tokSeqOrdinality - 1, datapoint);
		}
		LOGGER.info("Read datapoints for {} utterances.", result.size());
		return result;
	}

	private final int seqLen;

	public WordProbabiltyScoreSequenceWriter(final int seqLen) {
		this.seqLen = seqLen;
	}

	public void accept(final Map<UniqueTokenSequenceKey, ReferentTokenSequences> rows,
			final ThrowingSupplier<PrintStream, IOException> outStreamGetter) throws IOException {
		LOGGER.info("Writing to output.");
		final Iterator<Entry<UniqueTokenSequenceKey, ReferentTokenSequences>> entryIter = rows.entrySet().stream()
				.sorted(Comparator.comparing(Entry::getKey)).iterator();
		try (PrintStream outStream = outStreamGetter.get()) {
			final CSVPrinter printer = CSVFormat.TDF.withHeader(COL_HEADERS).print(outStream);
			while (entryIter.hasNext()) {
				final Entry<UniqueTokenSequenceKey, ReferentTokenSequences> entry = entryIter.next();
				final UniqueTokenSequenceKey key = entry.getKey();
				final ReferentTokenSequences seqs = entry.getValue();
				writePaddedSequences(key, seqs, printer);
			}
		}
		LOGGER.info("Finished writing.");
	}

	private List<SequenceDatapoint> createPaddedSequence(final List<SequenceDatapoint> seq, final int windowStartIdx,
			final int windowEndIdx) {
		List<SequenceDatapoint> result;
		if (windowStartIdx < 0) {
			result = new ArrayList<>(seqLen);
			final SequenceDatapoint firstSeqDatapoint = seq.get(0);
			for (int tokSeqOrdinality = windowStartIdx + 1; tokSeqOrdinality <= 0; ++tokSeqOrdinality) {
				final SequenceDatapoint padding = new SequenceDatapoint(firstSeqDatapoint);
				padding.setTokSeqOrdinality(tokSeqOrdinality);
				padding.setWord("__PADDING__");
				padding.setScore(0);
				result.add(padding);
			}
			final List<SequenceDatapoint> realDatapoints = seq.subList(0, windowEndIdx);
			result.addAll(realDatapoints);
		} else {
			// No need for padding
			result = seq.subList(windowStartIdx, windowEndIdx);
		}
		assert result.size() == seqLen;
		return result;
	}

	private void writePaddedSequences(final UniqueTokenSequenceKey key, final ReferentTokenSequences seqs,
			final CSVPrinter printer) throws IOException {
		final ListIterator<List<SequenceDatapoint>> refTokSeqIter = seqs.refTokenRows.listIterator();
		do {
			final List<SequenceDatapoint> seq = refTokSeqIter.next();
			final int refId = refTokSeqIter.nextIndex();
			int splitSeqNo = 1;
			for (int windowEndIdx = 1; windowEndIdx <= seq.size(); ++windowEndIdx) {
				final int windowStartIdx = windowEndIdx - seqLen;
				final List<SequenceDatapoint> splitSeq = createPaddedSequence(seq, windowStartIdx, windowEndIdx);
				for (final SequenceDatapoint datapoint : splitSeq) {
					final Stream<String> rowCells = Stream.of(key.cvId, key.dyad, key.uttStart, key.uttEnd, splitSeqNo,
							datapoint.getTokSeqOrdinality(), datapoint.getWord(), datapoint.isInstructor(), refId,
							datapoint.isTarget(), datapoint.getScore()).map(Object::toString);
					printer.printRecord((Iterable<String>) rowCells::iterator);
				}
				splitSeqNo++;
			}

		} while (refTokSeqIter.hasNext());
	}

}
