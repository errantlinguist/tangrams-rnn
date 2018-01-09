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

import se.kth.speech.coin.tangrams.wac.data.Session;

/**
 * @author <a href="mailto:errantlinguist+github@gmail.com">Todd Shore</a>
 * @since 5 Jan 2018
 *
 */
final class ReferentNGramRowGrouping<V, R> {

	private final int documentOccurrenceCount;

	private final Stream<R> ngramRows;

	private final V refVizElem;

	private final Session session;

	ReferentNGramRowGrouping(final Session session, final V refVizElem, final int documentOccurrenceCount,
			final Stream<R> ngramRows) {
		this.session = session;
		this.refVizElem = refVizElem;
		this.documentOccurrenceCount = documentOccurrenceCount;
		this.ngramRows = ngramRows;
	}

	/**
	 * @return the documentOccurrenceCount
	 */
	int getDocumentOccurrenceCount() {
		return documentOccurrenceCount;
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
	 * @return the session
	 */
	Session getSession() {
		return session;
	}

}