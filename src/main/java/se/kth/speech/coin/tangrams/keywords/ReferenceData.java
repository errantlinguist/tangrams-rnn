/*
 * 	Copyright 2018 Todd Shore
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
package se.kth.speech.coin.tangrams.keywords;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import se.kth.speech.coin.tangrams.wac.data.Referent;
import se.kth.speech.coin.tangrams.wac.data.Round;

final class ReferenceData {

	private static final boolean INITIAL_IS_NEW_REF = true;

	private static void checkOnlyOneTargetRefPerRound(final Collection<Round> rounds)
			throws UnsupportedOperationException {
		final Set<Long> roundTargetRefCounts = getRoundTargetRefs(rounds.stream()).mapToLong(Stream::count).boxed()
				.collect(Collectors.toCollection(() -> new HashSet<>(2)));
		switch (roundTargetRefCounts.size()) {
		case 0: {
			throw new UnsupportedOperationException("Round list is empty.");
		}
		case 1: {
			final long roundTargetRefCount = roundTargetRefCounts.iterator().next();
			if (roundTargetRefCount != 1L) {
				throw new UnsupportedOperationException(String.format(
						"Cannot calculate maximum possible reference count for rounds with target ref counts other than 1; Target ref count: %d",
						roundTargetRefCount));
			}
			break;
		}
		default: {
			throw new UnsupportedOperationException(String.format(
					"Cannot calculate maximum possible reference count for rounds with different numbers of target references; Unique target ref counts: %s",
					roundTargetRefCounts));
		}
		}
	}

	private static int getRefCountPerRound(final Collection<Round> rounds) throws UnsupportedOperationException {
		checkOnlyOneTargetRefPerRound(rounds);
		final int result;

		final Set<Integer> roundRefCounts = rounds.stream().map(Round::getReferents).mapToInt(List::size).boxed()
				.collect(Collectors.toCollection(() -> new HashSet<>(2)));
		switch (roundRefCounts.size()) {
		case 0: {
			throw new UnsupportedOperationException("Round list is empty.");
		}
		case 1: {
			result = roundRefCounts.iterator().next();
			break;
		}
		default: {
			throw new UnsupportedOperationException(String.format(
					"Cannot calculate maximum possible reference count for rounds with different numbers of references; Unique ref counts: %s",
					roundRefCounts));
		}
		}
		return result;
	}

	private static Stream<Stream<Referent>> getRoundTargetRefs(final Stream<Round> rounds) {
		return rounds.map(Round::getReferents).map(List::stream).map(refs -> refs.filter(Referent::isTarget));
	}

	/**
	 * The total number of rounds in which an entity was referenced which had
	 * already been referenced before (i.e.&nbsp;was coreferenced).
	 */
	private final int coreferenceCount;

	/**
	 * The number of distinct entities (possible referents) in the game.
	 */
	private final int distinctEntityCount;

	/**
	 * The total number of rounds in which an entity was referenced for the
	 * first time.
	 */
	private final int newReferenceCount;

	/**
	 * The total number of rounds in the game.
	 */
	private final int roundCount;

	/**
	 * If the next round in the game would have referenced an entity which had
	 * never been referenced before.
	 */
	private final boolean wouldNextReferentBeNew;

	/**
	 *
	 * @param rounds
	 *            The {@link Round rounds} to calculate reference data for.
	 */
	ReferenceData(final Collection<Round> rounds) {
		this(rounds.size(), getRefCountPerRound(rounds));
	}

	/**
	 *
	 * @param roundCount
	 *            The total number of rounds in the game.
	 * @param distinctEntityCount
	 *            The number of distinct entities (possible referents) in the
	 *            game.
	 */
	ReferenceData(final int roundCount, final int distinctEntityCount) {
		this.roundCount = roundCount;
		this.distinctEntityCount = distinctEntityCount;

		// NOTE: We assume that there is only one target ref per round here
		boolean isNextReferenceNew = INITIAL_IS_NEW_REF;
		int newReferenceCount = 0;
		int coreferenceCount = 0;
		for (int roundId = 0; roundId < roundCount; ++roundId) {
			// In the game, until each element has been picked once, the
			// referents
			// alternate between unseen and already-seen, i.e. alternating
			// between
			// initial reference and coreference
			if (newReferenceCount < distinctEntityCount) {
				if (isNextReferenceNew) {
					newReferenceCount++;
				} else {
					coreferenceCount++;
				}
			} else {
				coreferenceCount++;
			}
			isNextReferenceNew = !isNextReferenceNew;
		}
		assert newReferenceCount <= distinctEntityCount;
		assert newReferenceCount + coreferenceCount == roundCount;
		this.newReferenceCount = newReferenceCount;
		this.coreferenceCount = coreferenceCount;
		wouldNextReferentBeNew = isNextReferenceNew;
	}

	public int coreferenceChainLengthUpperBound() {
		// The maximum number of times any entity could possibly be
		// referenced is all coreferences referring to that one plus 1 for
		// the initial reference
		return coreferenceCount + 1;
	}

	/**
	 *
	 * @return The <a href=
	 *         "https://en.wikipedia.org/wiki/Expected_value">expected</a>
	 *         coreference sequence length for any given entity.
	 */
	public double expectedCorefSeqLength() {
		// final double result = coreferenceCount / (double)
		// distinctEntityCount;
		// assert Double.isFinite(result);
		// assert result >= 0.0;
		// return result;
		// TODO: re-implement: Not correct!
		return 1.0;
	}

	/**
	 * @return If the next round in the game would have referenced an entity
	 *         which had never been referenced before.
	 */
	public boolean isWouldNextReferentBeNew() {
		return wouldNextReferentBeNew;
	}

	public BigDecimal probabilityOfBeingCoreferentFromStart() {
		BigDecimal result = BigDecimal.ZERO;

		final BigDecimal allNewRefEvents = new BigDecimal(newReferenceCount);
		final BigDecimal otherNewRefEvents = allNewRefEvents.subtract(BigDecimal.ONE);
		int roundId = 1;
		BigDecimal lastEventProb = otherNewRefEvents.divide(allNewRefEvents, RoundingMode.HALF_EVEN).pow(roundId);
		for (; roundId < newReferenceCount; ++roundId) {
			final BigDecimal currentEventProb = otherNewRefEvents.divide(allNewRefEvents, RoundingMode.HALF_EVEN)
					.pow(roundId);
			final BigDecimal summand = lastEventProb.subtract(currentEventProb);
			result = result.add(summand);
			lastEventProb = currentEventProb;
		}
		assert result.compareTo(BigDecimal.ZERO) >= 0;
		assert result.compareTo(BigDecimal.ONE) <= 1;

		final BigDecimal corefProb = probabilityOfBeingCoreferent();
		result = result.add(corefProb);
		assert result.compareTo(BigDecimal.ZERO) >= 0;
		assert result.compareTo(BigDecimal.ONE) <= 1;
		return result;
	}

	public boolean wereAllEntitiesReferents() {
		return newReferenceCount >= distinctEntityCount;
	}

	/**
	 *
	 * @return The probability of any given entity being coreferenced at least
	 *         once in the game.
	 */
	private BigDecimal probabilityOfBeingCoreferent() {
		return probabilityOfBeingCoreferent(coreferenceCount);
	}

	/**
	 *
	 * @return The probability of any given entity being coreferenced after a
	 *         given number of rounds in which coreference is possible.
	 */
	private BigDecimal probabilityOfBeingCoreferent(final int corefRoundCount) {
		// https://www.freemathhelp.com/forum/threads/97857-Probability-of-rolling-at-least-one-6-in-3-tries?s=827f028210ba1b9932421b0684f60089&p=398294&viewfull=1#post398294
		final BigDecimal otherEventProb = new BigDecimal(distinctEntityCount - 1)
				.divide(new BigDecimal(distinctEntityCount));
		final BigDecimal result = BigDecimal.ONE.subtract(otherEventProb.pow(corefRoundCount));
		assert result.compareTo(BigDecimal.ZERO) >= 0;
		assert result.compareTo(BigDecimal.ONE) <= 1;
		return result;
	}

}