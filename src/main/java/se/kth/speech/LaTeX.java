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
package se.kth.speech;

import java.util.Arrays;
import java.util.Objects;
import java.util.stream.Collector;
import java.util.stream.Collectors;

/**
 * @author <a href="mailto:errantlinguist+github@gmail.com">Todd Shore</a>
 * @since 3 Jan 2018
 *
 */
public final class LaTeX {

	private static final Collector<CharSequence, ?, String> REPLACEMENT_JOINER = Collectors.joining();

	private static final String[][] REPLACEMENTS = createCharReplacementMatrix();

	/**
	 * Escapes characters which have special meaning in LaTeX.
	 *
	 * @see <a href=
	 *      "https://tex.stackexchange.com/a/34586/19135">StackExchange</a>
	 * @param input
	 *            The string to escape.
	 * @return A valid string which is linguistically equivalent in LaTeX to the
	 *         input string.
	 */
	public static String escapeReservedCharacters(final String input) {
		final int len = input.length();
		final String[] replacements = new String[len];
		for (int charIdx = 0; charIdx < len; ++charIdx) {
			final String replacee = input.substring(charIdx, charIdx + 1);
			replacements[charIdx] = findReplacement(replacee);
		}
		return Arrays.stream(replacements).collect(REPLACEMENT_JOINER);
	}

	private static String[] createBackslashEscapedCharReplacement(final String c) {
		return new String[] { c, "\\" + c };
	}

	private static String[][] createCharReplacementMatrix() {
		final String[][] result = { createBackslashEscapedCharReplacement("&"),
				createBackslashEscapedCharReplacement("%"), createBackslashEscapedCharReplacement("$"),
				createBackslashEscapedCharReplacement("#"), createBackslashEscapedCharReplacement("_"),
				createBackslashEscapedCharReplacement("{"), createBackslashEscapedCharReplacement("}"),
				new String[] { "~", "\\textasciitilde{}" }, new String[] { "^", "\\textasciicircum{}" },
				new String[] { "\\", "\\textbackslash{}" }, };
		assert Arrays.stream(result).mapToInt(arr -> arr.length).allMatch(len -> len == 2);
		assert Arrays.stream(result).flatMap(Arrays::stream).noneMatch(Objects::isNull);
		assert Arrays.stream(result).map(arr -> arr[0]).allMatch(str -> str.length() == 1);
		return result;
	}

	private static String findReplacement(final String c) {
		String result = c;
		for (final String[] replacement : REPLACEMENTS) {
			if (replacement[0].equals(c)) {
				result = replacement[1];
				break;
			}
		}
		return result;
	}

	private LaTeX() {
	}

}
