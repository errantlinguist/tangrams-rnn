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
package se.kth.speech;

import org.junit.Assert;
import org.junit.Test;

/**
 * @author <a href="mailto:tcshore@kth.se">Todd Shore</a>
 * @since 12 May 2017
 *
 */
public final class FileNamesTest {

	@Test
	public void testSanitizeNegative() {
		final String orig = "test_file.txt";
		final String actual = FileNames.sanitize(orig, "-");
		Assert.assertEquals(orig, actual);
	}

	@Test
	public void testSanitizePositive() {
		final String orig = "test*file|1.txt";
		final String expected = "test-file-1.txt";
		final String actual = FileNames.sanitize(orig, "-");
		Assert.assertEquals(expected, actual);
	}

}
