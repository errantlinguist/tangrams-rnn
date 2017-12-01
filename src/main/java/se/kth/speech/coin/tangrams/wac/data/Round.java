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

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Stream;

import se.kth.speech.coin.tangrams.wac.logistic.ModelParameter;

public final class Round {

	private final List<Referent> referents;

	private final int score;

	private final float time;

	private final List<Utterance> utts;

	public Round(final List<Referent> referents, final List<Utterance> utts, final int score, final float time) { // NO_UCD
																													// (use
																													// default)
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
	 * Returns a list of all referring-language tokens that have been used in
	 * this round.
	 *
	 * @param onlyInstructor
	 *            A flag that, when set, ensures that only instructor utterances
	 *            are returned.
	 * @return A {@link Stream} of tokens considered to be referring language
	 *         for training and classification purposes.
	 */
	public Stream<String> getReferringTokens(final boolean onlyInstructor) {
		final Predicate<Utterance> uttFilter = onlyInstructor ? Utterance::isInstructor : utt -> true;
		final Stream<Utterance> relevantUtts = utts.stream().filter(uttFilter);
		return relevantUtts.map(Utterance::getReferringTokens).flatMap(List::stream);
	}

	/**
	 * Returns a list of all referring-language tokens that have been used in
	 * this round.
	 *
	 * @param modelParams
	 *            A {@link Map} of {@link ModelParameter} values.
	 * @return A {@link Stream} of tokens considered to be referring language
	 *         for training and classification purposes.
	 */
	public Stream<String> getReferringTokens(final Map<ModelParameter, Object> modelParams) {
		// NOTE: Values are retrieved directly from the map instead of
		// e.g. assigning them to a final field because it's possible that the
		// map
		// values change at another place in the code and performance isn't an
		// issue here anyway
		return getReferringTokens((Boolean) modelParams.get(ModelParameter.ONLY_INSTRUCTOR));
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
	 * Checks if the round has a word which is not part of the provided
	 * {@link Collection} of words.
	 *
	 * @param vocabWords
	 *            The vocabulary of all words to be used as classifiers.
	 * @param modelParams
	 *            A {@link Map} of {@link ModelParameter} values.
	 * @return <code>true</code> iff the set of all referring language for the
	 *         given round contain words which are not in the given vocabulary.
	 */
	public boolean hasDiscount(final Collection<? super String> vocabWords,
			final Map<ModelParameter, Object> modelParams) {
		return getReferringTokens(modelParams).anyMatch(word -> !vocabWords.contains(word));
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
	 * Checks if the round has a specific word.
	 *
	 * @param word
	 *            The word to check.
	 * @param modelParams
	 *            A {@link Map} of {@link ModelParameter} values.
	 * @return <code>true</code> Iff the set of referring language for the round
	 *         contains the given word.
	 *
	 */
	public boolean hasWord(final String word, final Map<ModelParameter, Object> modelParams) {
		return getReferringTokens(modelParams).anyMatch(word::equals);
	}

	public boolean isNegative() {
		for (final Utterance utt : utts) {
			if (utt.isInstructor() && utt.isNegative()) {
				return true;
			}
		}
		return false;
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
