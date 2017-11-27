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
package se.kth.speech.coin.tangrams;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Function;
import java.util.regex.Pattern;

import se.kth.speech.HashedCollections;

/**
 * This class is threadsafe.
 *
 * @author <a href="mailto:tcshore@kth.se">Todd Shore</a>
 * @since Nov 24, 2017
 *
 */
public final class TokenSequenceSingletonFactory implements Function<String, List<String>> {

	private static final Pattern TOKEN_DELIMITER_PATTERN = Pattern.compile("\\s+");

	private static final int RESULT_MAP_EXPECTED_SIZE = 8095;

	private final ConcurrentMap<String, List<String>> singletonInstances;

	public TokenSequenceSingletonFactory() {
		this(RESULT_MAP_EXPECTED_SIZE);
	}

	public TokenSequenceSingletonFactory(final int instanceCacheInitialCapacity) {
		singletonInstances = new ConcurrentHashMap<>(HashedCollections.capacity(instanceCacheInitialCapacity));
	}

	@Override
	public List<String> apply(final String tokenStr) {
		return singletonInstances.computeIfAbsent(tokenStr, key -> {
			return Arrays.asList(TOKEN_DELIMITER_PATTERN.splitAsStream(key).map(String::intern).toArray(String[]::new));
		});
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
}
