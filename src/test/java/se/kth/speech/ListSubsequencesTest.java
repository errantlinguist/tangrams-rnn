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
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.Assert;
import org.junit.Test;

/**
 * @author <a href="mailto:tcshore@kth.se">Todd Shore</a>
 * @since 30 May 2017
 *
 */
public final class ListSubsequencesTest {

	/**
	 * Test method for
	 * {@link se.kth.speech.ListSubsequences#createDeduplicatedAdjacentSubsequenceList(java.util.List)}.
	 */
	@Test
	public void testCreateDeduplicatedAdjacentSubsequenceListEven() {
		final List<String> duplicatedSubseq = Arrays.asList("I", "can't");
		final List<List<String>> repetitions = Arrays.asList(duplicatedSubseq, duplicatedSubseq);
		final List<String> input = Arrays.asList(repetitions.stream().flatMap(List::stream).toArray(String[]::new));
		final List<String> expected = duplicatedSubseq;
		final List<String> actual = ListSubsequences.createDeduplicatedAdjacentSubsequenceList(input);
		Assert.assertEquals(expected, actual);
	}

	/**
	 * Test method for
	 * {@link se.kth.speech.ListSubsequences#createDeduplicatedAdjacentSubsequenceListFromListOfSubsequences(java.util.List)}.
	 */
	@Test
	public void testCreateDeduplicatedAdjacentSubsequenceListFromListOfSubsequences() {
		final List<String> duplicatedSubseq = Arrays.asList("I", "can't");
		final List<List<String>> input = Arrays.asList(duplicatedSubseq, duplicatedSubseq, Arrays.asList("do", "it"),
				Arrays.asList("well"));
		final List<List<String>> expected = Arrays.asList(duplicatedSubseq, Arrays.asList("do", "it"),
				Arrays.asList("well"));
		final List<List<String>> actual = ListSubsequences
				.createDeduplicatedAdjacentSubsequenceListFromListOfSubsequences(input);
		Assert.assertEquals(expected, actual);
	}

	/**
	 * Test method for
	 * {@link se.kth.speech.ListSubsequences#createDeduplicatedAdjacentSubsequenceListFromListOfSubsequences(java.util.List)}.
	 */
	@Test
	public void testCreateDeduplicatedAdjacentSubsequenceListFromListOfSubsequencesEvenSubseqCount() {
		final List<String> duplicatedSubseq = Arrays.asList("I", "can't");
		final List<List<String>> input = Arrays.asList(duplicatedSubseq, duplicatedSubseq);
		final List<List<String>> expected = Arrays.asList(duplicatedSubseq);
		final List<List<String>> actual = ListSubsequences
				.createDeduplicatedAdjacentSubsequenceListFromListOfSubsequences(input);
		Assert.assertEquals(expected, actual);
	}

	/**
	 * Test method for
	 * {@link se.kth.speech.ListSubsequences#createDeduplicatedAdjacentSubsequenceList(java.util.List)}.
	 */
	@Test
	public void testCreateDeduplicatedAdjacentSubsequenceListNegative1() {
		final List<String> input = Arrays.asList("I", "can't");
		final List<String> expected = input;
		final List<String> actual = ListSubsequences.createDeduplicatedAdjacentSubsequenceList(input);
		Assert.assertEquals(expected, actual);
	}

	/**
	 * Test method for
	 * {@link se.kth.speech.ListSubsequences#createDeduplicatedAdjacentSubsequenceList(java.util.List)}.
	 */
	@Test
	public void testCreateDeduplicatedAdjacentSubsequenceListNegative2() {
		final List<String> input = Arrays.asList("uh", "you", "should", "explain");
		final List<String> expected = input;
		final List<String> actual = ListSubsequences.createDeduplicatedAdjacentSubsequenceList(input);
		Assert.assertEquals(expected, actual);
	}

	/**
	 * Test method for
	 * {@link se.kth.speech.ListSubsequences#createDeduplicatedAdjacentSubsequenceList(java.util.List)}.
	 */
	@Test
	public void testCreateDeduplicatedAdjacentSubsequenceListNegativeEmpty() {
		final List<String> input = Collections.emptyList();
		final List<String> expected = input;
		final List<String> actual = ListSubsequences.createDeduplicatedAdjacentSubsequenceList(input);
		Assert.assertEquals(expected, actual);
	}

	/**
	 * Test method for
	 * {@link se.kth.speech.ListSubsequences#createDeduplicatedAdjacentSubsequenceList(java.util.List)}.
	 */
	@Test
	public void testCreateDeduplicatedAdjacentSubsequenceListSingleElem() {
		final List<String> input = Arrays.asList("I");
		final List<String> expected = input;
		final List<String> actual = ListSubsequences.createDeduplicatedAdjacentSubsequenceList(input);
		Assert.assertEquals(expected, actual);
	}

	/**
	 * Test method for
	 * {@link se.kth.speech.ListSubsequences#createDeduplicatedAdjacentSubsequenceList(java.util.List)}.
	 */
	@Test
	public void testCreateDeduplicatedAdjacentSubsequenceListUneven() {
		final List<String> input = Arrays.asList("I", "can't", "I", "can't", "do", "I", "can't", "do", "it");
		final List<String> expected = Arrays.asList("I", "can't", "do", "it");
		final List<String> actual = ListSubsequences.createDeduplicatedAdjacentSubsequenceList(input);
		Assert.assertEquals(expected, actual);
	}

	/**
	 * Test method for
	 * {@link se.kth.speech.ListSubsequences#createSubsequenceList(java.util.List, int)}.
	 */
	@Test
	public void testCreateSubsequenceListEvenLength() {
		final List<String> input = Arrays.asList("I", "can't", "do", "it");
		final List<List<String>> expected = Arrays.asList(Arrays.asList("I", "can't"), Arrays.asList("do", "it"));
		final List<List<String>> actual = ListSubsequences.createSubsequenceList(input, 2);
		Assert.assertEquals(expected, actual);
	}

	/**
	 * Test method for
	 * {@link se.kth.speech.ListSubsequences#createSubsequenceList(java.util.List, int)}.
	 */
	@Test
	public void testCreateSubsequenceListLengthOne() {
		final List<String> input = Arrays.asList("I", "can't", "do", "it", "well");
		final int subseqLen = 1;
		final List<List<String>> expected = input.stream().map(Collections::singletonList)
				.collect(Collectors.toCollection(() -> new ArrayList<>(Math.max(input.size() / subseqLen, 1))));
		final List<List<String>> actual = ListSubsequences.createSubsequenceList(input, subseqLen);
		Assert.assertEquals(expected, actual);
	}

	/**
	 * Test method for
	 * {@link se.kth.speech.ListSubsequences#createSubsequenceList(java.util.List, int)}.
	 */
	@Test
	public void testCreateSubsequenceListOddLength() {
		final List<String> input = Arrays.asList("I", "can't", "do", "it", "well");
		final List<List<String>> expected = Arrays.asList(Arrays.asList("I", "can't"), Arrays.asList("do", "it"),
				Arrays.asList("well"));
		final List<List<String>> actual = ListSubsequences.createSubsequenceList(input, 2);
		Assert.assertEquals(expected, actual);
	}

	/**
	 * Test method for
	 * {@link se.kth.speech.ListSubsequences#createSubsequenceList(java.util.List, int)}.
	 */
	@Test
	public void testCreateSubsequenceListSame() {
		final List<String> duplicatedSubseq = Arrays.asList("I", "can't");
		final List<List<String>> expected = Arrays.asList(duplicatedSubseq, duplicatedSubseq);
		final List<String> input = Arrays.asList(expected.stream().flatMap(List::stream).toArray(String[]::new));
		final List<List<String>> actual = ListSubsequences.createSubsequenceList(input, 2);
		Assert.assertEquals(expected, actual);
	}

	/**
	 * Test method for
	 * {@link se.kth.speech.ListSubsequences#createSubsequenceList(java.util.List, int)}.
	 */
	@Test
	public void testCreateSubsequenceListSameLength() {
		final List<String> input = Arrays.asList("I", "can't", "do", "it", "well");
		final List<List<String>> expected = Arrays.asList(input);
		final List<List<String>> actual = ListSubsequences.createSubsequenceList(input, input.size());
		Assert.assertEquals(expected, actual);
	}

}
