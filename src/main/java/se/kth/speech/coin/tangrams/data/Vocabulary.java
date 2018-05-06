package se.kth.speech.coin.tangrams.data;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.util.*;

public class Vocabulary {

	public Map<String,Integer> dict = new HashMap<>();

	public Vocabulary() {
	}
	
	public Vocabulary(File file) throws IOException {
		for (String line : Files.readAllLines(file.toPath())) {
			line = line.trim();
			if (line.length() > 0) {
				String[] cols = line.split("\t");
				dict.put(cols[0], Integer.parseInt(cols[1]));
			}
		}
	}
	
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
			sb.append(word).append(" ").append(dict.get(word)).append("\n");
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

	public List<String> getUpdatedWordsSince(Vocabulary oldVocab, int threshold) {
		List<String> newWords = new ArrayList<>();
		for (String word : getWords()) {
			//System.out.println(word + " " + oldVocab.dict.get(word) + " " + dict.get(word));
			if (!oldVocab.dict.containsKey(word) || (oldVocab.dict.get(word) <= threshold && !oldVocab.dict.get(word).equals(dict.get(word))))
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

	public int getCount(String word, int def) {
		Integer count = getCount(word);
		if (count == null)
			return def;
		else
			return count;
	}

	public int getSize() {
		return dict.size();
	}

	public void save(File file) throws FileNotFoundException {
		PrintWriter pw = new PrintWriter(file);
		for (String word : getWordsSortedByFreq()) {
			pw.println(word + "\t" + dict.get(word));
		}
		pw.close();
	}
	
	
}
