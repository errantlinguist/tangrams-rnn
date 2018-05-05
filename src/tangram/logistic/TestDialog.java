package tangram.logistic;

import java.awt.Color;
import java.io.File;
import java.io.PrintWriter;

import tangram.data.*;

public class TestDialog {

	public static void main(String[] args) throws Exception {
		//SessionSet set = new SessionSet(new File("C:/data/tangram"));
		//set.crossValidate((training,testing) -> {
		Parameters.WEIGHT_BY_FREQ = true;
		Parameters.WEIGHT_BY_POWER = true;
		Parameters.UPDATE_MODEL = false;
		Parameters.UPDATE_WEIGHT = 1;
		SessionSet testingSet = new SessionSet(new File("C:/data/tangram/testing.txt"));
		LogisticModel model = new LogisticModel();
		model.train(new SessionSet(new File("C:/data/tangram/training.txt")));
		model.storeModel();
		for (Session testing : testingSet.sessions) {

			if (Parameters.UPDATE_MODEL) {
				model.retrieveModel();
			}
			try {
				System.out.println(testing.name);
				//LogisticModel model = new LogisticModel();
				//model.train(training);
				DialogPrinter dialogPrinter = new DialogPrinter() {

					@Override
					public void print(PrintWriter pw, Session session, Round round) throws Exception {
						for (Referent ref : round.referents) {
							if (!ref.target)
								//pw.println("<div>" + ref.shape);
								continue;
							else
								pw.println("<div><b>" + ref.shape + "</b> " + model.targetRank(round));
							for (Utterance utt : round.utts) {
								pw.println("<div>");
								pw.println(utt.speaker + ": ");
								String[] words = Parameters.ONLY_REFLANG ? utt.refText : utt.fullText;
								for (String word : words) {
									double score = model.score(word, ref);
									double weight = Math.log10(model.vocab.getCount(word,3));
									weight *= model.power.getOrDefault(word, 0.0);
									//if (word.equals("the"))
										//System.out.println("the: " + weight + " " + model.power.getOrDefault(word, 0.0) + " " + Math.log10(model.vocab.getCount(word,3)));
									pw.println("<span style=\"color:" + getHTMLColorString(score, weight) + "\" title=\"" + score + "\">" + word  + "</span> ");  // (" + weight + ") "
								}
								pw.println("</div>");
							}
							pw.println("</div>");
						}

						if (Parameters.UPDATE_MODEL) {
							model.updateModel(round);
						}
					}
					
				};
				writeDialog(new File("C:/data/tangram/" + testing.name + "/dialog.html"), testing, dialogPrinter);
				

				//System.exit(0);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		//});
	}
	
	public interface DialogPrinter {

		void print(PrintWriter pw, Session session, Round round) throws Exception;
		
	}
	
	public static void writeDialog(File outFile, Session session, DialogPrinter dialogPrinter) throws Exception {
		PrintWriter pw = new PrintWriter(outFile);
		pw.println("<table border=\"1\">");
		int roundn = -1;
		for (Round round : session.rounds) {
			pw.println("<tr><td valign=\"top\">" + (round.n)  + "</td>");
			pw.println("<td>");
			dialogPrinter.print(pw, session, round);
			pw.println("</td>");
			pw.println("<td valign=\"top\">");
			String filen;
			if (roundn == -1) {
				filen = "game-start-";
			} else {
				filen = "turn-" + roundn + "-";
			}
			roundn++;
			File[] fa = new File("d:/data/tangram/" + session.name + "/screenshots").listFiles((file)->{
				return file.getName().startsWith(filen);
			});
			if (fa.length > 0) {
				int size = 10;
				int left = (int)(round.target.posy*200); 
				int top = (int)(round.target.posx*170) + size;
				left -= size/2;
				top -= size/2;
				pw.println("<div style=\"border:1px solid red;position:relative;left:" + left + "px;top:" + top + "px;width:10px;height:10px\"></div>");
				pw.println("<img width=\"200\" src=\"screenshots/" + fa[0].getName() + "\">");
			}
			pw.println("</td>");
			//pw.println("<td>" + model.targetRank(round) + "</td>");
			pw.println("</tr>");
		}
		pw.println("</table>");
		pw.close();
	}
	
	public static String getHTMLColorString(double score) {	
	    return TestColor.getHTMLColorString(Color.getHSBColor((float)score * 0.32f, 1f, 1f));     
	}
	
	public static String getHTMLColorString(double score, double weight) {	
	    return TestColor.getHTMLColorString(Color.getHSBColor((float)score * 0.32f, (float)weight, 1f));     
	}
	
}
