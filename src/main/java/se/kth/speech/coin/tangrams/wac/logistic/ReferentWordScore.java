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
package se.kth.speech.coin.tangrams.wac.logistic;

import se.kth.speech.coin.tangrams.wac.data.Referent;

public final class ReferentWordScore {

	private final boolean isOov;

	private final Referent ref;

	private final double score;

	private final String word;
	
	private final long wordObsCount;

	ReferentWordScore(final Referent ref, final String word, final boolean isOov, final long wordObsCount, final double score) {
		this.ref = ref;
		this.word = word;
		this.isOov = isOov;
		this.wordObsCount = wordObsCount;
		this.score = score;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (!(obj instanceof ReferentWordScore)) {
			return false;
		}
		ReferentWordScore other = (ReferentWordScore) obj;
		if (isOov != other.isOov) {
			return false;
		}
		if (ref == null) {
			if (other.ref != null) {
				return false;
			}
		} else if (!ref.equals(other.ref)) {
			return false;
		}
		if (Double.doubleToLongBits(score) != Double.doubleToLongBits(other.score)) {
			return false;
		}
		if (word == null) {
			if (other.word != null) {
				return false;
			}
		} else if (!word.equals(other.word)) {
			return false;
		}
		if (wordObsCount != other.wordObsCount) {
			return false;
		}
		return true;
	}

	/**
	 * @return the ref
	 */
	public Referent getRef() {
		return ref;
	}

	/**
	 * @return the score
	 */
	public double getScore() {
		return score;
	}

	/**
	 * @return the word
	 */
	public String getWord() {
		return word;
	}

	/**
	 * @return the wordObsCount
	 */
	public long getWordObsCount() {
		return wordObsCount;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + (isOov ? 1231 : 1237);
		result = prime * result + ((ref == null) ? 0 : ref.hashCode());
		long temp;
		temp = Double.doubleToLongBits(score);
		result = prime * result + (int) (temp ^ (temp >>> 32));
		result = prime * result + ((word == null) ? 0 : word.hashCode());
		result = prime * result + (int) (wordObsCount ^ (wordObsCount >>> 32));
		return result;
	}

	/**
	 * @return the isOov
	 */
	public boolean isOov() {
		return isOov;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder(256);
		builder.append("ReferentWordScore [ref=");
		builder.append(ref);
		builder.append(", word=");
		builder.append(word);
		builder.append(", isOov=");
		builder.append(isOov);
		builder.append(", wordObsCount=");
		builder.append(wordObsCount);
		builder.append(", score=");
		builder.append(score);
		builder.append("]");
		return builder.toString();
	}

}