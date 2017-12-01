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
package se.kth.speech.coin.tangrams.wac.data;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import se.kth.speech.Lists;
import se.kth.speech.coin.tangrams.TokenSequenceSingletonFactory;

/**
 * @author <a href="mailto:tcshore@kth.se">Todd Shore</a>
 * @since Nov 23, 2017
 *
 */
public final class UtteranceTabularDataReader { // NO_UCD (use default)

	// @formatter:off
	private enum Header {
		ROUND, SPEAKER, DIALOGUE_ROLE, START_TIME, END_TIME, TOKENS;
	}
	// @formatter:on

	private static final String DEFAULT_INSTRUCTOR_ROLE_NAME = "INSTRUCTOR";

	private static final Charset DEFAULT_INFILE_CHARSET = StandardCharsets.UTF_8;

	private static final CSVFormat FORMAT = CSVFormat.TDF.withHeader(Header.class).withSkipHeaderRecord();
	
	private static final Logger LOGGER = LoggerFactory.getLogger(UtteranceTabularDataReader.class);

	private static TokenSequenceSingletonFactory createDefaultTokenSeqTransformer() {
		return new TokenSequenceSingletonFactory();
	}

	private final Function<? super String, List<String>> tokenSeqFactory;

	private final String instructorRoleName;

	private final Function<? super List<String>, List<String>> referringTokenSeqFactory;

	public UtteranceTabularDataReader(final Function<? super List<String>, List<String>> referringTokenSeqFactory) {
		this(createDefaultTokenSeqTransformer(), referringTokenSeqFactory, DEFAULT_INSTRUCTOR_ROLE_NAME);
	}

	public UtteranceTabularDataReader(final Function<? super String, List<String>> tokenSeqFactory,
			final Function<? super List<String>, List<String>> referringTokenSeqFactory) {
		this(tokenSeqFactory, referringTokenSeqFactory, DEFAULT_INSTRUCTOR_ROLE_NAME);
	}

	public UtteranceTabularDataReader(final Function<? super String, List<String>> tokenSeqFactory,
			final Function<? super List<String>, List<String>> referringTokenSeqFactory,
			final String instructorRoleName) {
		this.tokenSeqFactory = tokenSeqFactory;
		this.referringTokenSeqFactory = referringTokenSeqFactory;
		this.instructorRoleName = instructorRoleName;
	}

	public List<ArrayList<Utterance>> apply(final Path infilePath) throws IOException {
		return apply(infilePath, DEFAULT_INFILE_CHARSET);
	}

	public List<ArrayList<Utterance>> apply(final Path infilePath, final Charset encoding) throws IOException {
		LOGGER.debug("Reading events file at \"{}\" with encoding \"{}\".", infilePath, encoding);
		try (BufferedReader reader = Files.newBufferedReader(infilePath, encoding)) {
			return apply(reader);
		}
	}

	public List<ArrayList<Utterance>> apply(final Reader reader) throws IOException {
		final CSVParser parser = FORMAT.parse(reader);
		final List<ArrayList<Utterance>> result = new ArrayList<>(75);
		for (final CSVRecord record : parser) {
			final String speakerId = record.get(Header.SPEAKER);
			final String diagRole = record.get(Header.DIALOGUE_ROLE);
			final boolean isInstructor = Objects.equals(instructorRoleName, diagRole);
			final float startTime = Float.parseFloat(record.get(Header.START_TIME));
			final float endTime = Float.parseFloat(record.get(Header.END_TIME));
			final List<String> tokens = tokenSeqFactory.apply(record.get(Header.TOKENS));
			final List<String> referringTokens = referringTokenSeqFactory.apply(tokens);
			final Utterance utt = new Utterance(startTime, endTime, speakerId, isInstructor, tokens, referringTokens);

			final int roundId = Integer.parseInt(record.get(Header.ROUND));
			Lists.ensureIndexSuppliedValues(result, roundId, () -> new ArrayList<>());
			final List<Utterance> roundUtts = result.get(roundId);
			roundUtts.add(utt);
		}
		LOGGER.debug("Parsed rounds up to ID {}.", result.size());
		return result;
	}

}
