/*******************************************************************************
 * Copyright 2017 Todd Shore
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License.  You may obtain a copy
 * of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations under
 * the License.
 ******************************************************************************/
package se.kth.speech.coin.tangrams.wac.data;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

public final class RoundSet {

	private final List<Round> rounds;

	public RoundSet(final List<Round> rounds) {
		this.rounds = rounds;
	}

	public RoundSet(final SessionSet set) {
		this(Arrays.asList(
				set.getSessions().stream().map(Session::getRounds).flatMap(List::stream).toArray(Round[]::new)));
	}

	public List<Referent> getDiscountExamples(final Collection<String> words) {
		final List<Referent> result = new ArrayList<>();
		for (final Round round : rounds) {
			if (round.hasDiscount(words)) {
				// result.add(round.target);
				// result.add(round.getRandomNonTarget());
				result.addAll(round.getReferents());
			}
		}
		return result;
	}

	public List<Referent> getExamples(final String hasWord) {
		final List<Referent> result = new ArrayList<>();
		for (final Round round : rounds) {
			if (round.hasWord(hasWord)) {
				// result.add(round.target);
				// result.add(round.getRandomNonTarget());
				result.addAll(round.getReferents());
			}
		}
		return result;
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
