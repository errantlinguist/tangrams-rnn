package se.kth.speech.coin.tangrams.logistic;

import java.io.File;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.List;

import se.kth.speech.coin.tangrams.data.Referent;
import se.kth.speech.coin.tangrams.data.SessionSet;

public class TestSpace {

	public static void main(String[] args) throws Exception {
		SessionSet set = new SessionSet(new File("C:/data/tangram"));
		LogisticModel model = new LogisticModel();
		model.train(set);
		
		List<String> wlist = Arrays.asList(new String[]{"left", "right", "bottom", "top", "middle", "center", "corner"});
		
		PrintWriter pw = new PrintWriter("space2.html");
		
		pw.println("<table cellpadding=\"5\" width=\"100%\">");
				
		for (float y = 0f; y <= 1f; y += 0.5) {
			pw.println("<tr>");
			for (float x = 0f; x <= 1f; x += 0.5) {
				pw.println("<td style=\"border:1px solid black\">");
				
				Referent ref = new Referent();
				ref.setPos(y, x);
				
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
