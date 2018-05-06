package se.kth.speech.coin.tangrams.rnn;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.util.*;
import se.kth.speech.coin.tangrams.data.*;

public class WordEncoder {

	private HashMap<String,Integer> indices = new HashMap<>((int) Math.ceil(DatasetConstants.EXPECTED_UNIQUE_WORD_COUNT / 0.75f));
	private HashMap<Integer,String> words = new HashMap<>((int) Math.ceil(DatasetConstants.EXPECTED_UNIQUE_WORD_COUNT / 0.75f));
	private int size;
	
	// whether there should be an oov token (represented by 0)
	private boolean oov;

	public WordEncoder(Vocabulary vocab, boolean oov) {
		this.oov = oov;
		int i = oov ? 1 : 0;
		for (String word : vocab.getWordsSortedByFreq()) {
			words.put(i, word);
			indices.put(word, i);
			i++;
		}
		this.size = i;
	}
	
	public WordEncoder(List<String> words, boolean oov) {
		this.oov = oov;
		int i = oov ? 1 : 0;
		for (String word : words) {
			indices.put(word, i++);
		}
		this.size = i;
	}
	
	public WordEncoder(File file) throws IOException {
		this.oov = true;
		for (String line : Files.readAllLines(file.toPath())) {
			line = line.trim();
			if (line.length() > 0) {
				String[] cols = line.split("\t");
				String word = cols[0];
				int index = Integer.parseInt(cols[1]);
				if (index == 0)
					oov = false;
				indices.put(word, index);
				words.put(index, word);
			}
		}
		this.size = indices.size() + (oov ? 1 : 0);
	}
	
	public String getEncoding(String word) {
		StringBuilder sb = new StringBuilder();
		if (!indices.containsKey(word) & !oov)
			throw new RuntimeException("Word '" + word + "' not found");
		int ind = indices.getOrDefault(word, 0);
		for (int i = 0; i < size; i++) {
			if (i > 0)
				sb.append(";");
			if (i == ind) 
				sb.append("1");
			else
				sb.append("0");
		}
		return sb.toString();
	} 
	
	public double[] getArrayEncoding(String word) {
		double[] arr = new double[size];
		Arrays.fill(arr, 0d);
		arr[indices.get(word)] = 1d;
		return arr;
	} 

	public void save(File file) throws FileNotFoundException {
		PrintWriter pw = new PrintWriter(file);
		for (String word : indices.keySet()) {
			pw.println(word + "\t" + indices.get(word));
		}
		pw.close();
	}

	public int size() {
		return size;
	}
	
	public String getWord(int index) {
		return words.get(index);
	}

	public int getIndex(String word) {
		return indices.getOrDefault(word, 0);
	}
	
	
}
