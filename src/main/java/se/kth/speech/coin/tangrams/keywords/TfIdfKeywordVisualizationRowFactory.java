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

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Function;
import java.util.function.ToDoubleBiFunction;
import java.util.function.ToDoubleFunction;
import java.util.stream.DoubleStream;
import java.util.stream.Stream;

import org.apache.commons.lang3.tuple.Pair;

import it.unimi.dsi.fastutil.objects.Object2IntMap;
import se.kth.speech.coin.tangrams.wac.data.Session;
import se.kth.speech.nlp.DocumentObservationData;

/**
 * @author <a href="mailto:errantlinguist+github@gmail.com">Todd Shore</a>
 * @since Dec 1, 2017
 *
 */
public final class TfIdfKeywordVisualizationRowFactory<V, R> implements
		Function<Map<Entry<Session, VisualizableReferent>, DocumentObservationData<List<String>>>, Stream<ReferentNGramRowGrouping<V, R>>> {

	public interface NGramRowFactory<R> {
		R apply(final List<String> ngram, final int count, final double score);
	}

	private class DocumentObservationScorer implements ToDoubleFunction<List<String>> {

		private final Entry<Session, VisualizableReferent> doc;

		private DocumentObservationScorer(final Entry<Session, VisualizableReferent> doc) {
			this.doc = doc;
		}

		@Override
		public double applyAsDouble(final List<String> obs) {
			return tfIdfScorer.applyAsDouble(obs, doc);
		}
	}

	private class NormalizedDocumentObservationScorer implements ToDoubleFunction<List<String>> {

		private final DocumentObservationScorer docObsScorer;

		private final double normalizer;

		private NormalizedDocumentObservationScorer(final Entry<Session, VisualizableReferent> doc,
				final DocumentObservationData<List<String>> docObsData) {
			// Normalize the score of the observation (n-gram) by the number of times the
			// document (referent) occurs in the data
			this(doc, createNormalizer(docObsData.getDocumentOccurrenceCount()));
		}

		private NormalizedDocumentObservationScorer(final Entry<Session, VisualizableReferent> doc,
				final double normalizer) {
			docObsScorer = new DocumentObservationScorer(doc);
			this.normalizer = normalizer;
		}

		@Override
		public double applyAsDouble(final List<String> obs) {
			final double score = docObsScorer.applyAsDouble(obs);
			return score * normalizer;
		}
	}

	private static final Comparator<Entry<Entry<Session, VisualizableReferent>, DocumentObservationData<List<String>>>> SESSION_DOC_OBS_DATA_NAME_COMPARATOR = Comparator
			.comparing(entry -> entry.getKey().getKey().getName(), Session.getNameComparator());

	private static double createNormalizer(final int docOccurenceCount) {
//		final double product = docOccurenceCount * 0.1;
		return docOccurenceCount + Math.log(docOccurenceCount);
//		return 1.0 / docOccurenceCount;
		
	}

	private final long nbestNgrams;

	private final long nbestRefs;

	private final Function<? super VisualizableReferent, ? extends V> refVisualizationFactory;

	private final NGramRowFactory<? extends R> rowFactory;

	private final ToDoubleBiFunction<? super List<String>, ? super Entry<Session, VisualizableReferent>> tfIdfScorer;

	public TfIdfKeywordVisualizationRowFactory(
			final ToDoubleBiFunction<? super List<String>, ? super Entry<Session, VisualizableReferent>> tfIdfScorer,
			final long nbestRefs, final long nbestNgrams,
			final Function<? super VisualizableReferent, ? extends V> refVisualizationFactory,
			final NGramRowFactory<? extends R> rowFactory) {
		this.tfIdfScorer = tfIdfScorer;
		this.nbestRefs = nbestRefs;
		this.nbestNgrams = nbestNgrams;
		this.refVisualizationFactory = refVisualizationFactory;
		this.rowFactory = rowFactory;
	}

	@Override
	public Stream<ReferentNGramRowGrouping<V, R>> apply(
			final Map<Entry<Session, VisualizableReferent>, DocumentObservationData<List<String>>> sessionDocObsData) {
		final Stream<Entry<Entry<Session, VisualizableReferent>, DocumentObservationData<List<String>>>> nbestSessionRefDocObsData = sessionDocObsData
				.entrySet().stream()
				.sorted(Comparator
						.comparingDouble(sessionRefDocObsData -> -scoreReferentLanguage(sessionRefDocObsData)))
				.limit(nbestRefs);
		// Once the n-best results have been chosen, re-sort by session name
		return nbestSessionRefDocObsData.sorted(SESSION_DOC_OBS_DATA_NAME_COMPARATOR).map(sessionRefDocObsData -> {
			final Entry<Session, VisualizableReferent> sessionRef = sessionRefDocObsData.getKey();
			final DocumentObservationData<List<String>> docObsData = sessionRefDocObsData.getValue();
			return createRows(sessionRef, docObsData);
		});
	}

	private ReferentNGramRowGrouping<V, R> createRows(final Entry<Session, VisualizableReferent> sessionRef,
			final DocumentObservationData<List<String>> docObsData) {
		final Session session = sessionRef.getKey();
		final VisualizableReferent ref = sessionRef.getValue();
		final V refViz = refVisualizationFactory.apply(ref);
		final Object2IntMap<List<String>> obsCounts = docObsData.getObservationCounts();

		// Create rows only for the n-best n-grams for the given
		// referent
		final DocumentObservationScorer ngramScorer = new DocumentObservationScorer(Pair.of(session, ref));
		final Comparator<Object2IntMap.Entry<List<String>>> nbestNgramCountComparator = Comparator.comparingDouble(
				ngramCount -> -ngramScorer.applyAsDouble(ngramCount.getKey()));
		final Stream<Object2IntMap.Entry<List<String>>> nbestDocObsData = obsCounts.object2IntEntrySet().stream()
				.sorted(nbestNgramCountComparator).limit(nbestNgrams);
		final Stream<R> rows = nbestDocObsData.map(ngramCount -> {
			final List<String> ngram = ngramCount.getKey();
			final int count = ngramCount.getIntValue();
			return rowFactory.apply(ngram, count, ngramScorer.applyAsDouble(ngram));
		});
		return new ReferentNGramRowGrouping<>(session, refViz, docObsData.getDocumentOccurrenceCount(), rows);
	}

	private double scoreReferentLanguage(
			final Entry<Entry<Session, VisualizableReferent>, DocumentObservationData<List<String>>> sessionRefDocObsData) {
		return scoreReferentLanguage(sessionRefDocObsData.getKey(), sessionRefDocObsData.getValue());
	}

	private double scoreReferentLanguage(final Entry<Session, VisualizableReferent> sessionRef,
			final DocumentObservationData<List<String>> docObsData) {
		final NormalizedDocumentObservationScorer sessionNgramScorer = new NormalizedDocumentObservationScorer(
				sessionRef, docObsData);
		final DoubleStream obsScores = docObsData.getObservationCounts().keySet().stream()
				.mapToDouble(sessionNgramScorer);
		return obsScores.max().orElse(Double.NEGATIVE_INFINITY);
	}

}
