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

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class Vocabulary {

	private final Map<String, Integer> dict = new HashMap<>();

	public void add(final String word) {
		if (!dict.containsKey(word)) {
			dict.put(word, 1);
		} else {
			dict.put(word, dict.get(word) + 1);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(final Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (!(obj instanceof Vocabulary)) {
			return false;
		}
		final Vocabulary other = (Vocabulary) obj;
		if (dict == null) {
			if (other.dict != null) {
				return false;
			}
		} else if (!dict.equals(other.dict)) {
			return false;
		}
		return true;
	}

	public Integer getCount(final String word) {
		return dict.get(word);
	}

	public double getCount(final String word, final int def) {
		final Integer count = getCount(word);
		if (count == null) {
			return def;
		} else {
			return count;
		}
	}

	public List<String> getUpdatedWordsSince(final Vocabulary oldVocab) {
		final List<String> newWords = new ArrayList<>();
		for (final String word : getWords()) {
			// System.out.println(word + " " + oldVocab.dict.get(word) + " " +
			// dict.get(word));
			if (!oldVocab.dict.containsKey(word) || !oldVocab.dict.get(word).equals(dict.get(word))) {
				newWords.add(word);
			}
		}
		return newWords;
	}

	public List<String> getWords() {
		return new ArrayList<>(dict.keySet());
	}

	public List<String> getWordsSortedByFreq() {
		final List<String> words = getWords();
		words.sort(new Comparator<String>() {
			@Override
			public int compare(final String o1, final String o2) {
				return dict.get(o2).compareTo(dict.get(o1));
			}
		});
		return words;
	}

	public boolean has(final String word) {
		return dict.containsKey(word);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + (dict == null ? 0 : dict.hashCode());
		return result;
	}

	public void prune(final int n) {
		for (final String word : getWords()) {
			if (dict.get(word) < n) {
				dict.remove(word);
			}
		}
	}

	public int size() {
		return dict.size();
	}

	@Override
	public String toString() {
		final StringBuilder sb = new StringBuilder();
		for (final String word : getWordsSortedByFreq()) {
			sb.append(word + " " + dict.get(word) + System.lineSeparator());
		}
		return sb.toString();
	}

}
