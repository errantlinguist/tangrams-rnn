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

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import weka.core.tokenizers.NGramTokenizer;

/**
 * @author <a href="mailto:tcshore@kth.se">Todd Shore</a>
 * @since 3 Jan 2018
 *
 */
final class NGramFactory implements Function<List<String>, List<List<String>>> {

	private static final String TOKEN_DELIMITER;

	private static final Collector<CharSequence, ?, String> TOKEN_JOINER;

	static {
		TOKEN_DELIMITER = " ";
		TOKEN_JOINER = Collectors.joining(TOKEN_DELIMITER);
	}

	private static NGramTokenizer createTokenizer(final int minLength, final int maxLength) {
		final NGramTokenizer result = new NGramTokenizer();
		result.setDelimiters(TOKEN_DELIMITER);
		result.setNGramMinSize(minLength);
		result.setNGramMaxSize(maxLength);
		return result;
	}

	private final Map<List<String>, List<List<String>>> cache;

	private final NGramTokenizer tokenizer;

	public NGramFactory(final int minLength, final int maxLength) {
		this(createTokenizer(minLength, maxLength), new HashMap<>(1000));
	}

	private NGramFactory(final NGramTokenizer tokenizer, final Map<List<String>, List<List<String>>> cache) {
		this.tokenizer = tokenizer;
		this.cache = cache;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see java.util.function.Function#apply(java.lang.Object)
	 */
	@Override
	public List<List<String>> apply(final List<String> tokenSeq) {
		return cache.computeIfAbsent(tokenSeq, this::createNgrams);
	}

	private List<List<String>> createNgrams(final List<String> tokenSeq) {
		final String inputStr = tokenSeq.stream().collect(TOKEN_JOINER);
		tokenizer.tokenize(inputStr);
		final Stream.Builder<List<String>> resultBuilder = Stream.builder();
		while (tokenizer.hasMoreElements()) {
			final String nextStr = tokenizer.nextElement();
			final List<String> ngram = Arrays
					.asList(Arrays.stream(nextStr.split(TOKEN_DELIMITER)).map(String::intern).toArray(String[]::new));
			resultBuilder.accept(ngram);
		}
		@SuppressWarnings("unchecked")
		final List<List<String>> result = Arrays.asList(resultBuilder.build().toArray(List[]::new));
		return result;
	}

}
