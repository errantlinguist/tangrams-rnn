package se.kth.speech.coin.tangrams.logistic;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.kth.speech.coin.tangrams.data.Referent;
import se.kth.speech.coin.tangrams.data.SessionReader;
import se.kth.speech.coin.tangrams.data.SessionSet;
import se.kth.speech.coin.tangrams.data.UtteranceReferringTokenMapReader;

public class TestSpace {

	private static final Logger LOGGER = LoggerFactory.getLogger(TestSpace.class);

	public static void main(String[] args) throws IOException, PredictionException, TrainingException {
		if (args.length != 3) {
			throw new IllegalArgumentException(String.format("Usage: %s <sessionDir> <refLangMapFile> <outfile>", TestSpace.class.getName()));
		}
		final Path refLangMapFilePath = Paths.get(args[1]);
		LOGGER.info("Reading referring-language map at \"{}\".", refLangMapFilePath);
		final Map<List<String>, String[]> refLangMap = new UtteranceReferringTokenMapReader().apply(refLangMapFilePath);
		final SessionReader sessionReader = new SessionReader(fullText -> refLangMap.get(Arrays.asList(fullText)));
		final File sessionDir = new File(args[0]);
		LOGGER.info("Reading sessions underneath \"{}\".", sessionDir);
		SessionSet set = new SessionSet(sessionDir, sessionReader);
		LogisticModel model = new LogisticModel();
		model.train(set);
		
		List<String> wlist = Arrays.asList("left", "right", "bottom", "top", "middle", "center", "corner");

		final File outfile = new File(args[2]);
		LOGGER.info("Writing results to \"{}\".", outfile);
		try(PrintWriter pw = new PrintWriter(outfile)) {

			pw.println("<table cellpadding=\"5\" width=\"100%\">");

			for (float y = 0f; y <= 1f; y += 0.5) {
				pw.println("<tr>");
				for (float x = 0f; x <= 1f; x += 0.5) {
					pw.println("<td style=\"border:1px solid black\">");

					Referent ref = new Referent();
					ref.setPos(y, x);

					for (String w : wlist) {
						double score = model.score(w, ref);
						pw.println("<div style=\"color:" + TestColor.getHTMLColorString(score) + "\">" + w + "</div>");
					}

					pw.println("</td>");
				}
				pw.println("</tr>");
			}

			pw.println("</table>");
		}
	
	}
	
}
