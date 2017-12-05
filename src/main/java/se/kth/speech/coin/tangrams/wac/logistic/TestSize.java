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
package se.kth.speech.coin.tangrams.wac.logistic;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import se.kth.speech.coin.tangrams.wac.data.Referent;
import se.kth.speech.coin.tangrams.wac.data.SessionSet;
import se.kth.speech.coin.tangrams.wac.data.SessionSetReader;
import weka.classifiers.functions.Logistic;

public final class TestSize {

	private static final Logger LOGGER = LoggerFactory.getLogger(TestSize.class);

	public static void main(final String[] args) throws IOException, ClassificationException {
		if (args.length != 3) {
			throw new IllegalArgumentException(String.format("Usage: %s INPATH REFERRING_LANG_FILE OUTPATH", TestSize.class.getName()));
		} else {
			final Path inpath = Paths.get(args[0]);
			final Path refTokenFilePath = Paths.get(args[1]);
			final Path outpath = Paths.get(args[2]);
			LOGGER.info(
					"Will read sessions from \"{}\", using referring language read from \"{}\"; Will write output to \"{}\".",
					inpath, refTokenFilePath, outpath);
			run(inpath, refTokenFilePath, outpath);
		}
	}

	public static void write(final LogisticModel model, final Path outpath)
			throws IOException, ClassificationException {
		final List<String> wlist = Arrays.asList(new String[] { "large", "big", "small" });

		final Path outfilePath = outpath.resolve("size.html");
		LOGGER.info("Writing to \"{}\".", outfilePath);
		try (PrintWriter pw = new PrintWriter(
				Files.newBufferedWriter(outfilePath, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING),
				true)) {

			pw.println("<table>");

			for (float size = 0f; size <= 0.04f; size += 0.005) {

				final Referent ref = new Referent();
				ref.setSize(size);

				pw.println("<tr><td>" + size + "<td>");

				for (final String word : wlist) {
					final Logistic wordClassifier = model.getWordClassifier(word);
					final double score = model.score(wordClassifier, ref);
					pw.println("<td style=\"color:" + TestColor.getHTMLColorString(score) + "\">" + word + "</td>");
				}

				pw.println("</tr>");
			}

			pw.println("</table>");
		}
		LOGGER.info("Finished writing to \"{}\".", outfilePath);
	}

	private static void run(final Path inpath, final Path refTokenFilePath, final Path outpath)
			throws IOException, ClassificationException {
		final SessionSet set = new SessionSetReader(refTokenFilePath).apply(inpath);
		LOGGER.info("Read {} session(s).", set.size());
		final LogisticModel model = new LogisticModel();
		model.train(set);
		write(model, outpath);
	}
}
