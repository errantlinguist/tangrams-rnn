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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * @author <a href="mailto:tcshore@kth.se">Todd Shore</a>
 * @since 6 Jan 2018
 *
 */
public final class BrownCorpusReading {

	private static final Set<String> FILENAME_BLACKLIST = new HashSet<>(
			Arrays.asList("cats.txt", "README", "CONTENTS"));

	private static final Pattern WHITESPACE_PATTERN = Pattern.compile("\\s+");

	public static Stream<List<String>> readPlainSentences(final Path indir) throws IOException {
		final Iterable<Path> infiles = Files.walk(indir).filter(Files::isRegularFile)
				.filter(path -> !FILENAME_BLACKLIST.contains(path.getFileName().toString()))::iterator;
		final Stream.Builder<List<String>> resultBuilder = Stream.builder();
		for (final Path infile : infiles) {
			readPlainSentencesSingleFile(infile, resultBuilder);
		}
		return resultBuilder.build();
	}

	private static String getWordForm(final String taggedToken) {
		final int delimIdx = taggedToken.lastIndexOf("/");
		assert delimIdx >= 0 : String.format("Could not parse POS tag from \"%s\".", taggedToken);
		return taggedToken.substring(0, delimIdx);
	}

	private static void readPlainSentencesSingleFile(final Path infile, final Stream.Builder<List<String>> sents)
			throws IOException {
		try (Stream<String> lines = Files.lines(infile).map(String::trim).filter(str -> !str.isEmpty())) {
			// NOTE: flat-mapping would be bad here because we want to keep sentence splits
			// intact
			final Stream<Stream<String>> sentTaggedTokenStreams = lines.map(WHITESPACE_PATTERN::splitAsStream);
			final Stream<List<String>> plainSents = sentTaggedTokenStreams.map(tokenStream -> Arrays
					.asList(tokenStream.map(BrownCorpusReading::getWordForm).toArray(String[]::new)));
			plainSents.forEachOrdered(sents);
		}
	}

	private BrownCorpusReading() {
	}

}
