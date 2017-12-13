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

public final class RoundEvaluationResult<C> { // NO_UCD (use default)

	private final C classificationResult;

	private final long endNanos;

	private final Round round;

	private final int roundId;

	private final String sessionId;

	private final long startNanos;

	RoundEvaluationResult(final long startNanos, final long endNanos, final String sessionId, final int roundId,
			final Round round, final C classificationResult) {
		this.startNanos = startNanos;
		this.endNanos = endNanos;
		this.sessionId = sessionId;
		this.roundId = roundId;
		this.round = round;
		this.classificationResult = classificationResult;
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
		if (!(obj instanceof RoundEvaluationResult)) {
			return false;
		}
		final RoundEvaluationResult<?> other = (RoundEvaluationResult<?>) obj;
		if (classificationResult == null) {
			if (other.classificationResult != null) {
				return false;
			}
		} else if (!classificationResult.equals(other.classificationResult)) {
			return false;
		}
		if (endNanos != other.endNanos) {
			return false;
		}
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
		if (startNanos != other.startNanos) {
			return false;
		}
		return true;
	}

	/**
	 * @return the classificationResult
	 */
	public C getClassificationResult() {
		return classificationResult;
	}

	/**
	 * @return the endNanos
	 */
	public long getEndNanos() {
		return endNanos;
	}

	/**
	 * @return the round
	 */
	public Round getRound() {
		return round;
	}

	/**
	 * @return the roundId
	 */
	public int getRoundId() {
		return roundId;
	}

	/**
	 * @return the sessionId
	 */
	public String getSessionId() {
		return sessionId;
	}

	/**
	 * @return the startNanos
	 */
	public long getStartNanos() {
		return startNanos;
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
		result = prime * result + (classificationResult == null ? 0 : classificationResult.hashCode());
		result = prime * result + (int) (endNanos ^ endNanos >>> 32);
		result = prime * result + (round == null ? 0 : round.hashCode());
		result = prime * result + roundId;
		result = prime * result + (sessionId == null ? 0 : sessionId.hashCode());
		result = prime * result + (int) (startNanos ^ startNanos >>> 32);
		return result;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		final StringBuilder builder = new StringBuilder(512);
		builder.append("RoundEvaluationResult [sessionId=");
		builder.append(sessionId);
		builder.append(", roundId=");
		builder.append(roundId);
		builder.append(", classificationResult=");
		builder.append(classificationResult);
		builder.append(", round=");
		builder.append(round);
		builder.append(", startNanos=");
		builder.append(startNanos);
		builder.append(", endNanos=");
		builder.append(endNanos);
		builder.append("]");
		return builder.toString();
	}

}