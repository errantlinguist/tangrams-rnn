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

/**
 * @author <a href="mailto:errantlinguist+github@gmail.com">Todd Shore</a>
 * @since Nov 26, 2017
 *
 */
public final class HashedCollections {

	/**
	 * The largest power of two that can be represented as an {@code int}.
	 *
	 * @see {@code com.google.common.primitives.Ints.MAX_POWER_OF_TWO} from
	 *      Guava version <em>23.2-jre</em>
	 * @since 10.0
	 */
	private static final int MAX_POWER_OF_TWO = 1 << Integer.SIZE - 2;

	/**
	 * Returns a capacity that is sufficient to keep a hashed collection from
	 * being resized as long as it grows no larger than
	 * <code>expectedSize</code> and the load factor is &geq; its default
	 * (0.75).
	 *
	 * @see {@code com.google.common.collect.Maps.capacity(int)} from Guava
	 *      version <em>23.2-jre</em>
	 * @param expectedSize
	 *            The expected maximum amount of elements in the hashed
	 *            collection.
	 */
	public static int capacity(final int expectedSize) {
		if (expectedSize < 3) {
			checkNonnegative(expectedSize, "expectedSize");
			return expectedSize + 1;
		}
		if (expectedSize < MAX_POWER_OF_TWO) {
			// This is the calculation used in JDK8 to resize when a putAll
			// happens; it seems to be the most conservative calculation we
			// can make. 0.75 is the default load factor.
			return (int) (expectedSize / 0.75F + 1.0F);
		}
		return Integer.MAX_VALUE; // any large value
	}

	/**
	 *
	 * @see {@code com.google.common.collect.CollectPreconditions.checkNonnegative(int, String)}
	 *      from Guava version <em>23.2-jre</em>
	 * @param value
	 *            The value to check.
	 * @param name
	 *            A description of the value.
	 * @return The value if no exception was thrown.
	 * @throws IllegalArgumentException
	 *             If the given value was negative.
	 */
	private static int checkNonnegative(final int value, final String name) {
		if (value < 0) {
			throw new IllegalArgumentException(name + " cannot be negative but was: " + value);
		}
		return value;
	}

	private HashedCollections() {

	}

}
