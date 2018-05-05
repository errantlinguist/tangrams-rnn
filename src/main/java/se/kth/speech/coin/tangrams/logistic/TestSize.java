package se.kth.speech.coin.tangrams.logistic;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.List;

import se.kth.speech.coin.tangrams.data.Referent;
import se.kth.speech.coin.tangrams.data.SessionSet;

public class TestSize {

	public static void main(String[] args) throws IOException, PredictionException, TrainingException {
		SessionSet set = new SessionSet(new File("C:/data/tangram"));
		LogisticModel model = new LogisticModel();
		model.train(set);
		
		List<String> wlist = Arrays.asList(new String[]{"large", "big", "small", "smallest"});
		
		PrintWriter pw = new PrintWriter("size.html");
		
		pw.println("<table>");
				
		for (float size = 0f; size <= 0.04f; size += 0.005) {
			
			Referent ref = new Referent();
			ref.size = size;
			
			pw.println("<tr><td>" + size + "<td>");
			
			for (String word: wlist) {
				double score = model.score(word, ref);
				pw.println("<td style=\"color:" + TestColor.getHTMLColorString(score) + "\">" + word + "</td>");
			}
		
			pw.println("</tr>");
		}
		
		pw.println("</table>");
		
		pw.close();
	
	}
	
}
