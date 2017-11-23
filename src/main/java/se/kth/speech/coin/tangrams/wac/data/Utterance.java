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

import java.util.Comparator;
import java.util.List;

public final class Utterance implements Comparable<Utterance> {

	private static final Comparator<Utterance> NATURAL_COMPARATOR = Comparator.comparingDouble(Utterance::getStartTime)
			.thenComparingDouble(Utterance::getEndTime);

	private final List<String> tokens;

	private final List<String> referringTokens;

	private final String speakerId;

	private final boolean isInstructor;

	private float startTime;

	private float endTime;

	public Utterance(final float startTime, final float endTime, final String speakerId, final boolean isInstructor,
			final List<String> tokens, final List<String> referringTokens) {
		this.speakerId = speakerId;
		this.isInstructor = isInstructor;
		this.startTime = startTime;
		this.endTime = endTime;
		this.tokens = tokens;
		this.referringTokens = referringTokens;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Comparable#compareTo(java.lang.Object)
	 */
	@Override
	public int compareTo(final Utterance o) {
		// TODO Auto-generated method stub
		return NATURAL_COMPARATOR.compare(this, o);
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
		if (!(obj instanceof Utterance)) {
			return false;
		}
		final Utterance other = (Utterance) obj;
		if (Float.floatToIntBits(endTime) != Float.floatToIntBits(other.endTime)) {
			return false;
		}
		if (isInstructor != other.isInstructor) {
			return false;
		}
		if (speakerId == null) {
			if (other.speakerId != null) {
				return false;
			}
		} else if (!speakerId.equals(other.speakerId)) {
			return false;
		}
		if (referringTokens == null) {
			if (other.referringTokens != null) {
				return false;
			}
		} else if (!referringTokens.equals(other.referringTokens)) {
			return false;
		}
		if (Float.floatToIntBits(startTime) != Float.floatToIntBits(other.startTime)) {
			return false;
		}
		if (tokens == null) {
			if (other.tokens != null) {
				return false;
			}
		} else if (!tokens.equals(other.tokens)) {
			return false;
		}
		return true;
	}

	/**
	 * @return the endTime
	 */
	public float getEndTime() {
		return endTime;
	}

	/**
	 * @return the referringTokens
	 */
	public List<String> getReferringTokens() {
		return referringTokens;
	}

	/**
	 * @return the speakerId
	 */
	public String getSpeakerId() {
		return speakerId;
	}

	/**
	 * @return the startTime
	 */
	public float getStartTime() {
		return startTime;
	}

	/**
	 * @return the tokens
	 */
	public List<String> getTokens() {
		return tokens;
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
		result = prime * result + Float.floatToIntBits(endTime);
		result = prime * result + (isInstructor ? 1231 : 1237);
		result = prime * result + (speakerId == null ? 0 : speakerId.hashCode());
		result = prime * result + (referringTokens == null ? 0 : referringTokens.hashCode());
		result = prime * result + Float.floatToIntBits(startTime);
		result = prime * result + (tokens == null ? 0 : tokens.hashCode());
		return result;
	}

	/**
	 * @return the isInstructor
	 */
	public boolean isInstructor() {
		return isInstructor;
	}

	public boolean isNegative() {
		for (final String word : tokens) {
			if (word.equals("no") || word.equals("nope")) {
				return true;
			}
		}
		return false;
	}

	/**
	 * @param endTime
	 *            the endTime to set
	 */
	public void setEndTime(final float endTime) {
		this.endTime = endTime;
	}

	/**
	 * @param startTime
	 *            the startTime to set
	 */
	public void setStartTime(final float startTime) {
		this.startTime = startTime;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		final StringBuilder builder = new StringBuilder(256);
		builder.append("Utterance [startTime=");
		builder.append(startTime);
		builder.append(", endTime=");
		builder.append(endTime);
		builder.append(", speakerId=");
		builder.append(speakerId);
		builder.append(", isInstructor=");
		builder.append(isInstructor);
		builder.append(", tokens=");
		builder.append(tokens);
		builder.append(", referringTokens=");
		builder.append(referringTokens);
		builder.append("]");
		return builder.toString();
	}

}
