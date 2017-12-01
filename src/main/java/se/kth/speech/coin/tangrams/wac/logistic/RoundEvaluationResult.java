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

public final class RoundEvaluationResult { // NO_UCD (use default)

	private final String sessionId;

	private final int roundId;

	private final ClassificationResult classificationResult;

	private final Round round;

	private final long startNanos;

	private final long endNanos;

	RoundEvaluationResult(final long startNanos, final long endNanos, final String sessionId, final int roundId,
			final Round round, final ClassificationResult classificationResult) {
		this.startNanos = startNanos;
		this.endNanos = endNanos;
		this.sessionId = sessionId;
		this.roundId = roundId;
		this.round = round;
		this.classificationResult = classificationResult;
	}

	/**
	 * @return the classificationResult
	 */
	public ClassificationResult getClassificationResult() {
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

}