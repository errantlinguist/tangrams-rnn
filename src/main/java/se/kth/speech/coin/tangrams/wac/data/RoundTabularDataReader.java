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
import java.util.function.IntFunction;
import java.util.function.Supplier;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;

/**
 * @author <a href="mailto:tcshore@kth.se">Todd Shore</a>
 * @since Nov 23, 2017
 *
 */
public final class RoundTabularDataReader {

	// @formatter:off
	private enum Header {
		EVENT, ROUND, SCORE, TIME, NAME, SUBMITTER, ENTITY, REFERENT, SELECTED, SHAPE, EDGE_COUNT, SIZE, RED, GREEN, BLUE, ALPHA, HUE, SATURATION, BRIGHTNESS, POSITION_X, POSITION_Y;
	}
	// @formatter:on
	
	private static final String DEFAULT_REFERRING_NAME = "nextturn.request";

	private static final Charset DEFAULT_INFILE_CHARSET = StandardCharsets.UTF_8;

	private static final int DEFAULT_EXPECTED_UNIQUE_ENTITY_COUNT = 20;

	private static Referent fetchReferent(final List<Referent> roundEntities, final int entityId, final int roundId) {
		final int entityIdx = entityId - 1;
		Lists.ensureIndexNullValues(roundEntities, entityIdx);
		Referent result = roundEntities.get(entityIdx);
		if (result == null) {
			result = new Referent();
			final Referent oldEntity = roundEntities.set(entityIdx, result);
			assert oldEntity == null;
		} else {
			throw new IllegalArgumentException(
					String.format("Already parsed an entity with ID %d for round %d.", entityId, roundId));
		}
		return result;
	}

	private static Round fetchRound(final List<Round> rounds, final int roundId, final Supplier<Round> roundFactory) {
		Lists.ensureIndexNullValues(rounds, roundId);
		Round result = rounds.get(roundId);
		if (result == null) {
			result = roundFactory.get();
			final Round oldRound = rounds.set(roundId, result);
			assert oldRound == null;
		} else {
			// Do nothing
		}
		return result;
	}

	private final String referringEventName;

	public RoundTabularDataReader() {
		this(DEFAULT_REFERRING_NAME);
	}

	public RoundTabularDataReader(final String referringEventName) {
		this.referringEventName = referringEventName;
	}

	public List<Round> apply(final Path infilePath, final IntFunction<List<Utterance>> roundUttListFactory)
			throws IOException {
		return apply(infilePath, roundUttListFactory, DEFAULT_INFILE_CHARSET);
	}

	public List<Round> apply(final Path infilePath, final IntFunction<List<Utterance>> roundUttListFactory,
			final Charset encoding) throws IOException {
		try (BufferedReader reader = Files.newBufferedReader(infilePath, encoding)) {
			return apply(reader, roundUttListFactory);
		}
	}

	public List<Round> apply(final Reader reader, final IntFunction<List<Utterance>> roundUttListFactory)
			throws IOException {
		final CSVParser parser = CSVFormat.TDF.withHeader(Header.class).withSkipHeaderRecord().parse(reader);
		final List<Round> result = new ArrayList<>(75);
		for (final CSVRecord record : parser) {
			final String eventName = record.get(Header.NAME);
			if (Objects.equals(referringEventName, eventName)) {
				final int score = Integer.parseInt(record.get(Header.SCORE));
				final float time = Float.parseFloat(record.get(Header.TIME));
				final int roundId = Integer.parseInt(record.get(Header.ROUND));
				final Supplier<Round> roundFactory = () -> new Round(
						new ArrayList<>(DEFAULT_EXPECTED_UNIQUE_ENTITY_COUNT), roundUttListFactory.apply(roundId), score, time);
				final Round round = fetchRound(result, roundId, roundFactory);
				final List<Referent> roundEntities = round.getReferents();

				final int entityId = Integer.parseInt(record.get(Header.ENTITY));
				final Referent entity = fetchReferent(roundEntities, entityId, roundId);
				entity.setShape(record.get(Header.SHAPE));
				entity.setEdgeCount(Integer.parseInt(record.get(Header.EDGE_COUNT)));
				entity.setSize(Float.parseFloat(record.get(Header.SIZE)));
				entity.setRed(Float.parseFloat(record.get(Header.RED)));
				entity.setGreen(Float.parseFloat(record.get(Header.GREEN)));
				entity.setBlue(Float.parseFloat(record.get(Header.BLUE)));
				entity.setHue(Float.parseFloat(record.get(Header.HUE)));
				entity.setPosition(Float.parseFloat(record.get(Header.POSITION_X)),
						Float.parseFloat(record.get(Header.POSITION_Y)));
				entity.setTarget(Boolean.parseBoolean(record.get(Header.REFERENT)));
			}
		}
		return result;
	}

}
