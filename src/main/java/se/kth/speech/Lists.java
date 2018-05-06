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
package se.kth.speech;

import java.util.*;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * @author <a href="mailto:errantlinguist+github@gmail.com">Todd Shore</a>
 * @since Nov 23, 2017
 *
 */
public final class Lists {

	/**
	 *
	 * @param ordering
	 *            The {@link List} to use for ordering.
	 * @return A {@link Comparator} using a given object's index in the given
	 *         {@code List} as its order, with unseen elements ordered last.
	 * @see <a href="http://stackoverflow.com/a/41128993/1391325">Original SO
	 *      answer</a>
	 */
	public static <T> Comparator<T> comparingByIndex(final List<? extends T> ordering) { // NO_UCD
																							// (unused
																							// code)
		return (elem1, elem2) -> Integer.compareUnsigned(ordering.indexOf(elem1), ordering.indexOf(elem2));
	}

	public static <K extends Enum<K>> Map<K, Integer> createIndexMap(final List<? extends K> list,
			final Class<K> keyType) {
		final Map<K, Integer> result = new EnumMap<>(keyType);
		final ListIterator<? extends K> iter = list.listIterator();
		while (iter.hasNext()) {
			final int idx = iter.nextIndex();
			final K next = iter.next();
			result.put(next, idx);
		}
		return result;
	}

	public static <T> Set<Integer> createMatchingElementIndexSet(final List<? extends T> list, // NO_UCD
																								// (unused
																								// code)
			final Predicate<T> matcher) {
		final Set<Integer> result = new HashSet<>();
		for (final ListIterator<? extends T> listIter = list.listIterator(); listIter.hasNext();) {
			final int idx = listIter.nextIndex();
			final T elem = listIter.next();
			if (matcher.test(elem)) {
				result.add(idx);
			}
		}
		return result;
	}

	public static <T> void ensureIndexIdenticalValues(final List<? super T> list, final int index, // NO_UCD
																									// (unused
																									// code)
			final T defaultValue) {
		final int minSize = index + 1;
		final int sizeDiff = minSize - list.size();
		if (sizeDiff > 0) {
			final List<T> addendValues = Collections.nCopies(sizeDiff, defaultValue);
			list.addAll(addendValues);
		}
	}

	public static void ensureIndexNullValues(final List<?> list, final int index) {
		final int minSize = index + 1;
		final int sizeDiff = minSize - list.size();
		if (sizeDiff > 0) {
			list.addAll(Collections.nCopies(sizeDiff, null));
		}
	}

	public static <T> void ensureIndexSuppliedValues(final List<? super T> list, final int index,
			final Supplier<T> defaultValueSupplier) {
		final int minSize = index + 1;
		final int sizeDiff = minSize - list.size();
		if (sizeDiff > 0) {
			final List<T> addendValues = IntStream.range(0, sizeDiff).mapToObj(i -> defaultValueSupplier.get())
					.collect(Collectors.toCollection(() -> new ArrayList<>(sizeDiff)));
			list.addAll(addendValues);
		}
	}

	private Lists() {
	}

}
