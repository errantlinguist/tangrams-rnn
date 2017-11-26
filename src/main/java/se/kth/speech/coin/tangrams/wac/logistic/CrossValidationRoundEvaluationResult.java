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

final class CrossValidationRoundEvaluationResult {

	private final int crossValidationIteration;

	private final RoundEvaluationResult evalResult;

	CrossValidationRoundEvaluationResult(final int crossValidationIteration, final RoundEvaluationResult evalResult) {
		this.crossValidationIteration = crossValidationIteration;
		this.evalResult = evalResult;

	}

	/**
	 * @return the crossValidationIteration
	 */
	int getCrossValidationIteration() {
		return crossValidationIteration;
	}

	/**
	 * @return the evalResult
	 */
	RoundEvaluationResult getEvalResult() {
		return evalResult;
	}
}