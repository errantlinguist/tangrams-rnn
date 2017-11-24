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

public final class TestSpace {

	private static final Logger LOGGER = LoggerFactory.getLogger(TestSpace.class);

	public static void main(final String[] args) throws Exception {
		if (args.length != 2) {
			throw new IllegalArgumentException(String.format("Usage: %s INPATH OUTPATH", TestSpace.class.getName()));
		} else {
			final Path inpath = Paths.get(args[0]);
			final Path outpath = Paths.get(args[1]);
			LOGGER.info("Will read sessions from \"{}\"; Will write output to \"{}\".", inpath, outpath);
			run(inpath, outpath);
		}
	}

	private static void run(final Path inpath, final Path outpath) throws Exception {
		final SessionSet set = new SessionSetReader().apply(inpath);
		LOGGER.info("Read {} session(s).", set.size());
		final LogisticModel model = new LogisticModel();
		model.train(set);

		final List<String> wlist = Arrays
				.asList(new String[] { "left", "right", "bottom", "top", "middle", "center", "corner" });

		try (PrintWriter pw = new PrintWriter(Files.newBufferedWriter(outpath.resolve("space.html"),
				StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING), true)) {

			pw.println("<table cellpadding=\"5\">");

			for (float y = 0f; y <= 1f; y += 0.2) {
				pw.println("<tr>");
				for (float x = 0f; x <= 1f; x += 0.2) {
					pw.println("<td style=\"border:1px solid black\">");

					final Referent ref = new Referent();
					ref.setPosition(y, x);

					for (final String w : wlist) {
						final double score = model.score(w, ref);
						pw.println("<div style=\"color:" + TestColor.getHTMLColorString(score) + "\">" + w + "</div>");
					}

					pw.println("</td>");
				}
				pw.println("</tr>");
			}

			pw.println("</table>");
		}
	}

}
