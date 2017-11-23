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

import se.kth.speech.coin.tangrams.wac.data.Round;
import se.kth.speech.coin.tangrams.wac.data.RoundSet;
import se.kth.speech.coin.tangrams.wac.data.SessionSet;
import se.kth.speech.coin.tangrams.wac.data.SessionSetReader;
import se.kth.speech.coin.tangrams.wac.data.Utterance;

public class TestDialog {

	public static void main(String[] args) throws Exception {
		final Path inpath = Paths.get(args[0]);
		final Path outpath = Paths.get(args[1]);
		System.err.println(String.format("Reading sessions from \"%s\"; Will write output to \"%s\".", inpath, outpath));
		SessionSet set = new SessionSetReader().apply(inpath);
		set.crossValidate((training,testing) -> {
			try (PrintWriter pw = new PrintWriter(Files.newBufferedWriter(outpath.resolve(testing.getName() + ".html")))) {
				pw.println("<table>");
				LogisticModel model = new LogisticModel();
				model.train(training);
				for (Round round : new RoundSet(set).getRounds()) {
					for (Utterance utt : round.getUtts()) {
						for (String word : utt.getTokens()) {
							
						}
					}
					//new File("turn-0").list;
					pw.println("<img src=\"" + testing.getName() + "/screenshots/\"></td></tr>");
				}
				pw.println("</table>");
			} catch (IOException e) {
				throw new UncheckedIOException(e);
			}
			
		});
	}
	
}
