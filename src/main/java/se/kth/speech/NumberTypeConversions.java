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

import java.math.BigDecimal;
import java.math.BigInteger;

/**
 * @author <a href="mailto:tcshore@kth.se">Todd Shore</a>
 * @since Nov 25, 2017
 *
 */
public final class NumberTypeConversions {

	/**
	 * This method converts a {@link Number} into a {@code double} value,
	 * ensuring that the result value is {@link Double#isFinite(double) finite}.
	 * This is done because certain subtypes of {@link Number} return an
	 * infinite value if the value they represent cannot be represented by a
	 * double-precision floating point value &mdash; such as done by
	 * e.g.&nbsp;{@link BigDecimal} and {@link BigInteger}.
	 *
	 * @param number
	 *            The {@code Number} instance to convert.
	 * @return A {@code double} value representing the given number.
	 */
	public static double finiteDoubleValue(final Number number) {
		final double result = number.doubleValue();
		if (Double.isInfinite(result)) {
			throw new IllegalArgumentException(String.format(
					"The provided number (%s) was converted to an infinite double value (%f).", number, result));
		}
		return result;
	}

	private NumberTypeConversions() {
	}

}
