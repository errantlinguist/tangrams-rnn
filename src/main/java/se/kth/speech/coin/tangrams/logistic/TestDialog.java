package se.kth.speech.coin.tangrams.logistic;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.kth.speech.coin.tangrams.data.*;

import java.awt.*;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class TestDialog {

	private static final Logger LOGGER = LoggerFactory.getLogger(TestDialog.class);

	public static void main(String[] args) throws IOException, TrainingException, PredictionException {
		if (args.length != 5) {
			throw new IllegalArgumentException(String.format("Usage: %s <trainingSetFile> <testingSetFile> <refLangMapFile> <sessionScreenshotDir> <outdir>", TestDialog.class.getName()));
		}
		Parameters.WEIGHT_BY_FREQ = true;
		Parameters.WEIGHT_BY_POWER = true;
		Parameters.UPDATE_MODEL = false;
		Parameters.UPDATE_WEIGHT = 1;

		final Path refLangMapFilePath = Paths.get(args[2]);
		LOGGER.info("Reading referring-language map at \"{}\".", refLangMapFilePath);
		final Map<List<String>, String[]> refLangMap = new UtteranceReferringTokenMapReader().apply(refLangMapFilePath);
		final SessionReader sessionReader = new SessionReader(fullText -> refLangMap.get(Arrays.asList(fullText)));
		final File testingFile = new File(args[1]);
		LOGGER.info("Reading testing set list at \"{}\".", testingFile);
		SessionSet testingSet = new SessionSet(testingFile, sessionReader);
		LogisticModel model = new LogisticModel();
		final File trainingFile = new File(args[0]);
		LOGGER.info("Reading training set list at \"{}\".", trainingFile);
		model.train(new SessionSet(trainingFile, sessionReader));
		model.storeModel();

		final File sessionScreenshotDir = new File(args[3]);
		LOGGER.info("Will look for session screenshots underneath \"{}\".", sessionScreenshotDir);

		final File outdir = new File(args[4]);
		LOGGER.info("Will write results underneath \"{}\".", outdir);
		for (Session testing : testingSet.sessions) {

			if (Parameters.UPDATE_MODEL) {
				model.retrieveModel();
			}
			try {
				System.out.println(testing.name);
				//LogisticModel model = new LogisticModel();
				//model.train(training);
				DialogPrinter<ClassifierException> dialogPrinter = new DialogPrinter<ClassifierException>() {

					@Override
					public void print(PrintWriter pw, Session session, Round round) throws ClassifierException {
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
				final File sessionResultsDir = new File(outdir, testing.name);
				//noinspection ResultOfMethodCallIgnored
				sessionResultsDir.mkdirs();
				final File dialogResultsFile = new File(sessionResultsDir, "dialog.html");
				LOGGER.info("Writing dialog results for session \"{}\" to \"{}\".", testing.name, dialogResultsFile);
				writeDialog(dialogResultsFile, testing, dialogPrinter, sessionScreenshotDir);

				//System.exit(0);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		//});
	}

	public interface DialogPrinter<E extends Exception> {

		void print(PrintWriter pw, Session session, Round round) throws E;

	}

	public static <E extends Exception> void writeDialog(File outFile, Session session, DialogPrinter<E> dialogPrinter, File sessionScreenshotDir) throws FileNotFoundException, E {
		try(PrintWriter pw = new PrintWriter(outFile)) {
			pw.println("<table border=\"1\">");
			int roundn = -1;
			for (Round round : session.rounds) {
				pw.println("<tr><td valign=\"top\">" + (round.n) + "</td>");
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
				final File sessionDir = new File(sessionScreenshotDir, session.name);
				File[] fa = new File(sessionDir, "screenshots").listFiles((file) -> {
					return file.getName().startsWith(filen);
				});
				if (fa.length > 0) {
					int size = 10;
					int left = (int) (round.target.posy * 200);
					int top = (int) (round.target.posx * 170) + size;
					left -= size / 2;
					top -= size / 2;
					pw.println("<div style=\"border:1px solid red;position:relative;left:" + left + "px;top:" + top + "px;width:10px;height:10px\"></div>");
					pw.println("<img width=\"200\" src=\"screenshots/" + fa[0].getName() + "\">");
				}
				pw.println("</td>");
				//pw.println("<td>" + model.targetRank(round) + "</td>");
				pw.println("</tr>");
			}
			pw.println("</table>");
		}
	}
	
	public static String getHTMLColorString(double score) {	
	    return TestColor.getHTMLColorString(Color.getHSBColor((float)score * 0.32f, 1f, 1f));     
	}
	
	public static String getHTMLColorString(double score, double weight) {	
	    return TestColor.getHTMLColorString(Color.getHSBColor((float)score * 0.32f, (float)weight, 1f));     
	}
	
}
