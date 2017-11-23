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
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

import se.kth.speech.coin.tangrams.wac.data.Referent;
import se.kth.speech.coin.tangrams.wac.data.SessionSet;
import se.kth.speech.coin.tangrams.wac.data.SessionSetReader;

public class TestSpace {

	public static void main(String[] args) throws Exception {
		final Path inpath = Paths.get(args[0]);
		System.err.println(String.format("Reading sessions from \"%s\".", inpath));
		SessionSet set = new SessionSetReader().apply(inpath);
		LogisticModel model = new LogisticModel();
		model.train(set);
		
		List<String> wlist = Arrays.asList(new String[]{"left", "right", "bottom", "top", "middle", "center", "corner"});
		
		PrintWriter pw = new PrintWriter("space.html");
		
		pw.println("<table cellpadding=\"5\">");
				
		for (float y = 0f; y <= 1f; y += 0.2) {
			pw.println("<tr>");
			for (float x = 0f; x <= 1f; x += 0.2) {
				pw.println("<td style=\"border:1px solid black\">");
				
				Referent ref = new Referent();
				ref.setPosition(y, x);
				
				for (String w: wlist) {
					double score = model.score(w, ref);
					pw.println("<div style=\"color:" + TestColor.getHTMLColorString(score) + "\">" + w + "</div>");
				}
				
				pw.println("</td>");
			}
			pw.println("</tr>");
		}
		
		pw.println("</table>");
		
		pw.close();
	
	}
	
}
