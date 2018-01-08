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
package se.kth.speech.nlp;

import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.function.ToDoubleBiFunction;

import it.unimi.dsi.fastutil.doubles.DoubleCollection;
import it.unimi.dsi.fastutil.doubles.DoubleIterable;
import it.unimi.dsi.fastutil.objects.Object2DoubleMap;
import it.unimi.dsi.fastutil.objects.Object2DoubleOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import se.kth.speech.HashedCollections;

/**
 * @param <D>
 *            The class representing a document in which observations are found.
 * @param <O>
 *            The type of observations to calculate TF-IDF scores for.
 * @author <a href="mailto:errantlinguist+github@gmail.com">Todd Shore</a>
 * @since Dec 1, 2017
 *
 */
public final class TfIdfScorer<O, D> implements ToDoubleBiFunction<O, D> {

	/**
	 * @author <a href="mailto:errantlinguist+github@gmail.com">Todd Shore</a>
	 * @since Dec 3, 2017
	 * @see <a href="https://doi.org/10.1017%2FCBO9780511809071.007">Manning,
	 *      Christopher D. et al (2008). <em>Introduction to Information
	 *      Retrieval</em>, p.&nbsp;128.</a>
	 *
	 */
	public enum TermFrequencyVariant {
		/**
		 * <em>tf(t,d)</em> = 0.5 + 0.5 &sdot; (<em>f</em><sub>t,d</sub> &divide;
		 * max<sub><em>t&prime;</em> &isin; <em>d</em></sub>
		 * <em>f</em><sub><em>t&prime;,d</em></sub>)
		 *
		 */
		AUGMENTED,
		/**
		 * Raw counts of a given term <em>t</em> in a given document <em>d</em>
		 * <em>tf(t,d)</em> = <em>f</em><sub><em>t,d</em></sub>.
		 */
		NATURAL
	}

	private static final int DEFAULT_INITIAL_WORD_MAP_CAPACITY = 1000;

	public static <O, D> TfIdfScorer<O, D> create(
			final Map<? extends D, ? extends Object2IntMap<? extends O>> docObservationCounts) {
		return create(docObservationCounts, TermFrequencyVariant.NATURAL);
	}

	public static <O, D> TfIdfScorer<O, D> create(
			final Map<? extends D, ? extends Object2IntMap<? extends O>> docObservationCounts,
			final TermFrequencyVariant tfVariant) {
		final int docCount = docObservationCounts.size();
		final int initialDocSetCapcity = HashedCollections.capacity(docCount);
		final Object2ObjectOpenHashMap<D, Object2DoubleOpenHashMap<O>> observationCountsPerDoc = new Object2ObjectOpenHashMap<>(
				docObservationCounts.size());
		final Object2ObjectOpenHashMap<O, Set<D>> observationDocs = new Object2ObjectOpenHashMap<>(
				DEFAULT_INITIAL_WORD_MAP_CAPACITY);
		for (final Entry<? extends D, ? extends Object2IntMap<? extends O>> entry : docObservationCounts.entrySet()) {
			final D doc = entry.getKey();
			final Object2DoubleMap<O> docTokenCounts = observationCountsPerDoc.computeIfAbsent(doc, key -> {
				final Object2DoubleOpenHashMap<O> counts = new Object2DoubleOpenHashMap<>(
						DEFAULT_INITIAL_WORD_MAP_CAPACITY);
				counts.defaultReturnValue(0.0);
				return counts;
			});
			final Object2IntMap<? extends O> observationCounts = entry.getValue();
			observationCounts.object2IntEntrySet().forEach(observationCount -> {
				final O observation = observationCount.getKey();
				final int count = observationCount.getIntValue();
				incrementCount(observation, count, docTokenCounts);
				observationDocs.computeIfAbsent(observation, key -> new HashSet<>(initialDocSetCapcity)).add(doc);
			});
		}

		observationCountsPerDoc.trim();
		observationCountsPerDoc.values().forEach(Object2DoubleOpenHashMap::trim);
		observationDocs.trim();
		return new TfIdfScorer<>(observationCountsPerDoc, observationDocs, docObservationCounts.size(), tfVariant);
	}

	private static <K> void incrementCount(final K key, final int addend, final Object2DoubleMap<? super K> counts) {
		final double augend = counts.getDouble(key);
		final double oldValue = counts.put(key, augend + addend);
		assert oldValue == augend;
	}

	private static double max(final DoubleIterable values) {
		double result = Double.NEGATIVE_INFINITY;
		for (final double value : values) {
			result = Math.max(result, value);
		}
		return result;
	}

	private final Map<D, ? extends Object2DoubleMap<O>> observationCountsPerDoc;

	private final Map<O, Set<D>> observationDocs;

	private final ToDoubleBiFunction<O, D> tfCalculator;

	private final double totalDocCount;

	private TfIdfScorer(final Map<D, ? extends Object2DoubleMap<O>> observationCountsPerDoc,
			final Map<O, Set<D>> observationDocs, final int totalDocCount, final TermFrequencyVariant tfVariant) {
		this.observationCountsPerDoc = observationCountsPerDoc;
		this.observationDocs = observationDocs;
		this.totalDocCount = totalDocCount;
		this.tfCalculator = getTermFrequencyCalculator(tfVariant);
	}

	@Override
	public double applyAsDouble(final O observation, final D doc) {
		final double tf = tfCalculator.applyAsDouble(observation, doc);
		final double idf = idf(observation);
		return tf * idf;
	}

	/**
	 * <em>tf(t,d)</em> = 0.5 + 0.5 &sdot; (<em>f</em><sub>t,d</sub> &divide;
	 * max<sub><em>t&prime;</em> &isin; <em>d</em></sub>
	 * <em>f</em><sub><em>t&prime;,d</em></sub>)
	 *
	 * @param observation
	 *            The observation to calculate the term frequency of.
	 * @param doc
	 *            The document in which the given observation was found.
	 * @return The augmented term frequency for the given observation during the
	 *         given document.
	 */
	private double augmentedTf(final O observation, final D doc) {
		final Object2DoubleMap<O> docWordCounts = observationCountsPerDoc.get(doc);
		assert docWordCounts != null;
		final double naturalTf = docWordCounts.getDouble(observation);
		assert naturalTf != docWordCounts.defaultReturnValue() : String
				.format("Term frequency for \"%s\" is null for document %s.", observation, doc);
		final DoubleCollection docTfs = docWordCounts.values();
		final double maxDocTf = max(docTfs);
		return 0.5 + 0.5 * (naturalTf / maxDocTf);
	}

	private double df(final O observation) {
		final Set<D> docs = observationDocs.get(observation);
		assert docs != null : String.format("Document set for \"%s\" is null.", observation);
		final int result = docs.size();
		assert result > 0 : String.format("Document set for \"%s\" is of size %d.", observation, result);
		return result;
	}

	private ToDoubleBiFunction<O, D> getTermFrequencyCalculator(final TermFrequencyVariant variant) {
		final ToDoubleBiFunction<O, D> result;
		switch (variant) {
		case AUGMENTED: {
			result = this::augmentedTf;
			break;
		}
		case NATURAL: {
			result = this::naturalTf;
			break;
		}
		default:
			throw new AssertionError("Missing enum-handling logic.");

		}
		return result;
	}

	private double idf(final O observation) {
		final double df = df(observation);
		final double result = Math.log(totalDocCount / df);
		assert Double.isFinite(result) : String.format("IDF score for \"%s\" is not finite.", observation);
		return result;
	}

	/**
	 * Calculates the term frequency for a given observation <em>t</em> in a given
	 * document <em>d</em> as the raw counts <em>tf(t,d)</em> =
	 * <em>f</em><sub><em>t,d</em></sub>.
	 *
	 * @param observation
	 *            The observation to calculate the term frequency of.
	 * @param doc
	 *            The document in which the given observation was found.
	 * @return The natural term frequency for the given observation during the given
	 *         document.
	 */
	private double naturalTf(final O observation, final D doc) {
		final Map<O, Double> docWordCounts = observationCountsPerDoc.get(doc);
		assert docWordCounts != null;
		final Double result = docWordCounts.get(observation);
		assert result != null : String.format("Term frequency for \"%s\" is null for document %s.", observation, doc);
		assert Double.isFinite(result) : String.format("Term frequency for \"%s\" is not finite for document %s.",
				observation, doc);
		return result;
	}
}
