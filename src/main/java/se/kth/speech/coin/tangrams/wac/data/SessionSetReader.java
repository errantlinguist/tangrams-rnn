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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class SessionSetReader {

	private final static Logger LOGGER = LoggerFactory.getLogger(SessionSetReader.class);

	private static final List<Utterance> NULL_ROUND_UTT_LIST = Collections.emptyList();

	private static void checkWellFormedness(final List<Round> rounds, final Path dir) {
		for (final ListIterator<Round> roundIter = rounds.listIterator(); roundIter.hasNext();) {
			final Round r = roundIter.next();
			if (r.getUtts().size() < 1) {
				throw new RuntimeException("Round " + r.roundId + " in session \"" + dir + "\" has no utterances.");
			}
			if (r.getReferents().size() != 20) {
				throw new RuntimeException("Round " + r.roundId + " in session \"" + dir + "\" has "
						+ r.getReferents().size() + " referent(s).");
			}
		}
		if (rounds.size() < 5) {
			throw new RuntimeException(String.format("Session \"%s\" has only %d round(s).", rounds.size()));
		}
	}

	private static List<Round> createDialogueRoundList(final Path subdir, final Path eventsFile, final Path uttsFile,
			final RoundTabularDataReader roundReader, final UtteranceTabularDataReader uttReader) throws IOException {
		LOGGER.info("Reading session at \"{}\".", subdir);
		LOGGER.debug("Reading utterances file at \"{}\".", uttsFile);
		final List<ArrayList<Utterance>> roundUtts = uttReader.apply(uttsFile);
		LOGGER.debug("Parsed utterances for rounds up to ID {}.", roundUtts.size());
		LOGGER.debug("Reading events file at \"{}\".", eventsFile);
		final List<Round> rounds = roundReader.apply(eventsFile, roundId -> fetchRoundUtts(roundUtts, roundId));
		LOGGER.debug("Parsed rounds up to ID {}.", rounds.size());
		final List<Round> result = trimRounds(rounds);
		checkWellFormedness(result, subdir);
		LOGGER.debug("Read {} game round(s) with utterances.", result.size());
		return result;
	}

	private static List<Utterance> fetchRoundUtts(final List<? extends List<Utterance>> roundUtts, final int roundId) {
		List<Utterance> utts = null;
		LOGGER.debug("Fetching utterances for round {}.", roundId);
		try {
			utts = roundUtts.get(roundId);
		} catch (final IndexOutOfBoundsException e) {
			LOGGER.debug("List is too short; The remaining round(s) has/have no utterances.", e);
		}
		return utts == null ? NULL_ROUND_UTT_LIST : utts;
	}

	private static int findLastValidRoundIdx(final List<Round> rounds) {
		final ListIterator<Round> roundIter = rounds.listIterator(rounds.size());
		int result = rounds.size();
		while (roundIter.hasPrevious()) {
			final Round round = roundIter.previous();
			if (round.getUtts().isEmpty()) {
				// Use arithmetic here rather than
				// "ListIterator.previousIndex()" so that the method properly
				// returns -1 if the list is empty
				// Rather than throwing an exception
				result = result - 1;
			} else {
				break;
			}
		}
		return result;
	}

	private static List<Round> trimRounds(final List<Round> rounds) {
		// Check first round if it is a valid round or is a "pre-game" round
		// with no referents
		final int subListStartIdx = rounds.get(0) == null ? 1 : 0;
		final int subListEndIdx = findLastValidRoundIdx(rounds);
		final List<Round> result = subListStartIdx == 0 && subListEndIdx == rounds.size() ? rounds
				: rounds.subList(subListStartIdx, subListEndIdx);
		assert result.stream().noneMatch(Objects::isNull);
		return result;
	}

	private final RoundTabularDataReader roundReader;

	private final UtteranceTabularDataReader uttReader;
	
	public SessionSetReader() {
		this(new RoundTabularDataReader(), new UtteranceTabularDataReader());
	}

	public SessionSetReader(final RoundTabularDataReader roundReader, final UtteranceTabularDataReader uttReader) {
		this.roundReader = roundReader;
		this.uttReader = uttReader;
	}

	public SessionSet apply(final Path dir) throws IOException {
		final List<Session> sessions = new ArrayList<>();
		for (final Iterator<Path> subdirIter = Files.list(dir).filter(Files::isDirectory).iterator(); subdirIter
				.hasNext();) {
			final Path subdir = subdirIter.next();
			final Path eventsFile = subdir.resolve("events.tsv");
			final Path uttsFile = subdir.resolve("extracted-referring-tokens.tsv");
			if (Files.isRegularFile(eventsFile) && Files.isRegularFile(uttsFile)) {
				final List<Round> rounds = createDialogueRoundList(subdir, eventsFile, uttsFile, roundReader,
						uttReader);
				final Session session = new Session(subdir.getFileName().toString(), rounds);
				sessions.add(session);
			}
		}
		return new SessionSet(sessions);
	}

}
