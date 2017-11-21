package tangram.data;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Random;

public class Round {

	public int n = -1;
	public List<Utterance> utts = new ArrayList<>();
	public List<Referent> referents = new ArrayList<>();
	public Referent target;
	
	static Random random = new Random(5);
	
	/**
	 * Checks if the round has a specific word
	 */
	public boolean hasWord(String hasWord) {
		for (String word : getWords()) {
			if (word.equals(hasWord)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Checks if the round has a word which is not part of the provided collection of words
	 */
	public boolean hasDiscount(Collection<String> words) {
		for (String word : getWords()) {
			if (!words.contains(word)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Returns a list of words that have been used in this round
	 */
	public List<String> getWords() {
		List<String> list = new ArrayList<>();
		for (Utterance utt : utts) {
			if (Parameters.ONLY_GIVER && !utt.isGiver)
				continue;
			String[] words;
			if (Parameters.ONLY_REFLANG) 
				words = utt.refText;
			else
				words = utt.fullText;
			for (String word : words) {
				list.add(word);
			}
		}
		return list;
	}

	public String prettyDialog() {
		StringBuilder sb = new StringBuilder();
		for (Utterance utt : utts) {
			sb.append(utt.speaker + ": ");
			for (String word : utt.fullText) {
				sb.append(word + " ");
			}
		}
		return sb.toString();
	}

	public Referent getRandomNonTarget() {
		List<Referent> list = new ArrayList<>(referents);
		list.remove(target);
		return list.get(random.nextInt(list.size()));
	}

	public boolean isNegative() {
		for (Utterance utt : utts) {
			if (utt.isGiver && utt.isNegative()) {
				return true;
			}
		}
		return false;
	}
	
}
