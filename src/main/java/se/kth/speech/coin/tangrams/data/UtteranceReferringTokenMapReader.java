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
package se.kth.speech.coin.tangrams.data;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * @author <a href="mailto:errantlinguist+github@gmail.com">Todd Shore</a>
 * @since Nov 24, 2017
 *
 */
public final class UtteranceReferringTokenMapReader {

	private static final Charset DEFAULT_ENCODING = StandardCharsets.UTF_8;

	private static final CSVFormat FORMAT = CSVFormat.TDF.withFirstRecordAsHeader();

	private static final int RESULT_MAP_EXPECTED_SIZE = 8095;

	private final static Logger LOGGER = LoggerFactory.getLogger(UtteranceReferringTokenMapReader.class);

	private static TokenSequenceSingletonFactory createDefaultTokenSeqTransformer() {
		return new TokenSequenceSingletonFactory(RESULT_MAP_EXPECTED_SIZE);
	}

	private final Function<? super String, String[]> tokenSeqFactory;

	public UtteranceReferringTokenMapReader() {
		this(createDefaultTokenSeqTransformer());
	}

	public UtteranceReferringTokenMapReader(final Function<? super String,String[]> tokenSeqFactory) { // NO_UCD (use private)
		this.tokenSeqFactory = tokenSeqFactory;
	}

	public Map<List<String>, String[]> apply(final Path infilePath) throws IOException {
		return apply(infilePath, DEFAULT_ENCODING);
	}

	public Map<List<String>, String[]> apply(final Path infilePath, final Charset encoding) throws IOException { // NO_UCD (use private)
		LOGGER.info("Reading referring language from \"{}\" with encoding \"{}\".", infilePath, encoding);
		try (BufferedReader reader = Files.newBufferedReader(infilePath, encoding)) {
			return apply(reader);
		}
	}

	public Map<List<String>, String[]> apply(final Reader reader) throws IOException { // NO_UCD (use private)
		final CSVParser parser = FORMAT.parse(reader);
		final float loadFactor = 0.75f;
		final Map<List<String>, String[]> result = new HashMap<>((int) Math.ceil(RESULT_MAP_EXPECTED_SIZE / loadFactor), loadFactor);
		// final Object2ObjectOpenHashMap<List<String>, List<String>> result =
		// new Object2ObjectOpenHashMap<>(
		// DEFAULT_EXPECTED_UNIQUE_TOKEN_SEQ_COUNT);
		for (final CSVRecord record : parser) {
			final String tokenStr = record.get("TOKENS");
			final List<String> tokens = Arrays.asList(tokenSeqFactory.apply(tokenStr));
			final String refTokenStr = record.get("REFERRING_TOKENS");
			final String[] refTokens = tokenSeqFactory.apply(refTokenStr);
			result.put(tokens, refTokens);
		}
		// result.trim();
		LOGGER.info("Read referring language for {} unique utterance(s).", result.size());
		return result;
	}

}
