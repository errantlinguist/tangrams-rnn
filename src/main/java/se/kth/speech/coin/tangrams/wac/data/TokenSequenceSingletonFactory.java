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

import java.lang.ref.Reference;
import java.lang.ref.SoftReference;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Function;

final class TokenSequenceSingletonFactory implements Function<String[], List<String>> {

	private final ConcurrentMap<List<String>, Reference<List<String>>> singletonInstances;

	TokenSequenceSingletonFactory(final int expectedUniqueTokenSequenceCount) {
		singletonInstances = new ConcurrentHashMap<>(expectedUniqueTokenSequenceCount);
	}

	@Override
	public List<String> apply(final String[] tokens) {
		return apply(Arrays.asList(tokens));
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		final StringBuilder builder = new StringBuilder(48 * (singletonInstances.size() + 1));
		builder.append("TokenListSingletonFactory [singletonInstances=");
		builder.append(singletonInstances);
		builder.append("]");
		return builder.toString();
	}

	private List<String> apply(final List<String> tokens) {
		return singletonInstances.compute(tokens, (key, oldValue) -> {
			final Reference<List<String>> newValue;
			if (oldValue == null || oldValue.get() == null) {
				final List<String> internedTokens = Arrays
						.asList(key.stream().map(String::intern).toArray(String[]::new));
				newValue = new SoftReference<>(Collections.unmodifiableList(internedTokens));
			} else {
				newValue = oldValue;
			}
			return newValue;
		}).get();
	}
}
