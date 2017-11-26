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
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import se.kth.speech.coin.tangrams.wac.data.Round;
import se.kth.speech.coin.tangrams.wac.data.RoundSet;
import se.kth.speech.coin.tangrams.wac.data.SessionSet;
import se.kth.speech.coin.tangrams.wac.data.SessionSetReader;
import se.kth.speech.coin.tangrams.wac.data.Utterance;

public final class TestDialog {

	private static final Logger LOGGER = LoggerFactory.getLogger(TestDialog.class);

	public static void main(final String[] args) throws IOException, ClassificationException {
		if (args.length != 3) {
			throw new IllegalArgumentException(String.format("Usage: %s INPATH REFERRING_LANG_FILE OUTPATH", TestDialog.class.getName()));
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

	private static void run(final Path inpath, final Path refTokenFilePath, final Path outpath) throws IOException, ClassificationException {
		final SessionSet set = new SessionSetReader(refTokenFilePath).apply(inpath);
		set.crossValidate((training, testing) -> {
			final Path outfilePath = outpath.resolve(testing.getName() + ".html");
			try (PrintWriter pw = new PrintWriter(Files.newBufferedWriter(outfilePath, StandardOpenOption.CREATE,
					StandardOpenOption.TRUNCATE_EXISTING))) {
				pw.println("<table>");
				final Map<ModelParameter, Object> modelParams = ModelParameter.createDefaultParamValueMap();
				final LogisticModel model = new LogisticModel(modelParams);
				model.train(training);
				for (final Round round : new RoundSet(set, modelParams).getRounds()) {
					for (final Utterance utt : round.getUtts()) {
						for (final String word : utt.getTokens()) {

						}
					}
					// new File("turn-0").list;
					pw.println("<img src=\"" + testing.getName() + "/screenshots/\"></td></tr>");
				}
				pw.println("</table>");
			} catch (final IOException e) {
				throw new UncheckedIOException(e);
			}

		});
	}

}
