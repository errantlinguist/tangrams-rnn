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
import java.util.OptionalDouble;
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
		Function<Map<Session, Map<VisualizableReferent, DocumentObservationData<List<String>>>>, Stream<ReferentNGramRowGrouping<V, R>>> {

	public interface NGramRowFactory<R> {
		R apply(final List<String> ngram, final int count, final double score);
	}

	private class DocumentTfIdfScorer implements ToDoubleFunction<List<String>> {

		private final Entry<Session, VisualizableReferent> doc;

		private DocumentTfIdfScorer(final Entry<Session, VisualizableReferent> doc) {
			this.doc = doc;
		}

		@Override
		public double applyAsDouble(final List<String> ngram) {
			final double score = tfIdfScorer.applyAsDouble(ngram, doc);
			// final int ngramOrder = ngram.size();
			// final double normalizer = ngramOrder + Math.log10(ngramOrder);
			// return score * normalizer;
			return score;
		}
	}

	private static final Comparator<Entry<Session, Map<VisualizableReferent, DocumentObservationData<List<String>>>>> SESSION_DOC_OBS_DATA_NAME_COMPARATOR = Comparator
			.comparing(entry -> entry.getKey().getName(), Session.getNameComparator());

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
			final Map<Session, Map<VisualizableReferent, DocumentObservationData<List<String>>>> sessionDocObsData) {
		final Stream<Entry<Session, Map<VisualizableReferent, DocumentObservationData<List<String>>>>> sortedSessionRefDocObsData = sessionDocObsData
				.entrySet().stream().sorted(SESSION_DOC_OBS_DATA_NAME_COMPARATOR);
		return sortedSessionRefDocObsData.flatMap(sessionRefDocObsData -> {
			final Session session = sessionRefDocObsData.getKey();
			// After having sorted by session name, sort by a given referent's total score
			final Map<VisualizableReferent, DocumentObservationData<List<String>>> refDocObsData = sessionRefDocObsData
					.getValue();
			final ToDoubleFunction<Entry<VisualizableReferent, DocumentObservationData<List<String>>>> inverseNgramCountScorer = counts -> -scoreReferentLanguage(
					session, counts);
			final Comparator<Entry<VisualizableReferent, DocumentObservationData<List<String>>>> ngramScoreComparator = Comparator
					.comparingDouble(inverseNgramCountScorer);
			final Stream<Entry<VisualizableReferent, DocumentObservationData<List<String>>>> nbestRefDocObsData = refDocObsData
					.entrySet().stream().sorted(ngramScoreComparator).limit(nbestRefs);
			return nbestRefDocObsData.map(entry -> createRows(session, entry));
		});
	}

	private ReferentNGramRowGrouping<V, R> createRows(final Session session,
			final Entry<VisualizableReferent, DocumentObservationData<List<String>>> refDocObsData) {
		final VisualizableReferent ref = refDocObsData.getKey();
		final V refViz = refVisualizationFactory.apply(ref);
		final DocumentObservationData<List<String>> docObsData = refDocObsData.getValue();
		final Object2IntMap<List<String>> obsCounts = docObsData.getObservationCounts();

		// Create rows only for the n-best n-grams for the given
		// referent
		final DocumentTfIdfScorer ngramScorer = new DocumentTfIdfScorer(Pair.of(session, ref));
		final Comparator<Object2IntMap.Entry<List<String>>> nbestNgramCountComparator = Comparator.comparingDouble(
				ngramCount -> -ngramScorer.applyAsDouble(ngramCount.getKey()) * ngramCount.getIntValue());
		final Stream<Object2IntMap.Entry<List<String>>> nbestDocObsData = obsCounts.object2IntEntrySet().stream()
				.sorted(nbestNgramCountComparator).limit(nbestNgrams);
		final Stream<R> rows = nbestDocObsData.map(ngramCount -> {
			final List<String> ngram = ngramCount.getKey();
			final int count = ngramCount.getIntValue();
			return rowFactory.apply(ngram, count, ngramScorer.applyAsDouble(ngram));
		});
		return new ReferentNGramRowGrouping<>(session, refViz, docObsData.getDocumentOccurrenceCount(), rows);
	}

	private double scoreReferentLanguage(final Session session,
			final Entry<VisualizableReferent, DocumentObservationData<List<String>>> refDocObsData) {
		final Entry<Session, VisualizableReferent> sessionRef = Pair.of(session, refDocObsData.getKey());
		final DocumentTfIdfScorer sessionNgramScorer = new DocumentTfIdfScorer(sessionRef);
		final DocumentObservationData<List<String>> docObsData = refDocObsData.getValue();
		final DoubleStream obsScores = docObsData.getObservationCounts().keySet().stream()
				.mapToDouble(sessionNgramScorer);

		final OptionalDouble maxScore = obsScores.max();
		final double result;
		if (maxScore.isPresent()) {
			// Normalize the score of the observation (n-gram) by the number of times the
			// document (referent) occurs in the data
			result = maxScore.getAsDouble() / docObsData.getDocumentOccurrenceCount();
		} else {
			result = Double.NEGATIVE_INFINITY;
		}
		return result;
	}

}
