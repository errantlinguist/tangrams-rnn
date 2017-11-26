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
package se.kth.speech.coin.tangrams.io;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;

import se.kth.speech.HashedCollections;
import se.kth.speech.coin.tangrams.TokenSequenceSingletonFactory;

/**
 * @author <a href="mailto:tcshore@kth.se">Todd Shore</a>
 * @since Nov 24, 2017
 *
 */
public final class UtteranceReferringTokenMapReader {

	private static final Charset DEFAULT_ENCODING = StandardCharsets.UTF_8;

	private static final CSVFormat FORMAT = CSVFormat.TDF.withFirstRecordAsHeader();

	private static final int RESULT_MAP_EXPECTED_SIZE = 3096;

	private static TokenSequenceSingletonFactory createDefaultTokenSeqTransformer() {
		return new TokenSequenceSingletonFactory(RESULT_MAP_EXPECTED_SIZE);
	}

	private final Function<? super String, List<String>> tokenSeqFactory;

	public UtteranceReferringTokenMapReader() {
		this(createDefaultTokenSeqTransformer());
	}

	public UtteranceReferringTokenMapReader(final Function<? super String, List<String>> tokenSeqFactory) {
		this.tokenSeqFactory = tokenSeqFactory;
	}

	public Map<List<String>, List<String>> apply(final Path infilePath) throws IOException {
		return apply(infilePath, DEFAULT_ENCODING);
	}

	public Map<List<String>, List<String>> apply(final Path infilePath, final Charset encoding) throws IOException {
		try (BufferedReader reader = Files.newBufferedReader(infilePath, encoding)) {
			return apply(reader);
		}
	}

	public Map<List<String>, List<String>> apply(final Reader reader) throws IOException {
		final CSVParser parser = FORMAT.parse(reader);
		final Map<List<String>, List<String>> result = new HashMap<>(
				HashedCollections.capacity(RESULT_MAP_EXPECTED_SIZE));
		// final Object2ObjectOpenHashMap<List<String>, List<String>> result =
		// new Object2ObjectOpenHashMap<>(
		// DEFAULT_EXPECTED_UNIQUE_TOKEN_SEQ_COUNT);
		for (final CSVRecord record : parser) {
			final String tokenStr = record.get("TOKENS");
			final List<String> tokens = tokenSeqFactory.apply(tokenStr);
			final String refTokenStr = record.get("REFERRING_TOKENS");
			final List<String> refTokens = tokenSeqFactory.apply(refTokenStr);
			result.put(tokens, refTokens);
		}
		// result.trim();
		return result;
	}

}
