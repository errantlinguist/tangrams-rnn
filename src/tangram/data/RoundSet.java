package tangram.data;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class RoundSet {

	public List<Round> rounds = new ArrayList<>();
	
	public RoundSet(SessionSet set) {
		for (Session sess : set.sessions) {
			this.rounds.addAll(sess.rounds);
		}
	}
	
	public Vocabulary getVocabulary() {
		Vocabulary vocab = new Vocabulary();
		for (Round round : rounds) {
			for (String word : round.getWords()) {
				vocab.add(word);
			}
		}
		return vocab;
	}

	public List<Referent> getExamples(String hasWord) {
		List<Referent> result = new ArrayList<>();
		for (Round round : rounds) {
			if (round.hasWord(hasWord)) {
				//result.add(round.target);
				//result.add(round.getRandomNonTarget());
				result.addAll(round.referents);
			}
		}
		return result;
	}

	public List<Referent> getDiscountExamples(Collection<String> words) {
		List<Referent> result = new ArrayList<>();
		for (Round round : rounds) {
			if (round.hasDiscount(words)) {
				//result.add(round.target);
				//result.add(round.getRandomNonTarget());
				result.addAll(round.referents);
			}
		}
		return result;
	}
}
