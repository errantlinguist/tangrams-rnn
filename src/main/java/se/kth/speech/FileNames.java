/*
 *  This file is part of se.kth.speech.coin.tangrams-restricted.analysis.
 *
 *  se.kth.speech.coin.tangrams-restricted.analysis is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
*/
package se.kth.speech;

import java.nio.file.Path;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.regex.Pattern;

/**
 * @author <a href="mailto:tcshore@kth.se">Todd Shore</a>
 * @since 12 May 2017
 *
 */
public final class FileNames {

	/**
	 * A {@link Pattern} matching characters which are invalid as parts of
	 * either Windows or POSIX-style filenames.
	 *
	 * @see <a href=
	 *      "http://stackoverflow.com/a/894133/1391325">StackOverflow</a>
	 */
	public static final Pattern ILLEGAL_CHAR_PATTERN;

	/**
	 * An array of characters which are invalid as parts of either Windows or
	 * POSIX-style filenames.
	 *
	 * @see <a href=
	 *      "http://stackoverflow.com/a/894133/1391325">StackOverflow</a>
	 */
	public static final char[] ILLEGAL_CHARACTERS;

	/**
	 * @see <a href=
	 *      "http://stackoverflow.com/a/4546093/1391325">StackOverflow</a>
	 */
	private static final Pattern FILE_EXT_SPLITTING_PATTERN = Pattern.compile("\\.(?!.*\\.)");

	static {
		// http://stackoverflow.com/a/894133/1391325
		ILLEGAL_CHARACTERS = new char[] { '/', '\n', '\r', '\t', '\0', '\f', '`', '?', '*', '\\', '<', '>', '|', '\"',
				':' };
		final String regex = "[" + Pattern.quote(new String(ILLEGAL_CHARACTERS)) + ']';
		ILLEGAL_CHAR_PATTERN = Pattern.compile(regex);
	}

	public static Map<Path, String> createMinimalPathLeafNameMap(final Collection<Path> paths,
			final Function<? super String, String> transformer) {
		int depth = 1;
		Map<Path, String> result = createPathLeafNameMap(paths, depth, transformer);
		Set<String> uniqueNames = new HashSet<>(result.values());
		while (uniqueNames.size() < result.values().size()) {
			result = createPathLeafNameMap(paths, ++depth, transformer);
			uniqueNames = new HashSet<>(result.values());
		}
		return result;
	}

	public static Map<Path, String> createPathLeafNameMap(final Collection<Path> paths, final int depth,
			final Function<? super String, String> transformer) {
		if (depth < 1) {
			throw new IllegalArgumentException("Depth must be a positive value.");
		}
		final Map<Path, String> result = new HashMap<>(HashedCollections.capacity(paths.size()));
		for (final Path path : paths) {
			final int endIndex = path.getNameCount();
			final int beginIndex = endIndex - depth;
			final Path leafPath = path.subpath(beginIndex, endIndex);
			final String transformedStr = transformer.apply(leafPath.toString());
			result.put(path, transformedStr);
		}
		return result;
	}

	/**
	 * Replaces characters in a given string which are
	 * {@link FileNames#ILLEGAL_CHARACTERS invalid either as Windows or as
	 * POSIX-style filenames}, i.e.&nbsp;as single path components.
	 *
	 * @param fileName
	 *            The filename to sanitize,
	 *            e.g.&nbsp;{@code "weird|filename.txt"} for a file with the
	 *            path {@code "/home/users/bob/Documents/weird|filename.txt"}.
	 * @param replacement
	 *            The {@link String} to use as a replacement.
	 * @return A {@link String} with all invalid characters replaced by the
	 *         given replacement.
	 */
	public static String sanitize(final CharSequence fileName, final String replacement) {
		return ILLEGAL_CHAR_PATTERN.matcher(fileName).replaceAll(replacement);
	}

	public static String[] splitBase(final String path) {
		return FILE_EXT_SPLITTING_PATTERN.split(path);
	}

	private FileNames() {
	}

}
