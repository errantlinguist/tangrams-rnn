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

import java.util.stream.Stream;

/**
 * @author <a href="mailto:tcshore@kth.se">Todd Shore</a>
 * @since 5 Jan 2018
 *
 */
final class ReferentNGramRowGrouping<V, R> {

	private final Stream<R> ngramRows;

	private final V refVizElem;

	private final String sessionName;

	ReferentNGramRowGrouping(final String sessionName, final V refVizElem, final Stream<R> ngramRows) {
		this.sessionName = sessionName;
		this.refVizElem = refVizElem;
		this.ngramRows = ngramRows;
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
		if (!(obj instanceof ReferentNGramRowGrouping)) {
			return false;
		}
		final ReferentNGramRowGrouping<?, ?> other = (ReferentNGramRowGrouping<?, ?>) obj;
		if (ngramRows == null) {
			if (other.ngramRows != null) {
				return false;
			}
		} else if (!ngramRows.equals(other.ngramRows)) {
			return false;
		}
		if (refVizElem == null) {
			if (other.refVizElem != null) {
				return false;
			}
		} else if (!refVizElem.equals(other.refVizElem)) {
			return false;
		}
		if (sessionName == null) {
			if (other.sessionName != null) {
				return false;
			}
		} else if (!sessionName.equals(other.sessionName)) {
			return false;
		}
		return true;
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
		result = prime * result + (ngramRows == null ? 0 : ngramRows.hashCode());
		result = prime * result + (refVizElem == null ? 0 : refVizElem.hashCode());
		result = prime * result + (sessionName == null ? 0 : sessionName.hashCode());
		return result;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		final StringBuilder builder = new StringBuilder(128);
		builder.append("ReferentNGramRowGouping [sessionName=");
		builder.append(sessionName);
		builder.append(", refVizElem=");
		builder.append(refVizElem);
		builder.append(", ngramRows=");
		builder.append(ngramRows);
		builder.append("]");
		return builder.toString();
	}

	/**
	 * @return the ngramRows
	 */
	Stream<R> getNgramRows() {
		return ngramRows;
	}

	/**
	 * @return the refVizElem
	 */
	V getRefVizElem() {
		return refVizElem;
	}

	/**
	 * @return the sessionName
	 */
	String getSessionName() {
		return sessionName;
	}

}