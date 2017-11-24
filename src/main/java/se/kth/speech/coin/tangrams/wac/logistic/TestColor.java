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

import java.awt.Color;
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

public class TestColor {

	private static final Logger LOGGER = LoggerFactory.getLogger(TestColor.class);

	public static String getHTMLColorString(final Color color) {
		final String red = Integer.toHexString(color.getRed());
		final String green = Integer.toHexString(color.getGreen());
		final String blue = Integer.toHexString(color.getBlue());
		return "#" + (red.length() == 1 ? "0" + red : red) + (green.length() == 1 ? "0" + green : green)
				+ (blue.length() == 1 ? "0" + blue : blue);
	}

	public static String getHTMLColorString(final double score) {
		return getHTMLColorString(Color.getHSBColor(0f, 0f, 1f - (float) score));
	}

	public static void main(final String[] args) throws IOException, ClassificationException {
		if (args.length != 2) {
			throw new IllegalArgumentException(String.format("Usage: %s INPATH OUTPATH", TestColor.class.getName()));
		} else {
			final Path inpath = Paths.get(args[0]);
			final Path outpath = Paths.get(args[1]);
			LOGGER.info("Will read sessions from \"{}\"; Will write output to \"{}\".", inpath, outpath);
			run(inpath, outpath);
		}
	}

	private static void run(final Path inpath, final Path outpath) throws IOException, ClassificationException {
		final SessionSet set = new SessionSetReader().apply(inpath);
		LOGGER.info("Read {} session(s).", set.size());
		final LogisticModel model = new LogisticModel();
		model.train(set);

		final List<String> wlist = Arrays
				.asList(new String[] { "red", "green", "blue", "yellow", "magenta", "pink", "orange" });

		final Path outfilePath = outpath.resolve("colors.html");
		LOGGER.info("Writing to \"{}\".", outfilePath);
		try (PrintWriter pw = new PrintWriter(
				Files.newBufferedWriter(outfilePath, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING),
				true)) {

			pw.println("<table>");

			for (float hue = 0f; hue < 1f; hue += 0.02f) {
				final Color col = Color.getHSBColor(hue, 1f, 1f);
				final Referent ref = new Referent();
				ref.setHue(hue);
				ref.setBlue(col.getBlue() / 255f);
				ref.setRed(col.getRed() / 255f);
				ref.setGreen(col.getGreen() / 255f);
				pw.println("<tr><td style=\"background-color:" + getHTMLColorString(col) + ";width:100\">"
						+ (int) (hue * 1000) + "</td>");

				for (final String w : wlist) {
					final double score = model.score(w, ref);
					pw.println("<td style=\"color:" + getHTMLColorString(score) + "\">" + w + "</td>");
				}

				/*
				 * for (int i = 0; i < 6; i++) { float h = (float)i / 6; float
				 * dist = Math.min(Math.min(Math.abs(ref.hue - h),
				 * Math.abs(ref.hue - (1f+h))), Math.abs((1f+ref.hue) - h));
				 * float val = (float) Math.pow((1-dist), 3); pw.println("<td>"
				 * + val + "</td>"); }
				 */

				pw.println("</tr>");

			}

			pw.println("</table>");
		}
		LOGGER.info("Finished writing to \"{}\".", outfilePath);
	}

}
