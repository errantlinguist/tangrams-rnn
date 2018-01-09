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
package se.kth.speech.nlp;

import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntMaps;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;

/**
 * @param <O>
 *            The observation type.
 * @author <a href="mailto:errantlinguist+github@gmail.com">Todd Shore</a>
 * @since Jan 9, 2018
 *
 */
public final class DocumentObservationData<O> {

	private static <K> void incrementCount(final K key, final Object2IntMap<? super K> counts) {
		final int oldValue = counts.getInt(key);
		final int oldValue2 = counts.put(key, oldValue + 1);
		assert oldValue == oldValue2;
	}

	private static <K> void incrementCount(final Object2IntMap.Entry<K> entry, final Object2IntMap<? super K> counts) {
		final K key = entry.getKey();
		final int oldValue = counts.getInt(key);
		final int oldValue2 = counts.put(key, oldValue + entry.getIntValue());
		assert oldValue == oldValue2;
	}

	private int documentOccurrenceCount;

	private final Object2IntMap<O> observationCounts;

	public DocumentObservationData() {
		this(new Object2IntOpenHashMap<>(), 0);
	}

	public DocumentObservationData(final Object2IntMap<O> observationCounts, final int documentOccurrenceCount) {
		this.observationCounts = observationCounts;
		this.documentOccurrenceCount = documentOccurrenceCount;
	}

	public void addObservationCounts(final Object2IntMap<O> addendCounts) {
		addendCounts.object2IntEntrySet().forEach(addendCount -> incrementCount(addendCount, this.observationCounts));
	}

	/**
	 * @return the documentOccurrenceCount
	 */
	public int getDocumentOccurrenceCount() {
		return documentOccurrenceCount;
	}

	/**
	 * @return the observationCounts
	 */
	public Object2IntMap<O> getObservationCounts() {
		return Object2IntMaps.unmodifiable(observationCounts);
	}

	public void incrementDocumentOccurrenceCount() {
		documentOccurrenceCount += 1;
	}

	public void incrementObservationCount(final O obs) {
		incrementCount(obs, observationCounts);
	}

}
