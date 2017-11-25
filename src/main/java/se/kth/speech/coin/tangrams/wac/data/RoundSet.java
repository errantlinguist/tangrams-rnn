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

public final class RoundSet {

	private final List<Round> rounds;

	public RoundSet(final List<Round> rounds) {
		this.rounds = rounds;
	}

	public RoundSet(final SessionSet set) {
		this(set.getSessions().stream().map(Session::getRounds).flatMap(List::stream).collect(Collectors.toList()));
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
		if (rounds == null) {
			if (other.rounds != null) {
				return false;
			}
		} else if (!rounds.equals(other.rounds)) {
			return false;
		}
		return true;
	}

	public Stream<Round> getDiscountRounds(final Collection<? super String> words) {
		return rounds.stream().filter(round -> round.hasDiscount(words));
	}

	public Stream<Round> getExampleRounds(final String hasWord) {
		return rounds.stream().filter(round -> round.hasWord(hasWord));
	}

	/**
	 * @return the rounds
	 */
	public List<Round> getRounds() {
		return rounds;
	}

	public Vocabulary getVocabulary() {
		final Vocabulary vocab = new Vocabulary();
		for (final Round round : rounds) {
			for (final String word : round.getWords()) {
				vocab.add(word);
			}
		}
		return vocab;
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
		final StringBuilder builder = new StringBuilder(64 * (rounds.size() + 1));
		builder.append("RoundSet [rounds=");
		builder.append(rounds);
		builder.append("]");
		return builder.toString();
	}
}
