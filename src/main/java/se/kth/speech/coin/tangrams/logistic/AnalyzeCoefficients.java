package se.kth.speech.coin.tangrams.logistic;

import java.io.File;
import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.kth.speech.coin.tangrams.data.*;

public class AnalyzeCoefficients {

	private static final Logger LOGGER = LoggerFactory.getLogger(AnalyzeCoefficients.class);
	
	public static void main(String[] args) throws IOException, PredictionException, TrainingException {
		if (args.length != 1) {
			throw new IllegalArgumentException(String.format("Usage: %s <trainingSetFile> <outfile>", AnalyzeCoefficients.class.getName()));
		}
		final File trainingSetFile = new File(args[0]);
		LOGGER.info("Reading training set file list at \"{}\".", trainingSetFile);

		Parameters.WEIGHT_BY_FREQ = true;
		Parameters.WEIGHT_BY_POWER = true;

		LogisticModel model = new LogisticModel();
		model.train(new SessionSet(trainingSetFile));
		//PrintWriter pw = new PrintWriter("word_analysis.tsv");
		//pw.println("word\tcount\tpower\tweight");
		for (String word : model.vocab.dict.keySet()) {
			double[][] coeff = model.wordModels.get(word).coefficients();
			for (int i = 0; i < coeff.length; i++) {
				if (Math.abs(coeff[i][0]) > 4)
					System.out.println(word + " " + i + " " + coeff[i][0]);
			}
		}
	}

}
