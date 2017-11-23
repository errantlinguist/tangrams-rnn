/*******************************************************************************
 * Copyright 2017 Todd Shore
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License.  You may obtain a copy
 * of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations under
 * the License.
 ******************************************************************************/
package se.kth.speech.coin.tangrams.wac.data;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.BiConsumer;

public final class SessionSet {

	private final List<Session> sessions;

	public SessionSet(final List<Session> sessions) {
		this.sessions = sessions;
	}

	public SessionSet(final Session session) {
		this(Collections.singletonList(session));
	}

	public SessionSet(final SessionSet toCopy) {
		this(new ArrayList<>(toCopy.sessions));
	}

	public void crossValidate(final BiConsumer<SessionSet, Session> consumer) {
		for (int i = 0; i < sessions.size(); i++) {
			final SessionSet training = new SessionSet(this);
			final Session testing = training.sessions.remove(i);
			// System.out.println("Testing on " + testing.sessions.get(0).name);
			consumer.accept(training, testing);
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
	public List<Session> getSessions() {
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

	public void printNegatives() {
		for (final Session sess : sessions) {
			for (final Round round : sess.getRounds()) {
				if (round.isNegative()) {
					System.out.println(round.prettyDialog());
				}
			}
		}
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

}
