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

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang3.tuple.Pair;

import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import se.kth.speech.HashedCollections;
import se.kth.speech.coin.tangrams.wac.data.Referent;
import se.kth.speech.coin.tangrams.wac.data.Round;
import se.kth.speech.coin.tangrams.wac.data.Session;
import se.kth.speech.coin.tangrams.wac.data.Utterance;
import se.kth.speech.nlp.DocumentObservationData;

/**
 * @author <a href="mailto:errantlinguist+github@gmail.com">Todd Shore</a>
 * @since 5 Jan 2018
 *
 */
final class SessionReferentNgramDataManager {

	private static final int EST_UNIQUE_REFS_PER_SESSION = 50;

	private static Stream<Referent> getTargetRefs(final Round round) {
		return round.getReferents().stream().filter(Referent::isTarget);
	}

	private static <K> void incrementCount(final K key, final Object2IntMap<? super K> counts) {
		final int oldValue = counts.getInt(key);
		final int oldValue2 = counts.put(key, oldValue + 1);
		assert oldValue == oldValue2;
	}

	/**
	 * Creates a map of <code>docGroup</code>&mdash;<code>doc</code> pairs to
	 * the observation counts for the given pair.
	 *
	 * @param groupDocObsCounts
	 *            A mapping of <code>docGroup</code> objects, each of which is
	 *            mapped to a mapping of <code>doc</code> &rarr;
	 *            <code>observationCounts</code> maps.
	 * @return A new {@link Map} of <code>docGroup</code>&mdash;<code>doc</code>
	 *         pairs mapped to a map of observation counts for the given pair.
	 */
	static <G, O> Map<Entry<G, VisualizableReferent>, DocumentObservationData<O>> createGroupDocumentPairObservationCountMap(
			final Map<? extends G, Map<VisualizableReferent, DocumentObservationData<O>>> groupDocObsCounts) {
		final Map<Entry<G, VisualizableReferent>, DocumentObservationData<O>> result = new HashMap<>();
		groupDocObsCounts.forEach((docGrouping, docObsCounts) -> {
			docObsCounts.forEach((doc, ngramCounts) -> {
				final Entry<G, VisualizableReferent> pair = Pair.of(docGrouping, doc);
				result.put(pair, ngramCounts);
			});
		});
		assert result.size() >= groupDocObsCounts.size();
		return result;
	}

	static Map<Referent, VisualizableReferent> createVisualizableReferentMap(final Collection<Session> sessions) {
		final Stream<Referent> refs = sessions.stream().map(Session::getRounds).flatMap(List::stream)
				.flatMap(SessionReferentNgramDataManager::getTargetRefs);
		return refs.distinct().collect(Collectors.toMap(Function.identity(), VisualizableReferent::fetch));
	}

	private final Function<? super List<String>, ? extends List<? extends List<String>>> ngramFactory;

	private final Predicate<Utterance> uttFilter;

	SessionReferentNgramDataManager(
			final Function<? super List<String>, ? extends List<? extends List<String>>> ngramFactory,
			final boolean onlyInstructor) {
		this.ngramFactory = ngramFactory;
		uttFilter = onlyInstructor ? Utterance::isInstructor : ref -> true;
	}

	private Object2IntMap<List<String>> createNgramCountMap(final Round round) {
		@SuppressWarnings("unchecked")
		final List<List<String>> ngrams = Arrays.asList(createNgrams(round).toArray(List[]::new));
		final Object2IntOpenHashMap<List<String>> result = new Object2IntOpenHashMap<>(ngrams.size());
		ngrams.forEach(ngram -> incrementCount(ngram, result));
		result.trim();
		return result;
	}

	private Stream<List<String>> createNgrams(final Round round) {
		final Stream<Utterance> utts = round.getUtts().stream();
		final Stream<Utterance> filteredUtts = utts.filter(uttFilter);
		final Stream<List<String>> uttTokenSeqs = filteredUtts.map(Utterance::getReferringTokens);
		return uttTokenSeqs.map(ngramFactory).flatMap(List::stream);
	}

	private Stream<List<String>> createNgrams(final Session session) {
		return session.getRounds().stream().flatMap(this::createNgrams);
	}

	Map<Session, DocumentObservationData<List<String>>> createSessionNgramCountMap(final Collection<Session> sessions) {
		final Map<Session, DocumentObservationData<List<String>>> result = new HashMap<>(
				HashedCollections.capacity(sessions.size()));
		sessions.forEach(session -> {
			final DocumentObservationData<List<String>> ngramCounts = result.computeIfAbsent(session,
					key -> new DocumentObservationData<>());
			ngramCounts.incrementDocumentOccurrenceCount();
			createNgrams(session).forEach(ngram -> ngramCounts.incrementObservationCount(ngram));
		});
		return result;
	}

	Map<Session, Map<VisualizableReferent, DocumentObservationData<List<String>>>> createSessionReferentNgramCountMap(
			final Collection<Session> sessions, final Map<Referent, VisualizableReferent> vizRefs) {
		final int refNgramCountMapInitialCapacity = HashedCollections.capacity(EST_UNIQUE_REFS_PER_SESSION);
		final Map<Session, Map<VisualizableReferent, DocumentObservationData<List<String>>>> result = sessions.stream()
				.collect(Collectors.toMap(Function.identity(),
						session -> new HashMap<>(refNgramCountMapInitialCapacity)));

		sessions.forEach(session -> {
			final Map<VisualizableReferent, DocumentObservationData<List<String>>> refNgramCounts = result
					.get(session);

			session.getRounds().forEach(round -> {
				// Use the same n-gram list for each referent
				final Object2IntMap<List<String>> ngramCounts = createNgramCountMap(round);

				getTargetRefs(round).map(vizRefs::get).forEach(vizRef -> {
					final DocumentObservationData<List<String>> extantNgramCounts = refNgramCounts
							.computeIfAbsent(vizRef, key -> new DocumentObservationData<>());
					extantNgramCounts.incrementDocumentOccurrenceCount();
					extantNgramCounts.addObservationCounts(ngramCounts);
				});
			});
		});
		return result;
	}

}
