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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.ListIterator;
import java.util.Set;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * @author <a href="mailto:tcshore@kth.se">Todd Shore</a>
 * @since Nov 23, 2017
 *
 */
public final class Lists {

	public static <T> Set<Integer> createMatchingElementIndexSet(final List<? extends T> list,
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

	public static <T> void ensureIndexIdenticalValues(final List<? super T> list, final int index,
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
