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

import it.unimi.dsi.fastutil.objects.Object2LongMap;

public final class Vocabulary {

	private final Object2LongMap<String> wordObservationCounts;

	Vocabulary(final Object2LongMap<String> wordObservationCounts) {
		this.wordObservationCounts = wordObservationCounts;
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

	public long getCount(final String word) {
		return wordObservationCounts.getLong(word);
	}

	public List<String> getUpdatedWordsSince(final Vocabulary oldVocab) {
		final List<String> newWords = new ArrayList<>();
		for (final String word : getWords()) {
			// System.out.println(word + " " + oldVocab.dict.get(word) + " " +
			// dict.get(word));
			if (!oldVocab.wordObservationCounts.containsKey(word)
					|| !(oldVocab.wordObservationCounts.getLong(word) == wordObservationCounts.getLong(word))) {
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
				return Long.compare(wordObservationCounts.getLong(o2), wordObservationCounts.getLong(o1));
			}
		});
		return words;
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

	/**
	 *
	 * @param n
	 *            The minimum observation count for a word to not be discounted.
	 * @return The number of word observations discounted for use in smoothing.
	 */
	public long prune(final int n) {
		long result = 0;
		for (final String word : getWords()) {
			if (wordObservationCounts.getLong(word) < n) {
				final long obsCount = wordObservationCounts.removeLong(word);
				result += obsCount;
			}
		}
		return result;
	}

	@Override
	public String toString() {
		final StringBuilder sb = new StringBuilder();
		for (final String word : getWordsSortedByFreq()) {
			sb.append(word + " " + wordObservationCounts.getLong(word) + System.lineSeparator());
		}
		return sb.toString();
	}

}
