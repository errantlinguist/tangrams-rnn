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

import java.util.List;

import se.kth.speech.coin.tangrams.wac.data.Referent;

final class ClassificationResult {

	/**
	 * An <em>n</em>-best list of possible target referents for a given
	 * {@code Round}.
	 */
	private final List<Referent> ranking;

	/**
	 * The count of words for which no pre-trained classifier could be found and
	 * thus were interpolated.
	 */
	private final int oovObservationCount;

	/**
	 * An array of strings used for choosing word classifiers during
	 * classification.
	 */
	private final String[] words;

	/**
	 *
	 * @param ranking
	 *            An <em>n</em>-best list of possible target referents for a
	 *            given {@code Round}.
	 * @param words
	 *            An array of strings used for choosing word classifiers during
	 *            classification.
	 * @param oovObservationCount
	 *            The count of words for which no pre-trained classifier could
	 *            be found and thus were interpolated.
	 */
	ClassificationResult(final List<Referent> ranking, final String[] words, final int oovObservationCount) {
		this.ranking = ranking;
		this.words = words;
		this.oovObservationCount = oovObservationCount;
	}

	/**
	 * @return The count of words for which no pre-trained classifier could be
	 *         found and thus were interpolated.
	 */
	int getOovObservationCount() {
		return oovObservationCount;
	}

	/**
	 * @return An <em>n</em>-best list of possible target referents for a given
	 *         {@code Round}.
	 */
	List<Referent> getRanking() {
		return ranking;
	}

	/**
	 * @return An array of strings used for choosing word classifiers during
	 *         classification.
	 */
	String[] getWords() {
		return words;
	}
}