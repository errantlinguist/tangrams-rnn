/*
 *  This file is part of se.kth.speech.coin.tangrams-restricted.analysis.
 *
 *  se.kth.speech.coin.tangrams-restricted.analysis is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
*/
package se.kth.speech;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author <a href="mailto:tcshore@kth.se">Todd Shore</a>
 * @since 30 May 2017
 *
 */
public final class ListSubsequences {

	public static <T> List<T> createDeduplicatedAdjacentSubsequenceList(final List<T> list) {
		// TODO: Optimize this: The complexity is terrible
		final List<T> result;
		if (list.size() == 1) {
			result = list;
		} else {
			List<T> intermediateResult = list;
			for (int subseqLength = intermediateResult.size() - 1; subseqLength > 0; --subseqLength) {
				final List<List<T>> subseqs = createSubsequenceList(intermediateResult, subseqLength);
				final List<List<T>> deduplicatedSubseqs = createDeduplicatedAdjacentSubsequenceListFromListOfSubsequences(
						subseqs);
				final Stream<List<T>> nextLevel = deduplicatedSubseqs.parallelStream()
						.map(ListSubsequences::createDeduplicatedAdjacentSubsequenceList);
				intermediateResult = nextLevel.flatMap(List::stream).collect(Collectors.toList());
			}
			result = intermediateResult;
		}

		return result;
	}

	public static <T> List<List<T>> createDeduplicatedAdjacentSubsequenceListFromListOfSubsequences(
			final List<List<T>> subseqs) {
		final List<List<T>> result = new ArrayList<>(subseqs.size());
		final Iterator<List<T>> subSeqIter = subseqs.iterator();
		while (subSeqIter.hasNext()) {
			final List<T> firstSubseq = subSeqIter.next();
			result.add(firstSubseq);
			if (subSeqIter.hasNext()) {
				final List<T> nextSubSeq = subSeqIter.next();
				if (!firstSubseq.equals(nextSubSeq)) {
					result.add(nextSubSeq);
				}
			}
		}
		return result;
	}

	public static <T> List<List<T>> createSubsequenceList(final List<T> list, final int subseqLength) {
		final List<List<T>> result;
		if (subseqLength < 1) {
			throw new IllegalArgumentException("Subsequence length must be positive.");
		} else {
			result = new ArrayList<>(Math.max(list.size() / subseqLength, 1));
			final int maxSubseqStart = list.size() - subseqLength;
			int lastSubseqEnd = 0;
			for (int subseqStart = 0; subseqStart < maxSubseqStart; subseqStart += subseqLength) {
				final int subseqEnd = subseqStart + subseqLength;
				final List<T> subseq = list.subList(subseqStart, subseqEnd);
				result.add(subseq);
				lastSubseqEnd = subseqEnd;
			}

			final int trailingSubseqLength = list.size() - lastSubseqEnd;
			if (trailingSubseqLength > 0) {
				final List<T> trailingSubseq = list.subList(lastSubseqEnd, list.size());
				assert trailingSubseq.size() == trailingSubseqLength;
				result.add(trailingSubseq);
			}
		}
		return result;
	}

	private ListSubsequences() {
	}

}
