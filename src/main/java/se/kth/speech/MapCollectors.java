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

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collector;
import java.util.stream.Collectors;

/**
 * @author <a href="mailto:errantlinguist+github@gmail.com">Todd Shore</a>
 * @since Nov 26, 2017
 *
 */
public final class MapCollectors {

	/**
	 * Returns a merge function, suitable for use in
	 * {@link Map#merge(Object, Object, BiFunction) Map.merge()} or
	 * {@link #toMap(Function, Function, BinaryOperator) toMap()}, which always
	 * throws {@code IllegalStateException}. This can be used to enforce the
	 * assumption that the elements being collected are distinct.
	 *
	 * @param <T>
	 *            the type of input arguments to the merge function
	 * @return a merge function which always throw {@code IllegalStateException}
	 * @see {@link Collectors#throwingMerger()}
	 */
	public static <T> BinaryOperator<T> throwingMerger() {
		return (u, v) -> {
			throw new IllegalStateException(String.format("Duplicate key %s", u));
		};
	}

	/**
	 * Returns a {@code Collector} that accumulates elements into a {@code Map}
	 * whose keys and values are the result of applying the provided mapping
	 * functions to the input elements.
	 *
	 * <p>
	 * If the mapped keys contains duplicates (according to
	 * {@link Object#equals(Object)}), an {@code IllegalStateException} is
	 * thrown when the collection operation is performed. If the mapped keys may
	 * have duplicates, use {@link #toMap(Function, Function, BinaryOperator)}
	 * instead.
	 *
	 * @apiNote It is common for either the key or the value to be the input
	 *          elements. In this case, the utility method
	 *          {@link java.util.function.Function#identity()} may be helpful.
	 *          For example, the following produces a {@code Map} mapping
	 *          students to their grade point average:
	 *
	 *          <pre>
	 * {@code
	 *     Map<Student, Double> studentToGPA
	 *         students.stream().collect(toMap(Functions.identity(),
	 *                                         student -> computeGPA(student)));
	 * }
	 *          </pre>
	 *
	 *          And the following produces a {@code Map} mapping a unique
	 *          identifier to students:
	 *
	 *          <pre>
	 * {@code
	 *     Map<String, Student> studentIdToStudent
	 *         students.stream().collect(toMap(Student::getId,
	 *                                         Functions.identity());
	 * }
	 *          </pre>
	 *
	 * @implNote The returned {@code Collector} is not concurrent. For parallel
	 *           stream pipelines, the {@code combiner} function operates by
	 *           merging the keys from one map into another, which can be an
	 *           expensive operation. If it is not required that results are
	 *           inserted into the {@code Map} in encounter order, using
	 *           {@link #toConcurrentMap(Function, Function)} may offer better
	 *           parallel performance.
	 *
	 * @param <T>
	 *            the type of the input elements
	 * @param <K>
	 *            the output type of the key mapping function
	 * @param <U>
	 *            the output type of the value mapping function
	 * @param keyMapper
	 *            a mapping function to produce keys
	 * @param valueMapper
	 *            a mapping function to produce values
	 * @param expectedSize
	 *            The expected maximum amount of elements in the result map.
	 * @return a {@code Collector} which collects elements into a {@code Map}
	 *         whose keys and values are the result of applying mapping
	 *         functions to the input elements
	 *
	 * @see #CollectorstoMap(Function, Function)
	 * @see #CollectorstoMap(Function, Function, BinaryOperator)
	 * @see #CollectorstoMap(Function, Function, BinaryOperator, Supplier)
	 * @see #CollectorstoConcurrentMap(Function, Function)
	 */
	public static <T, K, U> Collector<T, ?, Map<K, U>> toMap(final Function<? super T, ? extends K> keyMapper,
			final Function<? super T, ? extends U> valueMapper, final int expectedSize) {
		return Collectors.toMap(keyMapper, valueMapper, throwingMerger(),
				() -> new HashMap<>(HashedCollections.capacity(expectedSize)));
	}

	private MapCollectors() {
	}

}
