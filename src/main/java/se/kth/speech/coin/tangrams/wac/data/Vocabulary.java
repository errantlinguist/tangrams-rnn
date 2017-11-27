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
import java.util.List;
import java.util.Map;

public final class Vocabulary {

	private final Map<String, Integer> wordObservationCounts;

	public Vocabulary(final Map<String, Integer> wordObservationCounts) {
		this.wordObservationCounts = wordObservationCounts;
	}

	public void add(final String word) {
		if (!wordObservationCounts.containsKey(word)) {
			wordObservationCounts.put(word, 1);
		} else {
			wordObservationCounts.put(word, wordObservationCounts.get(word) + 1);
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
		if (wordObservationCounts == null) {
			if (other.wordObservationCounts != null) {
				return false;
			}
		} else if (!wordObservationCounts.equals(other.wordObservationCounts)) {
			return false;
		}
		return true;
	}

	public Integer getCount(final String word) {
		return wordObservationCounts.get(word);
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
			if (!oldVocab.wordObservationCounts.containsKey(word)
					|| !oldVocab.wordObservationCounts.get(word).equals(wordObservationCounts.get(word))) {
				newWords.add(word);
			}
		}
		return newWords;
	}

	public List<String> getWords() {
		return new ArrayList<>(wordObservationCounts.keySet());
	}

	public List<String> getWordsSortedByFreq() {
		final List<String> words = getWords();
		words.sort(new Comparator<String>() {
			@Override
			public int compare(final String o1, final String o2) {
				return wordObservationCounts.get(o2).compareTo(wordObservationCounts.get(o1));
			}
		});
		return words;
	}

	public boolean has(final String word) {
		return wordObservationCounts.containsKey(word);
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
		result = prime * result + (wordObservationCounts == null ? 0 : wordObservationCounts.hashCode());
		return result;
	}

	public void prune(final int n) {
		for (final String word : getWords()) {
			if (wordObservationCounts.get(word) < n) {
				wordObservationCounts.remove(word);
			}
		}
	}

	public int size() {
		return wordObservationCounts.size();
	}

	@Override
	public String toString() {
		final StringBuilder sb = new StringBuilder();
		for (final String word : getWordsSortedByFreq()) {
			sb.append(word + " " + wordObservationCounts.get(word) + System.lineSeparator());
		}
		return sb.toString();
	}

}
