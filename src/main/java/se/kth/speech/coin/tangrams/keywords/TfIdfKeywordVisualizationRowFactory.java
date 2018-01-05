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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.function.Function;
import java.util.function.ToDoubleFunction;
import java.util.stream.DoubleStream;
import java.util.stream.Stream;

import org.apache.commons.lang3.tuple.Pair;
import org.w3c.dom.Node;
import org.w3c.dom.svg.SVGDocument;
import org.w3c.dom.svg.SVGSVGElement;

import it.unimi.dsi.fastutil.objects.Object2IntMap;

/**
 * @author <a href="mailto:tcshore@kth.se">Todd Shore</a>
 * @since Dec 1, 2017
 *
 */
public final class TfIdfKeywordVisualizationRowFactory<R>
		implements Function<Map<String, Map<VisualizableReferent, Object2IntMap<List<String>>>>, Stream<R>> {

	public interface RowFactory<R> {
		R apply(final String dyadId, final Node refSvgRootElem, final List<String> ngram, final int count,
				final double score);
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

	private static final Comparator<String> SESSION_NAME_COMPARATOR = new Comparator<String>() {

		@Override
		public int compare(final String o1, final String o2) {
			int result = 0;
			try {
				final double n1 = Double.parseDouble(o1);
				try {
					final double n2 = Double.parseDouble(o2);
					// Both strings are numeric; Compare as doubles
					result = Double.compare(n1, n2);
				} catch (final NumberFormatException e2) {
					// The first string is numeric but the second isn't
					result = -1;
				}
			} catch (final NumberFormatException e1) {
				try {
					Double.parseDouble(o2);
					// The second string is numeric but the first isn't
					result = 1;
				} catch (final NumberFormatException e2) {
					// Neither string is numeric; Compare as strings
					result = o1.compareTo(o2);
				}
			}
			return result;
		}

	};

	private final long nbestNgrams;

	private final long nbestRefs;

	private final RowFactory<R> rowFactory;

	private final TfIdfCalculator<List<String>, Entry<String, VisualizableReferent>> tfIdfCalculator;

	private final Function<? super VisualizableReferent, ? extends SVGDocument> svgDocFactory;

	public TfIdfKeywordVisualizationRowFactory(
			final Function<? super VisualizableReferent, ? extends SVGDocument> svgDocFactory,
			final TfIdfCalculator<List<String>, Entry<String, VisualizableReferent>> tfIdfCalculator,
			final long nbestRefs, final long nbestNgrams, final RowFactory<R> rowFactory) {
		this.svgDocFactory = svgDocFactory;
		this.tfIdfCalculator = tfIdfCalculator;
		this.nbestRefs = nbestRefs;
		this.nbestNgrams = nbestNgrams;
		this.rowFactory = rowFactory;
	}

	@Override
	public Stream<R> apply(
			final Map<String, Map<VisualizableReferent, Object2IntMap<List<String>>>> sessionNgramCounts) {
		final Stream<VisualizableReferent> distinctRefs = sessionNgramCounts.values().stream().map(Map::keySet)
				.flatMap(Set::stream).distinct();
		final Map<VisualizableReferent, SVGSVGElement> refSvgRootElems = createRefSVGRootElementMap(distinctRefs);

		final Stream<Entry<String, Map<VisualizableReferent, Object2IntMap<List<String>>>>> sortedSessionRefNgramCounts = sessionNgramCounts
				.entrySet().stream().sorted(Comparator.comparing(entry -> entry.getKey(), SESSION_NAME_COMPARATOR));
		return sortedSessionRefNgramCounts.flatMap(sessionRefNgramCounts -> {
			final String sessionName = sessionRefNgramCounts.getKey();
			// After having sorted by session name, sort by the score of a given
			// referent's total score
			final Map<VisualizableReferent, Object2IntMap<List<String>>> refNgramCounts = sessionRefNgramCounts
					.getValue();
			final ToDoubleFunction<Entry<VisualizableReferent, Object2IntMap<List<String>>>> inverseNgramCountScorer = entry -> -calculateNgramScores(
					entry.getValue().keySet(), Pair.of(sessionName, entry.getKey())).average()
							.orElse(Double.NEGATIVE_INFINITY);
			final Comparator<Entry<VisualizableReferent, Object2IntMap<List<String>>>> ngramScoreComparator = Comparator
					.comparingDouble(inverseNgramCountScorer);
			final Stream<Entry<VisualizableReferent, Object2IntMap<List<String>>>> nbestRefNgramCounts = refNgramCounts
					.entrySet().stream().sorted(ngramScoreComparator).limit(nbestRefs);
			return nbestRefNgramCounts.flatMap(entry -> {
				final VisualizableReferent ref = entry.getKey();
				final SVGSVGElement refSvgRootElem = refSvgRootElems.get(ref);

				// Create rows only for the n-best n-grams for the given
				// referent
				final DocumentTfIdfCalculator ngramScorer = new DocumentTfIdfCalculator(Pair.of(sessionName, ref));
				final Comparator<Object2IntMap.Entry<List<String>>> nbestNgramCountComparator = Comparator
						.comparingDouble(ngramCount -> -ngramScorer.applyAsDouble(ngramCount.getKey()));
				final Stream<Object2IntMap.Entry<List<String>>> nbestNgramCounts = entry.getValue().object2IntEntrySet()
						.stream().sorted(nbestNgramCountComparator).limit(nbestNgrams);
				return nbestNgramCounts.map(ngramCount -> {
					final List<String> ngram = ngramCount.getKey();
					final int count = ngramCount.getIntValue();
					return rowFactory.apply(sessionName, refSvgRootElem, ngram, count,
							ngramScorer.applyAsDouble(ngram));
				});
			});
		});
	}

	private DoubleStream calculateNgramScores(final Collection<List<String>> ngrams,
			final Entry<String, VisualizableReferent> sessionRef) {
		final DocumentTfIdfCalculator sessionNgramScorer = new DocumentTfIdfCalculator(sessionRef);
		return ngrams.stream().mapToDouble(ngram -> {
			return sessionNgramScorer.applyAsDouble(ngram);
		});
	}

	private Map<VisualizableReferent, SVGSVGElement> createRefSVGRootElementMap(
			final Stream<VisualizableReferent> distinctRefs) {
		final Map<VisualizableReferent, SVGSVGElement> result = new HashMap<>();
		// int nextDocId = 1;
		distinctRefs.forEach(ref -> {
			final SVGDocument doc = svgDocFactory.apply(ref);
			final SVGSVGElement rootElem = doc.getRootElement();
			// rootElem.setId("svg-" + Integer.toString(nextDocId++));
			result.put(ref, rootElem);
		});
		return result;
	}

}
