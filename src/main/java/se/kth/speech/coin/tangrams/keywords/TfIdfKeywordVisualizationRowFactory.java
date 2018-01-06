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
package se.kth.speech.coin.tangrams.keywords;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Function;
import java.util.function.ToDoubleFunction;
import java.util.stream.DoubleStream;
import java.util.stream.Stream;

import org.apache.commons.lang3.tuple.Pair;

import it.unimi.dsi.fastutil.objects.Object2IntMap;
import se.kth.speech.coin.tangrams.wac.data.Session;

/**
 * @author <a href="mailto:tcshore@kth.se">Todd Shore</a>
 * @since Dec 1, 2017
 *
 */
public final class TfIdfKeywordVisualizationRowFactory<V, R> implements
		Function<Map<String, Map<VisualizableReferent, Object2IntMap<List<String>>>>, Stream<ReferentNGramRowGrouping<V, R>>> {

	public interface NGramRowFactory<R> {
		R apply(final List<String> ngram, final int count, final double score);
	}

	private class DocumentTfIdfCalculator implements ToDoubleFunction<List<String>> {

		private final Entry<String, VisualizableReferent> doc;

		private DocumentTfIdfCalculator(final Entry<String, VisualizableReferent> doc) {
			this.doc = doc;
		}

		@Override
		public double applyAsDouble(final List<String> ngram) {
			final double score = tfIdfCalculator.applyAsDouble(ngram, doc);
			final int ngramOrder = ngram.size();
			final double normalizer = ngramOrder + Math.log10(ngramOrder);
			return score * normalizer;
		}
	}

	private final long nbestNgrams;

	private final long nbestRefs;

	private final Function<? super VisualizableReferent, ? extends V> refVisualizationFactory;

	private final NGramRowFactory<? extends R> rowFactory;

	private final TfIdfCalculator<List<String>, Entry<String, VisualizableReferent>> tfIdfCalculator;

	public TfIdfKeywordVisualizationRowFactory(
			final TfIdfCalculator<List<String>, Entry<String, VisualizableReferent>> tfIdfCalculator,
			final long nbestRefs, final long nbestNgrams,
			final Function<? super VisualizableReferent, ? extends V> refVisualizationFactory,
			final NGramRowFactory<? extends R> rowFactory) {
		this.tfIdfCalculator = tfIdfCalculator;
		this.nbestRefs = nbestRefs;
		this.nbestNgrams = nbestNgrams;
		this.refVisualizationFactory = refVisualizationFactory;
		this.rowFactory = rowFactory;
	}

	@Override
	public Stream<ReferentNGramRowGrouping<V, R>> apply(
			final Map<String, Map<VisualizableReferent, Object2IntMap<List<String>>>> sessionNgramCounts) {
		final Stream<Entry<String, Map<VisualizableReferent, Object2IntMap<List<String>>>>> sortedSessionRefNgramCounts = sessionNgramCounts
				.entrySet().stream().sorted(Comparator.comparing(entry -> entry.getKey(), Session.getNameComparator()));
		return sortedSessionRefNgramCounts.flatMap(sessionRefNgramCounts -> {
			final String sessionName = sessionRefNgramCounts.getKey();
			// After having sorted by session name, sort by the a given referent's total score
			final Map<VisualizableReferent, Object2IntMap<List<String>>> refNgramCounts = sessionRefNgramCounts
					.getValue();
			final ToDoubleFunction<Entry<VisualizableReferent, Object2IntMap<List<String>>>> inverseNgramCountScorer = entry -> -calculateNgramScores(
					entry.getValue().keySet(), Pair.of(sessionName, entry.getKey())).average()
							.orElse(Double.NEGATIVE_INFINITY);
			final Comparator<Entry<VisualizableReferent, Object2IntMap<List<String>>>> ngramScoreComparator = Comparator
					.comparingDouble(inverseNgramCountScorer);
			final Stream<Entry<VisualizableReferent, Object2IntMap<List<String>>>> nbestRefNgramCounts = refNgramCounts
					.entrySet().stream().sorted(ngramScoreComparator).limit(nbestRefs);
			return nbestRefNgramCounts.map(entry -> createRows(sessionName, entry));
		});
	}

	private DoubleStream calculateNgramScores(final Collection<List<String>> ngrams,
			final Entry<String, VisualizableReferent> sessionRef) {
		final DocumentTfIdfCalculator sessionNgramScorer = new DocumentTfIdfCalculator(sessionRef);
		return ngrams.stream().mapToDouble(ngram -> {
			return sessionNgramScorer.applyAsDouble(ngram);
		});
	}

	private ReferentNGramRowGrouping<V, R> createRows(final String sessionName,
			final Entry<VisualizableReferent, Object2IntMap<List<String>>> refNgramCounts) {
		final VisualizableReferent ref = refNgramCounts.getKey();
		final V refViz = refVisualizationFactory.apply(ref);

		// Create rows only for the n-best n-grams for the given
		// referent
		final DocumentTfIdfCalculator ngramScorer = new DocumentTfIdfCalculator(Pair.of(sessionName, ref));
		final Comparator<Object2IntMap.Entry<List<String>>> nbestNgramCountComparator = Comparator
				.comparingDouble(ngramCount -> -ngramScorer.applyAsDouble(ngramCount.getKey()));
		final Stream<Object2IntMap.Entry<List<String>>> nbestNgramCounts = refNgramCounts.getValue().object2IntEntrySet()
				.stream().sorted(nbestNgramCountComparator).limit(nbestNgrams);
		final Stream<R> rows = nbestNgramCounts.map(ngramCount -> {
			final List<String> ngram = ngramCount.getKey();
			final int count = ngramCount.getIntValue();
			return rowFactory.apply(ngram, count, ngramScorer.applyAsDouble(ngram));
		});
		return new ReferentNGramRowGrouping<>(sessionName, refViz, rows);
	}

}
