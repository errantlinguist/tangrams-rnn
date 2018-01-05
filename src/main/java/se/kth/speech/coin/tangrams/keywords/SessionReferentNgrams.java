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

/**
 * @author <a href="mailto:tcshore@kth.se">Todd Shore</a>
 * @since 5 Jan 2018
 *
 */
final class SessionReferentNgrams {

	private static final int EST_UNIQUE_REFS_PER_SESSION = 50;

	private static Object2IntMap<List<String>> createNgramCountMap(final Round round, final NGramFactory ngramFactory,
			final boolean onlyInstructor) {
		@SuppressWarnings("unchecked")
		final List<List<String>> ngrams = Arrays
				.asList(createNgrams(round, ngramFactory, onlyInstructor).toArray(List[]::new));
		final Object2IntOpenHashMap<List<String>> result = new Object2IntOpenHashMap<List<String>>(ngrams.size());
		ngrams.forEach(ngram -> incrementCount(ngram, result));
		result.trim();
		return result;
	}

	private static Stream<List<String>> createNgrams(final Round round, final NGramFactory ngramFactory,
			final boolean onlyInstructor) {
		final Stream<Utterance> utts = round.getUtts().stream();
		final Stream<Utterance> instructorUtts = utts.filter(Utterance::isInstructor);
		final Stream<List<String>> uttTokenSeqs = instructorUtts.map(Utterance::getReferringTokens);
		return uttTokenSeqs.map(ngramFactory).flatMap(List::stream);
	}

	private static Stream<Referent> getTargetRefs(final Round round) {
		return round.getReferents().stream().filter(Referent::isTarget);
	}

	private static <K> void incrementCount(final K key, final Object2IntMap<? super K> counts) {
		final int oldValue = counts.getInt(key);
		final int oldValue2 = counts.put(key, oldValue + 1);
		assert oldValue == oldValue2;
	}

	static Map<String, Map<VisualizableReferent, Object2IntMap<List<String>>>> createSessionReferentNgramCountMap(
			final Collection<Session> sessions, final Map<Referent, VisualizableReferent> vizRefs,
			final NGramFactory ngramFactory, final boolean onlyInstructor) {
		final Map<String, Map<VisualizableReferent, Object2IntMap<List<String>>>> result = new HashMap<>(
				sessions.size());
		sessions.forEach(session -> {
			final String sessionName = session.getName();
			final Map<VisualizableReferent, Object2IntMap<List<String>>> refNgramCounts = result.computeIfAbsent(
					sessionName, key -> new HashMap<>(HashedCollections.capacity(EST_UNIQUE_REFS_PER_SESSION)));

			session.getRounds().forEach(round -> {
				// Use the same n-gram list for each referent
				final Object2IntMap<List<String>> ngramCounts = createNgramCountMap(round, ngramFactory,
						onlyInstructor);

				getTargetRefs(round).map(vizRefs::get).forEach(vizRef -> {
					final Object2IntMap<List<String>> oldTableValue = refNgramCounts.put(vizRef, ngramCounts);
					assert oldTableValue == null;
				});
			});
		});
		return result;
	}

	static Map<Entry<String, VisualizableReferent>, Object2IntMap<List<String>>> createSessionReferentPairNgramCountMap(
			final Map<String, Map<VisualizableReferent, Object2IntMap<List<String>>>> sessionRefNgramCounts) {
		final Map<Entry<String, VisualizableReferent>, Object2IntMap<List<String>>> result = new HashMap<>(
				HashedCollections.capacity(sessionRefNgramCounts.size() * EST_UNIQUE_REFS_PER_SESSION));
		sessionRefNgramCounts.forEach((sessionName, refNgramCounts) -> {
			refNgramCounts.forEach((ref, ngramCounts) -> {
				final Entry<String, VisualizableReferent> pair = Pair.of(sessionName, ref);
				result.put(pair, ngramCounts);
			});
		});
		assert result.size() >= sessionRefNgramCounts.size();
		return result;
	}

	static Map<Referent, VisualizableReferent> createVisualizableReferentMap(
			final Collection<Session> sessions) {
		final Stream<Referent> refs = sessions.stream().map(Session::getRounds).flatMap(List::stream)
				.flatMap(SessionReferentNgrams::getTargetRefs);
		return refs.distinct().collect(Collectors.toMap(Function.identity(), VisualizableReferent::fetch));
	}

	private SessionReferentNgrams() {
	}

}
