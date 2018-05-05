package se.kth.speech.coin.tangrams.data;

import java.io.*;
import java.nio.file.Files;
import java.util.*;

public class Session {

	public List<Round> rounds = new ArrayList<>();
	public String name;
	
	public Session(File dir) throws IOException {
		this.name = dir.getName();
		int i = 0;
		Round round = null;
		boolean AisGiver = false;
		for (String line : Files.readAllLines(new File(dir, "extracted-referring-tokens.tsv").toPath())) {
			if (i++ == 0) 
				continue;
			line = line.trim();
			Utterance utt = new Utterance(line);
			
			if (round == null || round.n != utt.round) {
				round = new Round();
				round.session = this;
				round.n = utt.round;
				rounds.add(round);
				AisGiver = !AisGiver;
			}
			if (utt.fullText.length < 1)
				throw new RuntimeException("Round " + round.n + " in session " + dir + " has no words");
					
			utt.setRole(AisGiver);
			round.utts.add(utt);
		}
		Map<Integer,Integer> mentioned = new HashMap<>();
		i = 0;
		for (String line : Files.readAllLines(new File(dir, "events.tsv").toPath())) {
			if (i++ == 0) 
				continue;
			line = line.trim();
			String[] cols = line.split("\t");
			if (cols[4].equals("nextturn.request")) {
				
				Referent referent = new Referent(cols);
				referent.mentioned = mentioned.getOrDefault(referent.id, 0);
				round = getRound(referent.round);
				if (round == null) {
					//System.out.println("Cannot find round " + referent.round);
				} else {
					round.referents.add(referent);
					if (referent.target)
						round.target = referent;
				}
				mentioned.put(referent.id, mentioned.getOrDefault(referent.id, 0) + 1);
			}
		}
		// Sanity check session
		for (int rn = 0; rn < rounds.size(); rn++) {
			Round r = rounds.get(rn);
			if (r.utts.size() < 1) {
				throw new RuntimeException("Round " + r.n + " in session " + dir + " has no utterances");
			}
			if (r.referents.size() != 20) {
				//throw new RuntimeException("Round " + r.n + " in session " + dir + " has " + r.referents.size() + " referents");
				while (rounds.size() > rn)
					rounds.remove(rn);
				break;
			}
		}
		if (rounds.size() < 5)
			throw new RuntimeException("Session " + dir + " has few rounds");
	}
	
	public Round getRound(int n) {
		for (Round r : rounds) {
			if (r.n == n)
				return r;
		}
		return null;
	}
	
 	public static void main(String[] args) throws IOException {
		new Session(new File("C:\\data\\Summer recordings\\Game150"));
	}
	
}
