package se.kth.speech.coin.tangrams.logistic;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.kth.speech.coin.tangrams.data.*;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class TestColor {

	private static final Logger LOGGER = LoggerFactory.getLogger(TestColor.class);

	public static void main(String[] args) throws IOException, TrainingException, PredictionException {
		if (args.length != 3) {
			throw new IllegalArgumentException(String.format("Usage: %s <sessionDir> <RefLangMapFile> <outfile>", TestColor.class.getName()));
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

		List<String> wlist = Arrays.asList("red", "green", "blue", "yellow", "magenta", "pink", "orange");

		PrintWriter pw = new PrintWriter("colors.html");

		pw.println("<table>");

		for (float hue = 0f; hue < 1f; hue += 0.02) {
			Color col = Color.getHSBColor(hue, 1f, 1f);
			Referent ref = new Referent();
			ref.hue = hue;
			ref.blue = col.getBlue() / 255f;
			ref.red = col.getRed() / 255f;
			ref.green = col.getGreen() / 255f;
			pw.println("<tr><td style=\"background-color:" + getHTMLColorString(col) + ";width:100\">" + (int)(hue*1000) + "</td>");

			for (String w : wlist) {
				double score = model.score(w, ref);
				pw.println("<td style=\"color:" + getHTMLColorString(score) + "\">" + w + "</td>");
			}

			/*
			for (int i = 0; i < 6; i++) {
				float h = (float)i / 6;
				float dist = Math.min(Math.min(Math.abs(ref.hue - h), Math.abs(ref.hue - (1f+h))), Math.abs((1f+ref.hue) - h));
				float val = (float) Math.pow((1-dist), 3);
				pw.println("<td>" + val + "</td>");
			}
			*/
			
			pw.println("</tr>");
			
		}
		
		pw.println("</table>");
		
		pw.close();
	
	}
	
	public static String getHTMLColorString(double score) {
	    return getHTMLColorString(Color.getHSBColor(0f, 0f, 1f-(float)score));     
	}
	
	public static String getHTMLColorString(Color color) {
	    String red = Integer.toHexString(color.getRed());
	    String green = Integer.toHexString(color.getGreen());
	    String blue = Integer.toHexString(color.getBlue());
	    return "#" + 
	            (red.length() == 1? "0" + red : red) +
	            (green.length() == 1? "0" + green : green) +
	            (blue.length() == 1? "0" + blue : blue);        
	}
	
}
