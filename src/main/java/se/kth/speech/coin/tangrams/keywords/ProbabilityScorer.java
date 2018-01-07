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

import java.util.Map;
import java.util.function.Function;
import java.util.function.ToDoubleFunction;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import it.unimi.dsi.fastutil.objects.Object2DoubleMap;
import it.unimi.dsi.fastutil.objects.Object2DoubleOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2IntMap;

/**
 *
 * @param <O>
 *            The type of observations to probabilities for.
 * @author <a href="mailto:tcshore@kth.se">Todd Shore</a>
 * @since Jan 7, 2018
 *
 */
final class ProbabilityScorer<O> implements ToDoubleFunction<O> {

	private static <O> Object2DoubleMap<O> createProbabilityMap(final Map<? extends O, Long> obsCounts) {
		// NOTE: This should be a double so that floating-point division is done rather
		// than integer division
		final double total = obsCounts.values().stream().mapToLong(Number::longValue).sum();
		final Object2DoubleOpenHashMap<O> result = new Object2DoubleOpenHashMap<>(obsCounts.size());
		result.defaultReturnValue(0.0);
		obsCounts.forEach((obs, count) -> {
			final double prob = count / total;
			final double oldValue = result.put(obs, prob);
			assert oldValue == 0.0;
		});
		result.trim();
		return result;
	}

	private static <O> Object2DoubleMap<O> createProbabilityMap(final Object2IntMap<? extends O> obsCounts) {
		// NOTE: This should be a double so that floating-point division is done rather
		// than integer division
		final double total = sumValues(obsCounts);
		final Object2DoubleOpenHashMap<O> result = new Object2DoubleOpenHashMap<>(obsCounts.size());
		result.defaultReturnValue(0.0);
		for (final Object2IntMap.Entry<? extends O> obsCount : obsCounts.object2IntEntrySet()) {
			final double prob = obsCount.getIntValue() / total;
			final double oldValue = result.put(obsCount.getKey(), prob);
			assert oldValue == 0.0;
		}
		result.trim();
		return result;
	}

	private static <O> Object2DoubleMap<O> createProbabilityMap(final Stream<? extends O> observations) {
		final Map<O, Long> obsCounts = observations
				.collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));
		return createProbabilityMap(obsCounts);
	}

	private static long sumValues(final Object2IntMap<?> map) {
		long result = 0L;
		for (final int value : map.values()) {
			result += value;
		}
		return result;
	}

	private final Object2DoubleMap<O> probs;

	public ProbabilityScorer(final Object2DoubleMap<O> probs) {
		this.probs = probs;
	}

	public ProbabilityScorer(final Object2IntMap<O> obsCounts) {
		this(createProbabilityMap(obsCounts));
	}

	public ProbabilityScorer(final Stream<? extends O> observations) {
		this(createProbabilityMap(observations));
	}

	@Override
	public double applyAsDouble(final O observation) {
		return probs.applyAsDouble(observation);
	}

}
