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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Stream;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author <a href="mailto:errantlinguist+github@gmail.com">Todd Shore</a>
 * @since Nov 26, 2017
 *
 */
public final class ModelParameterTabularDataReader {

	private static final Logger LOGGER = LoggerFactory.getLogger(ModelParameterTabularDataReader.class);

	private static final Charset DEFAULT_ENCODING = StandardCharsets.UTF_8;

	private static final CSVFormat FORMAT = CSVFormat.TDF.withHeader(createColumnHeaders().toArray(String[]::new))
			.withSkipHeaderRecord();

	private static final ModelParameter[] PARAMS_TO_READ = ModelParameter.values();

	private static final String PARAM_SET_NAME_COL_NAME = "NAME";

	private static Stream<String> createColumnHeaders() {
		final Stream.Builder<String> resultBuilder = Stream.builder();
		resultBuilder.add(PARAM_SET_NAME_COL_NAME);
		Arrays.stream(PARAMS_TO_READ).map(ModelParameter::toString).forEach(resultBuilder);
		return resultBuilder.build();
	}

	private static Map<ModelParameter, Object> createParamValueMap(final CSVRecord record) {
		final Map<ModelParameter, Object> result = new EnumMap<>(ModelParameter.class);
		Arrays.stream(PARAMS_TO_READ).forEach(param -> {
			final String valueStr = record.get(param);
			assert valueStr != null;
			final Object parsedValue = param.parseValue(valueStr);
			assert parsedValue != null;
			result.put(param, parsedValue);
		});
		return result;
	}

	public Map<String, Map<ModelParameter, Object>> apply(final Path infilePath) throws IOException {
		return apply(infilePath, DEFAULT_ENCODING);
	}

	public Map<String, Map<ModelParameter, Object>> apply(final Path infilePath, final Charset encoding)
			throws IOException {
		LOGGER.info("Reading model parameters from \"{}\" with encoding \"{}\".", infilePath, encoding);
		try (BufferedReader reader = Files.newBufferedReader(infilePath, encoding)) {
			return apply(reader);
		}
	}

	public Map<String, Map<ModelParameter, Object>> apply(final Reader reader) throws IOException {
		final CSVParser parser = FORMAT.parse(reader);
		final Map<String, Map<ModelParameter, Object>> result = new LinkedHashMap<>();
		for (final CSVRecord record : parser) {
			final Map<ModelParameter, Object> newParams = createParamValueMap(record);
			final String name = record.get(PARAM_SET_NAME_COL_NAME);
			final Map<ModelParameter, Object> oldParams = result.put(name, newParams);
			if (oldParams != null) {
				throw new IllegalArgumentException(
						String.format("Duplicate rows for %s \"%s\".", PARAM_SET_NAME_COL_NAME, name));
			}
		}
		LOGGER.debug("Read {} model parameter set(s).", result.size());
		return result;
	}
}
