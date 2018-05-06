package se.kth.speech.coin.tangrams.logistic;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.kth.speech.coin.tangrams.data.Referent;
import se.kth.speech.coin.tangrams.data.SessionSet;

public class TestSize {

	private static final Logger LOGGER = LoggerFactory.getLogger(TestSize.class);

	public static void main(String[] args) throws IOException, PredictionException, TrainingException {
		if (args.length != 2) {
			throw new IllegalArgumentException(String.format("Usage: %s <sessionDir> <outfile>", TestSize.class.getName()));
		}
		final File sessionDir = new File(args[0]);
		LOGGER.info("Reading sessions underneath \"{}\".", sessionDir);
		SessionSet set = new SessionSet(sessionDir);
		LogisticModel model = new LogisticModel();
		model.train(set);
		
		List<String> wlist = Arrays.asList(new String[]{"large", "big", "small", "smallest"});

		final File outfile = new File(args[1]);
		LOGGER.info("Writing results to \"{}\".", outfile);
		try (PrintWriter pw = new PrintWriter(outfile)) {

			pw.println("<table>");

			for (float size = 0f; size <= 0.04f; size += 0.005) {

				Referent ref = new Referent();
				ref.size = size;

				pw.println("<tr><td>" + size + "<td>");

				for (String word : wlist) {
					double score = model.score(word, ref);
					pw.println("<td style=\"color:" + TestColor.getHTMLColorString(score) + "\">" + word + "</td>");
				}

				pw.println("</tr>");
			}

			pw.println("</table>");
		}
	
	}
	
}
