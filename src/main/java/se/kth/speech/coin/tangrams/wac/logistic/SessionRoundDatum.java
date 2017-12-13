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

import se.kth.speech.coin.tangrams.wac.data.Round;

final class SessionRoundDatum {

	private final Round round;

	private final int roundId;

	private final String sessionId;

	SessionRoundDatum(final String sessionId, final int roundId, final Round round) {
		this.sessionId = sessionId;
		this.roundId = roundId;
		this.round = round;
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
		if (!(obj instanceof SessionRoundDatum)) {
			return false;
		}
		final SessionRoundDatum other = (SessionRoundDatum) obj;
		if (round == null) {
			if (other.round != null) {
				return false;
			}
		} else if (!round.equals(other.round)) {
			return false;
		}
		if (roundId != other.roundId) {
			return false;
		}
		if (sessionId == null) {
			if (other.sessionId != null) {
				return false;
			}
		} else if (!sessionId.equals(other.sessionId)) {
			return false;
		}
		return true;
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
		result = prime * result + (round == null ? 0 : round.hashCode());
		result = prime * result + roundId;
		result = prime * result + (sessionId == null ? 0 : sessionId.hashCode());
		return result;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		final StringBuilder builder = new StringBuilder(256);
		builder.append("SessionRoundDatum [round=");
		builder.append(round);
		builder.append(", roundId=");
		builder.append(roundId);
		builder.append(", sessionId=");
		builder.append(sessionId);
		builder.append("]");
		return builder.toString();
	}

	/**
	 * @return the round
	 */
	Round getRound() {
		return round;
	}

	/**
	 * @return the roundId
	 */
	int getRoundId() {
		return roundId;
	}

	/**
	 * @return the sessionId
	 */
	String getSessionId() {
		return sessionId;
	}
}