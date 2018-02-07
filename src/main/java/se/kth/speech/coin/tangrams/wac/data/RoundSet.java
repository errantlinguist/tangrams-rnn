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
import java.util.stream.Collectors;
import java.util.stream.Stream;

import it.unimi.dsi.fastutil.objects.Object2LongMap;
import it.unimi.dsi.fastutil.objects.Object2LongOpenHashMap;

public final class RoundSet {

	private final boolean onlyInstructor;

	private final List<Round> rounds;

	public RoundSet(final List<Round> rounds, final boolean onlyInstructor) { // NO_UCD
																				// (use
																				// private)
		this.rounds = rounds;
		this.onlyInstructor = onlyInstructor;
	}

	public RoundSet(final SessionSet set, final boolean onlyInstructor) {
		this(set.getSessions().stream().map(Session::getRounds).flatMap(List::stream).collect(Collectors.toList()),
				onlyInstructor);
	}

	public void addWordCountsForRounds(final Round round, final Object2LongMap<String> counts) {
		final Stream<String> tokens = getReferringTokens(round);
		tokens.forEach(word -> {
			final long oldValue = counts.getLong(word);
			counts.put(word, oldValue + 1L);
		});
	}

	public Vocabulary createVocabulary(final int estimatedVocabSize) {
		final Object2LongOpenHashMap<String> counts = new Object2LongOpenHashMap<>(estimatedVocabSize);
		counts.defaultReturnValue(0L);
		addWordCountsForRounds(counts);
		counts.trim();
		return new Vocabulary(counts);
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
		if (!(obj instanceof RoundSet)) {
			return false;
		}
		final RoundSet other = (RoundSet) obj;
		if (onlyInstructor != other.onlyInstructor) {
			return false;
		}
		if (rounds == null) {
			if (other.rounds != null) {
				return false;
			}
		} else if (!rounds.equals(other.rounds)) {
			return false;
		}
		return true;
	}

	/**
	 * Gets {@link Round} instances which contain words not part of the given
	 * {@link Collection} of words.
	 *
	 * @param vocabWords
	 *            The vocabulary of all words to be used as classifiers.
	 * @return A {@link Stream} of {@code Round} instances, each of which
	 *         featuring at least one referring-language word which is not part
	 *         of the given {@code Collection} of vocabulary words.
	 */
	public Stream<Round> getDiscountRounds(final Collection<? super String> vocabWords) {
		return rounds.stream().filter(round -> round.hasDiscount(vocabWords, onlyInstructor));
	}

	/**
	 * Gets {@link Round} instances which contains at least one observation of a
	 * given word used as referring language.
	 *
	 * @param vocabWord
	 *            The word to search for.
	 * @return A {@link Stream} of {@code Round} instances, each of which
	 *         featuring usage of the given word as referring language.
	 */
	public Stream<Round> getExampleRounds(final String vocabWord) {
		return rounds.stream().filter(round -> round.hasWord(vocabWord, onlyInstructor));
	}

	public Stream<String> getReferringTokens(final Round round) {
		return round.getReferringTokens(onlyInstructor);
	}

	/**
	 * @return the rounds
	 */
	public List<Round> getRounds() {
		return rounds;
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
		result = prime * result + (onlyInstructor ? 1231 : 1237);
		result = prime * result + (rounds == null ? 0 : rounds.hashCode());
		return result;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		final StringBuilder builder = new StringBuilder();
		builder.append("RoundSet [rounds=");
		builder.append(rounds);
		builder.append(", onlyInstructor=");
		builder.append(onlyInstructor);
		builder.append("]");
		return builder.toString();
	}

	private void addWordCountsForRounds(final Object2LongMap<String> counts) {
		rounds.stream().forEach(round -> addWordCountsForRounds(round, counts));
	}
}
