package tangram.data;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Stream;

public class Session {
	
	private static void checkWellFormedness(final List<Round> rounds, final Path dir) {
		for (final ListIterator<Round> roundIter = rounds.listIterator(); roundIter.hasNext();) {
			final Round r = roundIter.next();
			if (r.utts.size() < 1) {
				throw new RuntimeException("Round " + r.n + " in session \"" + dir + "\" has no utterances.");
			}
			if (r.referents.size() != 20) {
				throw new RuntimeException("Round " + r.n + " in session \"" + dir + "\" has " + r.referents.size() + " referent(s).");
			}
		}
		if (rounds.size() < 5)
			throw new RuntimeException(String.format("Session \"%s\" has only %d round(s).", rounds.size()));		
	}

	private static int findLastValidRoundIdx(final List<Round> rounds) {
		final ListIterator<Round> roundIter = rounds.listIterator(rounds.size());
		int result = rounds.size();
		while (roundIter.hasPrevious()) {
			final Round round = roundIter.previous();
			if (round.utts.isEmpty()) {
				// Use arithmetic here rather than "ListIterator.previousIndex()" so that the method properly returns -1 if the list is empty
				// Rather than throwing an exception
				result = result - 1;
			} else {
				break;
			}
		}
		return result;
	}
	
	private static void readRoundReferents(final Path infilePath, final Collection<Round> rounds) throws IOException {
		try (final Stream<String> lines = Files.lines(infilePath)){
			final Iterator<String> lineIter = lines.iterator();
			// Skip header
			lineIter.next();
			while (lineIter.hasNext()) {
				final String line = lineIter.next().trim();
				String[] cols = line.split("\t");
				if (cols[4].equals("nextturn.request")) {
					Referent referent = new Referent(cols);
					Round round = rounds.stream().filter(extantRound -> extantRound.n == referent.round).findFirst().orElse(null);
					if (round == null) {
						//System.out.println("Cannot find round " + referent.round);
					} else {
						round.referents.add(referent);
						if (referent.target)
							round.target = referent;
					}
				}
			}
		}
	}
	
	private static void readRoundUtterances(final Path infilePath, final Collection<Round> rounds) throws IOException {
		try (final Stream<String> lines = Files.lines(infilePath)){
			final Iterator<String> lineIter = lines.iterator();
			// Skip header
			lineIter.next();
			// NOTE: A round with the ID "0" is a "pre-game" round, with no valid referents
			Round round = null;
			boolean AisGiver = false;
			while (lineIter.hasNext()) {
				final String line = lineIter.next().trim();
				final Utterance utt = new Utterance(line);
				if (round == null || round.n != utt.round) {
					round = new Round();
					round.n = utt.round;
					rounds.add(round);
					AisGiver = !AisGiver;
				}
				utt.setRole(AisGiver);
				round.utts.add(utt);
			}
		}
	}
	
	private static List<Round> trimRounds(final List<Round> rounds){
		// Check first round if it is a valid round or is a "pre-game" round with no referents
		final int subListStartIdx = rounds.get(0).referents.isEmpty() ? 1 : 0;
		final int subListEndIdx = findLastValidRoundIdx(rounds);
		return subListStartIdx == 0 && subListEndIdx == rounds.size() ? rounds : rounds.subList(subListStartIdx, subListEndIdx);
	}	
	
	public final List<Round> rounds;
	public String name;
	
	public Session(Path dir) throws IOException {
		this.name = dir.getFileName().toString();
		final ArrayList<Round> readRounds = new ArrayList<>(150);
		readRoundUtterances(dir.resolve("extracted-referring-tokens.tsv"), readRounds);
		readRoundReferents(dir.resolve("events.tsv"), readRounds);
		readRounds.trimToSize();
		final List<Round> trimmedRounds = trimRounds(readRounds);
		checkWellFormedness(trimmedRounds, dir);
		this.rounds = trimmedRounds;
	}
	
 	public static void main(String[] args) throws IOException {
		final Path inpath = Paths.get(args[0]);
		System.err.println(String.format("Reading sessions from \"%s\".", inpath));
		new Session(inpath);
	}
	
}
