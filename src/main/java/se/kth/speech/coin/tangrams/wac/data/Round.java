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
import java.util.Collection;
import java.util.List;

public final class Round {

	private final List<Utterance> utts;

	private final List<Referent> referents;

	private final int score;

	private final float time;

	public Round(final List<Referent> referents, final List<Utterance> utts, final int score, final float time) {
		this.referents = referents;
		this.utts = utts;
		this.score = score;
		this.time = time;
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
		if (!(obj instanceof Round)) {
			return false;
		}
		final Round other = (Round) obj;
		if (referents == null) {
			if (other.referents != null) {
				return false;
			}
		} else if (!referents.equals(other.referents)) {
			return false;
		}
		if (score != other.score) {
			return false;
		}
		if (Float.floatToIntBits(time) != Float.floatToIntBits(other.time)) {
			return false;
		}
		if (utts == null) {
			if (other.utts != null) {
				return false;
			}
		} else if (!utts.equals(other.utts)) {
			return false;
		}
		return true;
	}

	/**
	 * @return the referents
	 */
	public List<Referent> getReferents() {
		return referents;
	}

	/**
	 * @return the score
	 */
	public int getScore() {
		return score;
	}

	/**
	 * @return the time
	 */
	public float getTime() {
		return time;
	}

	/**
	 * @return the utts
	 */
	public List<Utterance> getUtts() {
		return utts;
	}

	/**
	 * Returns a list of words that have been used in this round
	 */
	public List<String> getWords() {
		final List<String> list = new ArrayList<>();
		for (final Utterance utt : utts) {
			if (Parameters.ONLY_GIVER && !utt.isInstructor()) {
				continue;
			}
			final List<String> words = Parameters.ONLY_REFLANG ? utt.getReferringTokens() : utt.getTokens();
			for (final String word : words) {
				list.add(word);
			}
		}
		return list;
	}

	/**
	 * Checks if the round has a word which is not part of the provided
	 * collection of words
	 */
	public boolean hasDiscount(final Collection<? super String> words) {
		for (final String word : getWords()) {
			if (!words.contains(word)) {
				return true;
			}
		}
		return false;
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
		result = prime * result + (referents == null ? 0 : referents.hashCode());
		result = prime * result + score;
		result = prime * result + Float.floatToIntBits(time);
		result = prime * result + (utts == null ? 0 : utts.hashCode());
		return result;
	}

	/**
	 * Checks if the round has a specific word
	 */
	public boolean hasWord(final String hasWord) {
		for (final String word : getWords()) {
			if (word.equals(hasWord)) {
				return true;
			}
		}
		return false;
	}

	public boolean isNegative() {
		for (final Utterance utt : utts) {
			if (utt.isInstructor() && utt.isNegative()) {
				return true;
			}
		}
		return false;
	}

	public String prettyDialog() {
		final StringBuilder sb = new StringBuilder();
		for (final Utterance utt : utts) {
			sb.append(utt.getSpeakerId() + ": ");
			for (final String word : utt.getTokens()) {
				sb.append(word + " ");
			}
		}
		return sb.toString();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		final StringBuilder builder = new StringBuilder(256);
		builder.append("Round [utts=");
		builder.append(utts);
		builder.append(", referents=");
		builder.append(referents);
		builder.append(", score=");
		builder.append(score);
		builder.append(", time=");
		builder.append(time);
		builder.append("]");
		return builder.toString();
	}

}
