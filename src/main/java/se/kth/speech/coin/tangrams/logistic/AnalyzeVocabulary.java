package se.kth.speech.coin.tangrams.logistic;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.kth.speech.coin.tangrams.data.*;

public class AnalyzeVocabulary {

	private static final Logger LOGGER = LoggerFactory.getLogger(AnalyzeVocabulary.class);
	
	public static void main(String[] args) throws IOException, PredictionException, TrainingException {
		if (args.length != 2) {
			throw new IllegalArgumentException(String.format("Usage: %s <trainingSetFile> <outfile>", AnalyzeVocabulary.class.getName()));
		}
		final File trainingSetFile = new File(args[0]);
		LOGGER.info("Reading training set file list at \"{}\".", trainingSetFile);
		final File outfile = new File(args[1]);
		LOGGER.info("Will write results to \"{}\".", outfile);

		Parameters.WEIGHT_BY_FREQ = true;
		Parameters.WEIGHT_BY_POWER = true;

		LogisticModel model = new LogisticModel();
		model.train(new SessionSet(trainingSetFile));
		try (PrintWriter pw = new PrintWriter(outfile)) {
			pw.println("word\tcount\tpower\tweight");
			for (String word : model.vocab.dict.keySet()) {
				if (model.vocab.getCount(word, 0) > 0)
					pw.println(word + "\t" + model.vocab.getCount(word, 0) + "\t" + model.power.get(word) + "\t" +
							(Math.log10(model.vocab.getCount(word, 0)) * model.power.get(word)));
			}
		}
	}

}
