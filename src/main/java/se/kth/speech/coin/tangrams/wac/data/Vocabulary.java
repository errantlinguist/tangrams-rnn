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
			sb.append(word + " " + dict.get(word) + System.lineSeparator());
		}
		return sb.toString();
	}
	
	public void prune(int n) {
		for (String word : getWords()) {
			if (dict.get(word) < n) {
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
