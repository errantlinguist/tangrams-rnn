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
package se.kth.speech.concurrent;

import java.util.Map.Entry;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ForkJoinTask;
import java.util.function.Supplier;

public final class MapPopulator<K, V> extends ForkJoinTask<K> {

	/**
	 *
	 */
	private static final long serialVersionUID = 2476606510654821423L;

	private final ConcurrentMap<? super K, V> map;

	private K result;

	private final Supplier<? extends Entry<K, ? extends V>> valueFactory;

	public MapPopulator(final Supplier<? extends Entry<K, ? extends V>> valueFactory,
			final ConcurrentMap<? super K, V> map) {
		this.valueFactory = valueFactory;
		this.map = map;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see java.util.concurrent.ForkJoinTask#getRawResult()
	 */
	@Override
	public K getRawResult() {
		return result;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see java.util.concurrent.ForkJoinTask#exec()
	 */
	@Override
	protected boolean exec() {
		final Entry<K, ? extends V> keyValuePair = valueFactory.get();
		final K key = keyValuePair.getKey();
		final V newValue = keyValuePair.getValue();
		final V oldValue = map.put(key, newValue);
		assert oldValue == null ? true : !oldValue.equals(newValue);
		result = key;
		return true;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see java.util.concurrent.ForkJoinTask#setRawResult(java.lang.Object)
	 */
	@Override
	protected void setRawResult(final K value) {
		result = value;
	}

}