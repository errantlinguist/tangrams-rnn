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
import java.util.Map;

import se.kth.speech.coin.tangrams.wac.data.Referent;

final class ClassificationResult {

	/**
	 * The words which were encountered during classification for which no
	 * trained model could be found, thus using the discount model for them
	 * instead.
	 */
	private final List<String> oovObservations;

	/**
	 * A list of {@link Weighted} instances representing the confidence score of
	 * each {@link Referent} being the target referent for the given game round.
	 */
	private final List<Weighted<Referent>> scoredReferents;

	/**
	 * A {@link Map} of the different word classifiers used (including the OOV
	 * label if used) mapping to a {@link Map} of classification scores computed
	 * for each {@link Referent}.
	 */
	private final Map<Referent, Map<String, List<Double>>> refWordClassifierScoreLists;

	/**
	 * An array of strings used for choosing word classifiers during
	 * classification.
	 */
	private final String[] words;

	/**
	 *
	 * @param scoredReferents
	 *            A list of {@link Weighted} instances representing the
	 *            confidence score of each {@link Referent} being the target
	 *            referent for the given game round.
	 * @param words
	 *            An array of strings used for choosing word classifiers during
	 *            classification.
	 * @param oovObservations
	 *            The words which were encountered during classification for
	 *            which no trained model could be found, thus using the discount
	 *            model for them instead.
	 * @param wordClassifierScoreLists
	 *            A {@link Map} of the different word classifiers used
	 *            (including the OOV label if used) mapping to a {@link Map} of
	 *            classification scores computed for each {@link Referent}.
	 */
	ClassificationResult(final List<Weighted<Referent>> scoredReferents, final String[] words,
			final List<String> oovObservations,
			final Map<Referent, Map<String, List<Double>>> refWordClassifierScoreLists) {
		this.scoredReferents = scoredReferents;
		this.words = words;
		this.oovObservations = oovObservations;
		this.refWordClassifierScoreLists = refWordClassifierScoreLists;
	}

	/**
	 * @return The words which were encountered during classification for which
	 *         no trained model could be found, thus using the discount model
	 *         for them instead.
	 */
	List<String> getOovObservations() {
		return oovObservations;
	}

	/**
	 * @return A {@link Map} of the different word classifiers used (including
	 *         the OOV label if used) mapping to a {@link Map} of classification
	 *         scores computed for each {@link Referent}.
	 */
	Map<Referent, Map<String, List<Double>>> getRefWordClassifierScoreLists() {
		return refWordClassifierScoreLists;
	}

	/**
	 * @return A list of {@link Weighted} instances representing the confidence
	 *         score of each {@link Referent} being the target referent for the
	 *         given game round.
	 */
	List<Weighted<Referent>> getScoredReferents() {
		return scoredReferents;
	}

	/**
	 * @return An array of strings used for choosing word classifiers during
	 *         classification.
	 */
	String[] getWords() {
		return words;
	}
}