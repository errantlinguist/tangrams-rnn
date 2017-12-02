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

import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.function.ToDoubleBiFunction;

import se.kth.speech.HashedCollections;
import se.kth.speech.coin.tangrams.wac.data.Session;

/**
 * @author <a href="mailto:tcshore@kth.se">Todd Shore</a>
 * @since Dec 1, 2017
 *
 */
public final class TfIdfCalculator<T> implements ToDoubleBiFunction<T, Session> {

	private static final int DEFAULT_INITIAL_WORD_MAP_CAPACITY = HashedCollections.capacity(1000);

	public static <T> TfIdfCalculator<T> create(final Map<Session, ? extends Iterable<T>> sessionObservations,
			final boolean onlyInstructor) {
		final int initialMapCapcity = HashedCollections.capacity(sessionObservations.size());
		final Map<Session, Map<T, Double>> observationCountsPerSession = new IdentityHashMap<>(initialMapCapcity);
		final Map<T, Set<Session>> observationSessions = new HashMap<>(DEFAULT_INITIAL_WORD_MAP_CAPACITY);
		for (final Entry<Session, ? extends Iterable<T>> entry : sessionObservations.entrySet()) {
			final Session session = entry.getKey();
			final Map<T, Double> sessionTokenCounts = observationCountsPerSession.computeIfAbsent(session,
					key -> new HashMap<>(DEFAULT_INITIAL_WORD_MAP_CAPACITY));
			final Iterable<T> observations = entry.getValue();
			observations.forEach(observation -> {
				sessionTokenCounts.compute(observation, (key, oldValue) -> oldValue == null ? 1 : oldValue + 1);
				observationSessions.computeIfAbsent(observation, key -> new HashSet<>(initialMapCapcity)).add(session);
			});
		}

		return new TfIdfCalculator<>(observationCountsPerSession, observationSessions, sessionObservations.size());
	}

	private final Map<Session, Map<T, Double>> observationCountsPerSession;

	private final Map<T, Set<Session>> observationSessions;

	private final double totalSessionCount;

	private TfIdfCalculator(final Map<Session, Map<T, Double>> observationCountsPerSession,
			final Map<T, Set<Session>> observationSessions, final int totalSessionCount) {
		this.observationCountsPerSession = observationCountsPerSession;
		this.observationSessions = observationSessions;
		this.totalSessionCount = totalSessionCount;
	}

	@Override
	public double applyAsDouble(final T observation, final Session session) {
		final double tf = tf(observation, session);
		final double idf = idf(observation);
		return tf * idf;
	}

	private double df(final T observation) {
		final Set<Session> sessions = observationSessions.get(observation);
		assert sessions != null : String.format("Session set for \"%s\" is null.", observation);
		final int result = sessions.size();
		assert result > 0 : String.format("Session set for \"%s\" is of size %d.", observation, result);
		return result;
	}

	private double idf(final T observation) {
		final double df = df(observation);
		final double result = Math.log(totalSessionCount / df);
		assert Double.isFinite(result) : String.format("IDF score for \"%s\" is not finite.", observation);
		return result;
	}

	private double tf(final T observation, final Session session) {
		final Map<T, Double> sessionWordCounts = observationCountsPerSession.get(session);
		assert sessionWordCounts != null;
		final Double result = sessionWordCounts.get(observation);
		assert result != null : String.format("Term frequency for \"%s\" is null for session \"%s\".", observation,
				session.getName());
		assert Double.isFinite(result) : String.format("Term frequency for \"%s\" is not finite for session \"%s\".",
				observation, session.getName());
		return result;
	}
}
