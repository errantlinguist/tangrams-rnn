package tangram.data;

import java.util.*;

public class Vocabulary {

	Map<String,Integer> dict = new HashMap<>();
	
	public void add(String word) {
		if (!dict.containsKey(word))
			dict.put(word, 1);
		else
			dict.put(word, dict.get(word) + 1);
	}
	
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		for (String word : getWordsSortedByFreq()) {
			sb.append(word + " " + dict.get(word) + "\n");
		}
		return sb.toString();
	}
	
	public void prune(int n) {
		for (String word : getWords()) {
			if (dict.get(word) < n || word.endsWith("-")) {
				dict.remove(word);
			}
		}
	}

	public int size() {
		return dict.size();
	}

	public List<String> getWordsSortedByFreq() {
		List<String> words = getWords();
		words.sort(new Comparator<String>() {
			@Override
			public int compare(String o1, String o2) {
				return dict.get(o2).compareTo(dict.get(o1));
			}
		});
		return words;
	}
	
	public List<String> getWords() {
		return new ArrayList<>(dict.keySet());
	}

	public List<String> getUpdatedWordsSince(Vocabulary oldVocab) {
		List<String> newWords = new ArrayList<>();
		for (String word : getWords()) {
			//System.out.println(word + " " + oldVocab.dict.get(word) + " " + dict.get(word));
			if (!oldVocab.dict.containsKey(word) || !oldVocab.dict.get(word).equals(dict.get(word)))
				newWords.add(word);
		}
		return newWords;
	}

	public Integer getCount(String word) {
		return dict.get(word);
	}
	
	public boolean has(String word) {
		return dict.containsKey(word);
	}

	public double getCount(String word, int def) {
		Integer count = getCount(word);
		if (count == null)
			return def;
		else
			return count;
	}
	
	
}
