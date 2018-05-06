package se.kth.speech.coin.tangrams.data;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.*;
import java.util.function.Function;

public class SessionReader {

	private static final Logger LOGGER = LoggerFactory.getLogger(SessionReader.class);

	private final Function<? super String, String[]> tokenSeqFactory;

	private final Function<? super String[], String[]> referringTokenSeqFactory;

	public SessionReader(final Function<? super String[], String[]> referringTokenSeqFactory) {
		this(new TokenSequenceSingletonFactory(), referringTokenSeqFactory);
	}

	public SessionReader(Function<? super String, String[]> tokenSeqFactory, final Function<? super String[], String[]> referringTokenSeqFactory) {
		this.tokenSeqFactory = tokenSeqFactory;
		this.referringTokenSeqFactory = referringTokenSeqFactory;
	}

	public Session apply(File dir) throws IOException {
		final Session result = new Session(dir.getName());
		int i = 0;
		Round round = null;
		boolean AisGiver = false;
		for (String line : Files.readAllLines(new File(dir, "utts.tsv").toPath())) {
			if (i++ == 0)
				continue;
			line = line.trim();
			Utterance utt = parseUtterance(line);

			if (round == null || round.n != utt.getRound()) {
				if (round != null) {
					LOGGER.debug("Read {} utts for round {}.", round.utts.size(), round.n);
				}
				round = new Round();
				round.session = result;
				round.n = utt.getRound();
				result.rounds.add(round);
				AisGiver = !AisGiver;
			}
			if (utt.fullText.length < 1)
				throw new RuntimeException("Round " + round.n + " in session " + dir + " has no words");

			round.utts.add(utt);
		}
		LOGGER.debug("Read {} rounds (possibly also including pre-game round).", result.rounds.size());
		assert (result.rounds.stream().noneMatch(r -> r.utts.isEmpty()));
		{
			// Remove any pre-game rounds
			Iterator<Round> rIter = result.rounds.iterator();
			while (rIter.hasNext()) {
				final Round r = rIter.next();
				// All round utterances should have the same round ID
				assert r.utts.stream().mapToInt(Utterance::getRound).distinct().count() == 1L;
				if (r.utts.get(0).getRound() < 1){
					rIter.remove();
				}
			}
		}

		Map<Integer, Integer> mentioned = new HashMap<>();
		i = 0;
		for (String line : Files.readAllLines(new File(dir, "events.tsv").toPath())) {
			if (i++ == 0)
				continue;
			line = line.trim();
			String[] cols = line.split("\t");
			if (cols[4].equals("nextturn.request")) {

				Referent referent = new Referent(cols);
				referent.mentioned = mentioned.getOrDefault(referent.id, 0);
				round = result.getRound(referent.round);
				if (round == null) {
					//System.out.println("Cannot find round " + referent.round);
				} else {
					round.referents.add(referent);
					if (referent.isTarget())
						round.target = referent;
				}
				mentioned.put(referent.id, mentioned.getOrDefault(referent.id, 0) + 1);
			}
		}
		// Sanity check session
		final List<Round> rounds = result.rounds;
		for (int rn = 0; rn < rounds.size(); rn++) {
			Round r = rounds.get(rn);
			if (r.utts.size() < 1) {
				throw new RuntimeException("Round " + r.n + " in session " + dir + " has no utterances");
			}
			if (r.referents.size() != 20) {
//				throw new RuntimeException("Round " + r.n + " in session " + dir + " has " + r.referents.size() + " referents");
				while (rounds.size() > rn)
					rounds.remove(rn);
				break;
			}
		}
		if (rounds.size() < 5)
			throw new RuntimeException("Session " + dir + " has few rounds: " + rounds.size());

		return result;
	}

	private Utterance parseUtterance(final String line) {
		final String[] cols = line.split("\t");
		final int round = Integer.parseInt(cols[0]);
		final String speaker = cols[1];
		final boolean isGiver = (cols[2].equalsIgnoreCase("INSTRUCTOR"));
		final float startTime = Float.parseFloat(cols[3]);
		final float endTime = Float.parseFloat(cols[4]);
		final String[] fullText = tokenSeqFactory.apply(cols[5]);
		final String[] refText = referringTokenSeqFactory.apply(fullText);
		return new Utterance(round, startTime, endTime, speaker, isGiver, fullText, refText);
	}

}
