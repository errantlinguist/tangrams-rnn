package tangram.logistic;

import java.io.PrintWriter;
import java.nio.file.Paths;

import tangram.data.*;

public class TestDialog {

	public static void main(String[] args) throws Exception {
		SessionSet set = new SessionSet(Paths.get("C:/data/tangram"));
		set.crossValidate((training,testing) -> {
			try {
				PrintWriter pw = new PrintWriter("C:/data/tangram/dialogs/" + testing.name + ".html");
				pw.println("<table>");
				LogisticModel model = new LogisticModel();
				model.train(training);
				for (Round round : new RoundSet(set).rounds) {
					for (Utterance utt : round.utts) {
						for (String word : utt.fullText) {
							
						}
					}
					//new File("turn-0").list;
					pw.println("<img src=\"" + testing.name + "/screenshots/\"></td></tr>");
				}
				pw.println("</table>");
				pw.close();
			} catch (Exception e) {
				e.printStackTrace();
			}
			
		});
	}
	
}
