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
package se.kth.speech.coin.tangrams.wac.logistic;

import java.math.BigDecimal;
import java.util.EnumMap;
import java.util.Map;

/**
 * @author <a href="mailto:errantlinguist+github@gmail.com">Todd Shore</a>
 * @since Nov 25, 2017
 *
 */
public enum ModelParameter {
	/**
	 * The number of times a full cross-validation should be performed; This is
	 * useful for cases where randomization might affect results, such as when
	 * {@link #TRAINING_SET_SIZE_DISCOUNT} is greater than 0. This should be a
	 * positive {@link Integer}.
	 */
	CROSS_VALIDATION_ITER_COUNT {
		/**
		 * @return A positive {@link Integer} value.
		 */
		@Override
		protected Object parseValue(final String input) {
			return parsePositiveInteger(input);
		}
	},
	/**
	 * Only build model for words with more or equal number of instances than
	 * this; This should be a positive {@link Integer}.
	 */
	DISCOUNT {
		/**
		 * @return A positive {@link Integer} value.
		 */
		@Override
		protected Object parseValue(final String input) {
			return parsePositiveInteger(input);
		}
	},

	/**
	 * Only use language from the instructor, i.e.&nbsp;ignore language from the
	 * manipulator; This should be a {@link Boolean}.
	 */
	ONLY_INSTRUCTOR {
		/**
		 * @return A {@link Boolean} value.
		 */
		@Override
		protected Object parseValue(final String input) {
			return Boolean.valueOf(input);
		}
	},
	/**
	 * The seed used for random number generation; This should be a
	 * {@link Long}.
	 */
	RANDOM_SEED {
		/**
		 * A {@link Long} value.
		 */
		@Override
		protected Object parseValue(final String input) {
			return Long.valueOf(input);
		}
	},
	/**
	 * The number of sessions to discount from each cross-validation training
	 * set; This should be a non-negative {@link Integer}.
	 */
	TRAINING_SET_SIZE_DISCOUNT {
		/**
		 * @return A non-negative {@link Integer} value.
		 */
		@Override
		protected Object parseValue(final String input) {
			return parseNonNegativeInteger(input);
		}
	},

	/**
	 * Weight for incremental updates (relative to 1.0 for background model);
	 * This should be a non-negative {@link Number}.
	 */
	UPDATE_WEIGHT {
		/**
		 * A non-negative {@link BigDecimal} instance.
		 */
		@Override
		protected Object parseValue(final String input) {
			return parseNonNegativeBigDecimal(input);
		}
	},
	/**
	 * Weight score by word frequency; This should be a {@link Boolean}.
	 */
	WEIGHT_BY_FREQ {
		/**
		 * @return A {@link Boolean} value.
		 */
		@Override
		protected Object parseValue(final String input) {
			return Boolean.valueOf(input);
		}
	};

	/**
	 * Creates a {@link Map} of {@link ModelParameter} values largely analogous
	 * to those used for classification by Kennington et al. (2015) and
	 * Kennington &amp; Schlangen (2015) but with slight variation to improve
	 * classification accuracy on the dataset used during development: They used
	 * {@link #DISCOUNT} of <code>4</code>, whereas this method uses
	 * <code>3</code> instead. Likewise, this method sets
	 * {@link #WEIGHT_BY_FREQ} to true <code>true</code>, whereas it is
	 * (currently) unknown if the other authors did something analogous to this.
	 *
	 * @see
	 *      <ul>
	 *      <li><a href="http://anthology.aclweb.org/W/W15/W15-0124.pdf">Casey
	 *      Kennington, Livia Dia, &amp; David Schlangen (2015). &ldquo;A
	 *      Discriminative Model for Perceptually-Grounded Incremental Reference
	 *      Resolution.&rdquo; In <em>Proceedings of IWCS 2015</em><a>.</li>
	 *      <li><a href="http://www.aclweb.org/anthology/P15-1029">Casey
	 *      Kennington, &amp; David Schlangen (2015). &ldquo;Simple Learning and
	 *      Compositional Application of Perceptually Grounded Word Meanings for
	 *      Incremental Reference Resolution&rdquo;. In <em>Proceedings of the
	 *      53<sup>rd</sup> Annual Meeting of the Association for Computational
	 *      Linguistics and the 7<sup>th</sup> International Joint Conference on
	 *      Natural Language Processing</em><a>.</li>
	 *      </ul>
	 *
	 * @return A new {@link Map} of training parameters.
	 */
	public static Map<ModelParameter, Object> createDefaultParamValueMap() {
		final Map<ModelParameter, Object> result = new EnumMap<>(ModelParameter.class);
		result.put(CROSS_VALIDATION_ITER_COUNT, 1);
		result.put(DISCOUNT, 3);
		result.put(ONLY_INSTRUCTOR, true);
		result.put(RANDOM_SEED, 1L);
		result.put(TRAINING_SET_SIZE_DISCOUNT, 0);
		result.put(UPDATE_WEIGHT, BigDecimal.ZERO);
		result.put(WEIGHT_BY_FREQ, true);
		assert result.size() == ModelParameter.values().length;
		return result;
	}

	/**
	 * Parses a {@link BigDecimal} from a given string, ensuring that the value
	 * is non-negative.
	 *
	 * @param input
	 *            The {@link String} to parse.
	 * @return A new {@code BigDecimal} instance representing a non-negative
	 *         value.
	 * @throws IllegalArgumentException
	 *             if the value is negative.
	 *
	 */
	private static BigDecimal parseNonNegativeBigDecimal(final String input) {
		final BigDecimal result = new BigDecimal(input);
		if (BigDecimal.ZERO.compareTo(result) > 0) {
			throw new IllegalArgumentException(
					String.format("Value must be non-negative but received \"%s\" as input.", input));
		}
		return result;
	}

	/**
	 * Parses a {@link Integer} from a given string, ensuring that the value is
	 * non-negative.
	 *
	 * @param input
	 *            The {@link String} to parse.
	 * @return An {@code Integer} instance representing a non-negative value.
	 * @throws IllegalArgumentException
	 *             if the value is negative.
	 *
	 */
	private static Integer parseNonNegativeInteger(final String input) {
		final Integer result = Integer.valueOf(input);
		if (result < 0) {
			throw new IllegalArgumentException(
					String.format("Value must be non-negative but received \"%s\" as input.", input));
		}
		return result;
	}

	/**
	 * Parses a {@link Integer} from a given string, ensuring that the value is
	 * positive.
	 *
	 * @param input
	 *            The {@link String} to parse.
	 * @return An {@code Integer} instance representing a positive value.
	 * @throws IllegalArgumentException
	 *             if the value is non-positive.
	 *
	 */
	private static Integer parsePositiveInteger(final String input) {
		final Integer result = Integer.valueOf(input);
		if (result < 1) {
			throw new IllegalArgumentException(
					String.format("Value must be positive but received \"%s\" as input.", input));
		}
		return result;
	}

	/**
	 * Parses a string as a value for this {@link ModelParameter parameter}.
	 *
	 * @param input
	 *            The {@link String} to parse.
	 * @return The parsed value.
	 */
	protected abstract Object parseValue(String input);
}
