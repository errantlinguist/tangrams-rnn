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
import java.util.stream.DoubleStream;

import se.kth.speech.HashedCollections;
import se.kth.speech.coin.tangrams.wac.data.Session;

/**
 * @author <a href="mailto:tcshore@kth.se">Todd Shore</a>
 * @since Dec 1, 2017
 *
 */
public final class TfIdfCalculator<T> implements ToDoubleBiFunction<T, Session> {

	/**
	 * @author <a href="mailto:tcshore@kth.se">Todd Shore</a>
	 * @since Dec 3, 2017
	 * @see <a href="https://doi.org/10.1017%2FCBO9780511809071.007">Manning,
	 *      Christopher D. et al (2008). <em>Introduction to Information
	 *      Retrieval</em>, p.&nbsp;128.</a>
	 *
	 */
	public enum TermFrequencyVariant {
		/**
		 * <em>tf(t,d)</em> = 0.5 + 0.5 &sdot; (<em>f</em><sub>t,d</sub>
		 * &divide; max<sub><em>t&prime;</em> &isin; <em>d</em></sub>
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

	private static final int DEFAULT_INITIAL_WORD_MAP_CAPACITY = HashedCollections.capacity(1000);

	public static <T> TfIdfCalculator<T> create(final Map<Session, ? extends Iterable<T>> sessionObservations,
			final boolean onlyInstructor) {
		return create(sessionObservations, onlyInstructor, TermFrequencyVariant.NATURAL);
	}

	public static <T> TfIdfCalculator<T> create(final Map<Session, ? extends Iterable<T>> sessionObservations,
			final boolean onlyInstructor, final TermFrequencyVariant tfVariant) {
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

		return new TfIdfCalculator<>(observationCountsPerSession, observationSessions, sessionObservations.size(),
				tfVariant);
	}

	private final Map<Session, Map<T, Double>> observationCountsPerSession;

	private final Map<T, Set<Session>> observationSessions;

	private final ToDoubleBiFunction<T, Session> tfCalculator;

	private final double totalSessionCount;

	private TfIdfCalculator(final Map<Session, Map<T, Double>> observationCountsPerSession,
			final Map<T, Set<Session>> observationSessions, final int totalSessionCount,
			final TermFrequencyVariant tfVariant) {
		this.observationCountsPerSession = observationCountsPerSession;
		this.observationSessions = observationSessions;
		this.totalSessionCount = totalSessionCount;
		this.tfCalculator = getTermFrequencyCalculator(tfVariant);
	}

	@Override
	public double applyAsDouble(final T observation, final Session session) {
		final double tf = tfCalculator.applyAsDouble(observation, session);
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
	 * @param session
	 *            The {@link Session} during which the given observation
	 *            occurred.
	 * @return The augmented term frequency for the given observation during the
	 *         given session.
	 */
	private double augmentedTf(final T observation, final Session session) {
		final Map<T, Double> sessionWordCounts = observationCountsPerSession.get(session);
		assert sessionWordCounts != null;
		final Double naturalTf = sessionWordCounts.get(observation);
		assert naturalTf != null : String.format("Term frequency for \"%s\" is null for session \"%s\".", observation,
				session.getName());
		final DoubleStream sessionTfs = sessionWordCounts.values().stream().mapToDouble(Double::doubleValue);
		final double maxSessionTf = sessionTfs.max().getAsDouble();
		return 0.5 + 0.5 * (naturalTf / maxSessionTf);
	}

	private double df(final T observation) {
		final Set<Session> sessions = observationSessions.get(observation);
		assert sessions != null : String.format("Session set for \"%s\" is null.", observation);
		final int result = sessions.size();
		assert result > 0 : String.format("Session set for \"%s\" is of size %d.", observation, result);
		return result;
	}

	private ToDoubleBiFunction<T, Session> getTermFrequencyCalculator(final TermFrequencyVariant variant) {
		final ToDoubleBiFunction<T, Session> result;
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

	private double idf(final T observation) {
		final double df = df(observation);
		final double result = Math.log(totalSessionCount / df);
		assert Double.isFinite(result) : String.format("IDF score for \"%s\" is not finite.", observation);
		return result;
	}

	/**
	 * Calculates the term frequency for a given observation in a given
	 * {@link Session} as the raw counts of a given term <em>t</em> in a given
	 * document <em>d</em> <em>tf(t,d)</em> = <em>f</em><sub><em>t,d</em></sub>.
	 *
	 * @param observation
	 *            The observation to calculate the term frequency of.
	 * @param session
	 *            The {@link Session} during which the given observation
	 *            occurred.
	 * @return The natural term frequency for the given observation during the
	 *         given session.
	 */
	private double naturalTf(final T observation, final Session session) {
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
