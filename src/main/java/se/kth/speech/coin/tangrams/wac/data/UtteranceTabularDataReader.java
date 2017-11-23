/*******************************************************************************
 * Copyright 2017 Todd Shore
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License.  You may obtain a copy
 * of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations under
 * the License.
 ******************************************************************************/
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
import java.util.regex.Pattern;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;

/**
 * @author <a href="mailto:tcshore@kth.se">Todd Shore</a>
 * @since Nov 23, 2017
 *
 */
public final class UtteranceTabularDataReader {

	private enum Header {
		ROUND, SPEAKER, DIALOGUE_ROLE, START_TIME, END_TIME, UTTERANCE, REFERRING_TOKENS;
	}

	private static final String DEFAULT_INSTRUCTOR_ROLE_NAME = "INSTRUCTOR";

	private static final Pattern TOKEN_DELIMITER_PATTERN = Pattern.compile("\\s+");

	private static final Charset DEFAULT_INFILE_CHARSET = StandardCharsets.UTF_8;

	private static TokenSequenceSingletonFactory createDefaultTokenSeqTransformer() {
		return new TokenSequenceSingletonFactory(7000);
	}

	private final Function<? super String[], List<String>> tokenSeqTransformer;

	private final String instructorRoleName;

	public UtteranceTabularDataReader() {
		this(DEFAULT_INSTRUCTOR_ROLE_NAME, createDefaultTokenSeqTransformer());
	}

	public UtteranceTabularDataReader(final Function<? super String[], List<String>> tokenSeqTransformer) {
		this(DEFAULT_INSTRUCTOR_ROLE_NAME, tokenSeqTransformer);
	}

	public UtteranceTabularDataReader(final String instructorRoleName,
			final Function<? super String[], List<String>> tokenSeqTransformer) {
		this.instructorRoleName = instructorRoleName;
		this.tokenSeqTransformer = tokenSeqTransformer;
	}

	public List<ArrayList<Utterance>> apply(final Path infilePath) throws IOException {
		return apply(infilePath, DEFAULT_INFILE_CHARSET);
	}

	public List<ArrayList<Utterance>> apply(final Path infilePath, final Charset encoding) throws IOException {
		try (BufferedReader reader = Files.newBufferedReader(infilePath, encoding)) {
			return apply(reader);
		}
	}

	public List<ArrayList<Utterance>> apply(final Reader reader) throws IOException {
		final CSVParser parser = CSVFormat.TDF.withHeader(Header.class).withSkipHeaderRecord().parse(reader);
		final List<ArrayList<Utterance>> result = new ArrayList<>(75);
		for (final CSVRecord record : parser) {
			final String participantId = record.get(Header.SPEAKER);
			final String diagRole = record.get(Header.DIALOGUE_ROLE);
			final boolean isInstructor = Objects.equals(instructorRoleName, diagRole);
			final float startTime = Float.parseFloat(record.get(Header.START_TIME));
			final float endTime = Float.parseFloat(record.get(Header.END_TIME));
			final List<String> tokens = tokenSeqTransformer
					.apply(TOKEN_DELIMITER_PATTERN.split(record.get(Header.UTTERANCE)));
			final List<String> referringTokens = tokenSeqTransformer
					.apply(TOKEN_DELIMITER_PATTERN.split(record.get(Header.REFERRING_TOKENS)));
			final Utterance utt = new Utterance(startTime, endTime, participantId, isInstructor, tokens,
					referringTokens);

			final int roundId = Integer.parseInt(record.get(Header.ROUND));
			Lists.ensureIndexSuppliedValues(result, roundId, () -> new ArrayList<>());
			final List<Utterance> roundUtts = result.get(roundId);
			roundUtts.add(utt);
		}
		return result;
	}

}
