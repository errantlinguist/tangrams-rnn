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
import java.util.Arrays;
import java.util.EnumMap;
import java.util.Map;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author <a href="mailto:tcshore@kth.se">Todd Shore</a>
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
	CROSS_VALIDATION_ITER_COUNT("ci", 1) {
		@Override
		public Option.Builder createCLIOptionBuilder() {
			return Option.builder(getOptName()).longOpt("cviters")
					.desc("The number of times a full cross-validation should be performed; This is useful for cases where randomization might affect results.")
					.hasArg().argName("count").type(Number.class);
		}

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
	DISCOUNT("sm", 3) {
		@Override
		public Option.Builder createCLIOptionBuilder() {
			return Option.builder(getOptName()).longOpt("smoothing")
					.desc("Only build model for words with more or equal number of instances than this.").hasArg()
					.argName("mincount").type(Number.class);
		}

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
	ONLY_INSTRUCTOR("oi", true) {
		@Override
		public Option.Builder createCLIOptionBuilder() {
			// https://stackoverflow.com/a/33379307/1391325
			return Option.builder(getOptName()).longOpt("only-instructor")
					.desc("Only use language from the instructor, i.e. ignore language from the manipulator.").hasArg()
					.argName("boolean").optionalArg(true).numberOfArgs(1);
		}

		@Override
		protected Object parseValue(final CommandLine cl) {
			return parseFlag(cl);
		}

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
	RANDOM_SEED("rs", 1L) {
		@Override
		public Option.Builder createCLIOptionBuilder() {
			return Option.builder(getOptName()).longOpt("random-seed")
					.desc("The seed used for random number generation.").hasArg().argName("seed").type(Number.class);
		}

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
	TRAINING_SET_SIZE_DISCOUNT("td", 0) {
		@Override
		public Option.Builder createCLIOptionBuilder() {
			return Option.builder(getOptName()).longOpt("training-discount")
					.desc("The number of sessions to discount from each cross-validation training set.").hasArg()
					.argName("count").type(Number.class);
		}

		/**
		 * @return A non-negative {@link Integer} value.
		 */
		@Override
		protected Object parseValue(final String input) {
			return parseNonNegativeInteger(input);
		}
	},

	/**
	 * Weight for incremental updates (relative to 1.0 for the background
	 * model); This should be a non-negative {@link Number}.
	 */
	UPDATE_WEIGHT("uw", BigDecimal.ZERO) {
		@Override
		public Option.Builder createCLIOptionBuilder() {
			return Option.builder(getOptName()).longOpt("update-weight")
					.desc("Weight for incremental updates (relative to 1.0 for the background model).").hasArg()
					.argName("weight").type(Number.class);
		}

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
	WEIGHT_BY_FREQ("wf", false) {
		@Override
		public Option.Builder createCLIOptionBuilder() {
			return Option.builder(getOptName()).longOpt("weight-by-freq").desc("Weight score by word frequency.");
		}

		@Override
		protected Object parseValue(final CommandLine cl) {
			return parseFlag(cl);
		}

		/**
		 * @return A {@link Boolean} value.
		 */
		@Override
		protected Object parseValue(final String input) {
			return Boolean.valueOf(input);
		}
	};

	private static final Logger LOGGER = LoggerFactory.getLogger(ModelParameter.class);

	static {
		final ModelParameter[] params = ModelParameter.values();
		assert Arrays.stream(params).map(ModelParameter::getOptName).distinct().count() == params.length;
	}

	/**
	 * Creates a {@link Map} of {@link ModelParameter} values largely analogous
	 * to those used for classification by Kennington et al. (2015) and
	 * Kennington &amp; Schlangen (2015) but with slight variation to improve
	 * classification accuracy on the dataset used during development: They used
	 * {@link #DISCOUNT} of <code>4</code>, whereas this method uses
	 * <code>3</code> instead.
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
	public static Map<ModelParameter, Object> createDefaultParamValueMap() { // NO_UCD
																				// (use
																				// default)
		final Map<ModelParameter, Object> result = new EnumMap<>(ModelParameter.class);
		final ModelParameter[] params = ModelParameter.values();
		Arrays.stream(params).forEach(param -> result.put(param, param.getDefaultValue()));
		assert result.size() == params.length;
		return result;
	}

	/**
	 * Creates a {@link Map} of {@link ModelParameter} values represented by a
	 * given {@link CommandLine}.
	 *
	 * @param cl
	 *            The {@code CommandLine} instance to parse values from.
	 * @return A new {@link Map} of training parameters.
	 */
	public static Map<ModelParameter, Object> createParamValueMap(final CommandLine cl) { // NO_UCD (use default)
		final Map<ModelParameter, Object> result = new EnumMap<>(ModelParameter.class);
		final ModelParameter[] params = ModelParameter.values();
		for (final ModelParameter param : params) {
			final Object parsedValue = param.parseValue(cl);
			result.put(param, parsedValue);
		}
		assert result.size() == params.length;
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

	private final Object defaultValue;

	private final String optName;

	private ModelParameter(final String optName, final Object defaultValue) {
		this.optName = optName;
		this.defaultValue = defaultValue;
	}

	public abstract Option.Builder createCLIOptionBuilder();

	/**
	 * @return the defaultValue
	 */
	public Object getDefaultValue() {
		return defaultValue;
	}

	protected String createShortOptRepr() {
		return "-" + getOptName();
	}

	/**
	 * @return the optName
	 */
	protected String getOptName() {
		return optName;
	}

	protected Object parseFlag(final CommandLine cl) {
		final Object result;
		final String optName = getOptName();
		if (cl.hasOption(optName)) {
			final String optValue = cl.getOptionValue(optName);
			if (optValue == null) {
				// A "flag-style" option (i.e. "--flag", without any args)
				// means "true"
				result = Boolean.TRUE;
			} else {
				result = parseValue(optValue);
			}
			LOGGER.info("Parsed a value of \"{}\" for option {} (\"{}\").", result, this, createShortOptRepr());
		} else {
			result = getDefaultValue();
			LOGGER.info("Option {} (\"{}\") not present; using default value of \"{}\" for it.", this,
					createShortOptRepr(), result);
		}
		assert result instanceof Boolean;
		return result;
	}

	/**
	 * Parses the value for the given {@link ModelParameter} represented by a
	 * given {@link CommandLine}.
	 *
	 * @param cl
	 *            The {@code CommandLine} instance to parse values from.
	 * @return The parsed value or the {@link #getDefaultValue() default value}
	 *         if not present.
	 */
	protected Object parseValue(final CommandLine cl) {
		final String optValue = cl.getOptionValue(optName);
		final Object result;
		if (optValue == null) {
			result = getDefaultValue();
			LOGGER.info("Option {} (\"{}\") not present; using default value of \"{}\" for it.", this,
					createShortOptRepr(), result);
		} else {
			result = parseValue(optValue);
			LOGGER.info("Parsed a value of \"{}\" for option {} (\"{}\").", result, this, createShortOptRepr());
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
