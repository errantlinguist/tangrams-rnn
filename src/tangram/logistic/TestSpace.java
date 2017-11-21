package tangram.logistic;

import java.io.PrintWriter;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import tangram.data.Referent;
import tangram.data.SessionSet;

public class TestSpace {

	public static void main(String[] args) throws Exception {
		SessionSet set = new SessionSet(Paths.get("C:/data/tangram"));
		LogisticModel model = new LogisticModel();
		final CompletableFuture<Void> trainingJob = model.train(set);
		trainingJob.join();
		
		List<String> wlist = Arrays.asList(new String[]{"left", "right", "bottom", "top", "middle", "center", "corner"});
		
		PrintWriter pw = new PrintWriter("space.html");
		
		pw.println("<table cellpadding=\"5\">");
				
		for (float y = 0f; y <= 1f; y += 0.2) {
			pw.println("<tr>");
			for (float x = 0f; x <= 1f; x += 0.2) {
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
