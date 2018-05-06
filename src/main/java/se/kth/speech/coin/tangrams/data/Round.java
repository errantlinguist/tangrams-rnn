package se.kth.speech.coin.tangrams.data;

import java.util.*;


public class Round {

	public Session session;
	public int n = -2;
	public List<Utterance> utts = new ArrayList<>();
	public List<Referent> referents = new ArrayList<>();
	public Referent target;
	public double weight = 1d;
	
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
	
	public boolean hasBigram(String hasWord) {
		String[] bigram = hasWord.split(" ");
		for (Utterance utt : utts) {
			for (int i = 0; i < utt.fullText.length-1; i++) {
				if (utt.fullText[i].equalsIgnoreCase(bigram[0]) && utt.fullText[i+1].equalsIgnoreCase(bigram[1]))
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
			Collections.addAll(list, words);
		}
		return list;
	}

	public Collection<String> getUniqueWords() {
		return new HashSet<>(getWords());
	}

	public String prettyDialog(boolean newline) {
		StringBuilder sb = new StringBuilder();
		for (Utterance utt : utts) {
			sb.append(utt.prettyString());
			if (newline)
				sb.append("\n");
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

	public int getWordCount() {
		int i = 0;
		for (Utterance utt : utts) 
			i += utt.fullText.length;
		return i;
	}

	public Referent[] getRandomNonTargets() {
		List<Referent> list = new ArrayList<>(referents);
		list.remove(target);
		list.sort(new Comparator<Referent>() {
			@Override
			public int compare(Referent o1, Referent o2) {
				return (random.nextDouble() < 0.5) ? -1 : 1; 
			}
		});
		return list.toArray(new Referent[0]);
	}

	public int getNormalizedTextWordCount() {
		int wordCount = 0;
		for (Utterance utt : utts) {
			wordCount += utt.getNormalizedWords().size();
		}
		return wordCount;
	}

	
}
