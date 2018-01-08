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

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import se.kth.speech.function.ThrowingSupplier;

/**
 * @author <a href="mailto:errantlinguist+github@gmail.com">Todd Shore</a>
 * @since 1 Jun 2017
 *
 */
public final class CLIParameters {

	private static final Charset DEFAULT_OUTPUT_ENCODING = StandardCharsets.UTF_8;

	private static final Logger LOGGER = LoggerFactory.getLogger(CLIParameters.class);

	public static ThrowingSupplier<PrintStream, IOException> parseOutpath(final File outfile) throws IOException {
		return parseOutpath(outfile, DEFAULT_OUTPUT_ENCODING);
	}

	public static ThrowingSupplier<PrintStream, IOException> parseOutpath(final File outfile, // NO_UCD (use private)
			final Charset outputEncoding) throws IOException { // NO_UCD
		final ThrowingSupplier<PrintStream, IOException> result;
		if (outfile == null) {
			LOGGER.info("No output file path specified; Will write to standard output stream.");
			result = () -> System.out;
		} else {
			LOGGER.info("Output file path is \"{}\".", outfile);
			result = () -> new PrintStream(new BufferedOutputStream(Files.newOutputStream(outfile.toPath(),
					StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)));
		}
		return result;
	}

	private CLIParameters() {
	}

}
