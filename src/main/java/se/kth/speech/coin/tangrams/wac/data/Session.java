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

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public final class Session {

	private static final Comparator<String> NAME_COMPARATOR = new Comparator<String>() {

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

	/**
	 * @return the nameComparator
	 */
	public static Comparator<String> getNameComparator() {
		return NAME_COMPARATOR;
	}

	private final int hashCode;

	private final String name;

	private final List<Round> rounds;

	public Session(final String name, final List<Round> rounds) { // NO_UCD (use
																	// default)
		this.name = name;
		this.rounds = Collections.unmodifiableList(rounds);

		hashCode = calculateHashCode();
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
		if (!(obj instanceof Session)) {
			return false;
		}
		final Session other = (Session) obj;
		if (name == null) {
			if (other.name != null) {
				return false;
			}
		} else if (!name.equals(other.name)) {
			return false;
		}
		if (rounds == null) {
			if (other.rounds != null) {
				return false;
			}
		} else if (!rounds.equals(other.rounds)) {
			return false;
		}
		return true;
	}

	/**
	 * @return the name
	 */
	public String getName() {
		return name;
	}

	/**
	 * @return the rounds (unmodifiable view)
	 */
	public List<Round> getRounds() {
		return rounds;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		return hashCode;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		final StringBuilder builder = new StringBuilder(64 * (rounds.size() + 1));
		builder.append("Session [name=");
		builder.append(name);
		builder.append(", rounds=");
		builder.append(rounds);
		builder.append("]");
		return builder.toString();
	}

	private int calculateHashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + (name == null ? 0 : name.hashCode());
		result = prime * result + (rounds == null ? 0 : rounds.hashCode());
		return result;
	}

}
