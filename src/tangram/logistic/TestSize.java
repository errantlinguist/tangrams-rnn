package tangram.logistic;

import java.io.PrintWriter;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

import tangram.data.Referent;
import tangram.data.SessionSet;

public class TestSize {

	public static void main(String[] args) throws Exception {
		SessionSet set = new SessionSet(Paths.get("C:/data/tangram"));
		LogisticModel model = new LogisticModel();
		model.train(set);
		
		List<String> wlist = Arrays.asList(new String[]{"large", "big", "small"});
		
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
