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
package se.kth.speech.coin.tangrams.wac.data;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import se.kth.speech.HashedCollections;
import se.kth.speech.coin.tangrams.wac.logistic.ModelParameter;

public final class SessionSet {

	private static void addRandomIntegers(final Collection<? super Integer> idxs, final int resultSize, final int bound,
			final Random random) {
		while (idxs.size() < resultSize) {
			final int nextIdx = random.nextInt(bound);
			idxs.add(nextIdx);
		}
	}

	private final List<Session> sessions;

	public SessionSet(final List<Session> sessions) { // NO_UCD (use default)
		this.sessions = sessions;
	}

	public SessionSet(final Session session) {
		this(Collections.singletonList(session));
	}

	public void crossValidate(final BiConsumer<SessionSet, SessionSet> consumer,
			final Map<ModelParameter, Object> modelParams, final Random random) {
		final int trainingSetSizeDiscount = (Integer) modelParams.get(ModelParameter.TRAINING_SET_SIZE_DISCOUNT);
		final ListIterator<Session> testSessionIter = sessions.listIterator();
		while (testSessionIter.hasNext()) {
			final int testSessionIdx = testSessionIter.nextIndex();
			final Session testSession = testSessionIter.next();
			final List<Session> trainingSessions = createTrainingSessionList(testSessionIdx, trainingSetSizeDiscount,
					random);
			consumer.accept(new SessionSet(trainingSessions), new SessionSet(testSession));
		}
	}

	public void crossValidate(final BiConsumer<SessionSet, SessionSet> consumer,
			final Map<ModelParameter, Object> modelParams, final Random random,
			final Predicate<? super Session> testSessionMatcher) {
		final int trainingSetSizeDiscount = (Integer) modelParams.get(ModelParameter.TRAINING_SET_SIZE_DISCOUNT);
		final Map<Boolean, List<Session>> testTrainingSessions = sessions.stream()
				.collect(Collectors.partitioningBy(testSessionMatcher));
		final List<Session> trainingSessions = testTrainingSessions.get(Boolean.FALSE);
		final List<Session> testSessions = testTrainingSessions.get(Boolean.TRUE);
		final int trainingSetSize = trainingSessions.size() - trainingSetSizeDiscount;
		if (trainingSetSize <= 0) {
			throw new IllegalArgumentException(String.format(
					"Cannot use a training set discount of %d with a test set of size %d and a total of %d possible training sessions.",
					trainingSetSizeDiscount, testSessions.size(), trainingSessions.size()));
		} else {
			while (trainingSessions.size() > trainingSetSize) {
				final int nextIdx = random.nextInt(trainingSessions.size());
				trainingSessions.remove(nextIdx);
			}
			consumer.accept(new SessionSet(trainingSessions), new SessionSet(testSessions));
		}
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(final Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (!(obj instanceof SessionSet)) {
			return false;
		}
		final SessionSet other = (SessionSet) obj;
		if (sessions == null) {
			if (other.sessions != null) {
				return false;
			}
		} else if (!sessions.equals(other.sessions)) {
			return false;
		}
		return true;
	}

	/**
	 * @return the sessions
	 */
	public Collection<Session> getSessions() {
		return sessions;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + (sessions == null ? 0 : sessions.hashCode());
		return result;
	}

	public int size() {
		return sessions.size();
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		final StringBuilder builder = new StringBuilder(64 * (sessions.size() + 1));
		builder.append("SessionSet [sessions=");
		builder.append(sessions);
		builder.append("]");
		return builder.toString();
	}

	private List<Session> createTrainingSessionList(final int testSessionIdx, final int trainingSetSizeDiscount,
			final Random random) {
		final int idxsToRemoveCount = trainingSetSizeDiscount + 1;
		final int resultSize = sessions.size() - idxsToRemoveCount;
		if (resultSize < 1) {
			throw new IllegalArgumentException(
					String.format("Tried to discount training set size by %d but there are only %s sessions.",
							trainingSetSizeDiscount, sessions.size()));
		}

		final Set<Integer> idxsToRemove = new HashSet<>(HashedCollections.capacity(idxsToRemoveCount));
		idxsToRemove.add(testSessionIdx);
		addRandomIntegers(idxsToRemove, idxsToRemoveCount, sessions.size(), random);
		assert idxsToRemove.size() == idxsToRemoveCount;

		final List<Session> result = new ArrayList<>(resultSize);
		final ListIterator<Session> trainingSessionIter = sessions.listIterator();
		while (trainingSessionIter.hasNext()) {
			final int trainingSessionIdx = trainingSessionIter.nextIndex();
			final Session trainingSession = trainingSessionIter.next();
			if (idxsToRemove.contains(trainingSessionIdx)) {
				// Do nothing
			} else {
				result.add(trainingSession);
			}
		}
		assert result.size() == resultSize;
		return result;
	}

}
