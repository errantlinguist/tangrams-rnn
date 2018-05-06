package se.kth.speech.coin.tangrams.logistic;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.kth.speech.coin.tangrams.data.*;

public class AnalyzeWords {

	private static final Logger LOGGER = LoggerFactory.getLogger(AnalyzeWords.class);
	
	public static void main(String[] args) throws IOException, PredictionException, TrainingException {
		if (args.length != 3) {
			throw new IllegalArgumentException(String.format("Usage: %s <trainingSetFile> <testingSetFile> <outfile>", AnalyzeWords.class.getName()));
		}
		final File trainingSetFile = new File(args[0]);
		LOGGER.info("Reading training set file list at \"{}\".", trainingSetFile);
		final File testingSetFile = new File(args[1]);
		LOGGER.info("Reading testing set file list at \"{}\".", testingSetFile);

		Parameters.WEIGHT_BY_FREQ = true;
		Parameters.WEIGHT_BY_POWER = true;

		SessionSet testingSet = new SessionSet(testingSetFile);
		LogisticModel model = new LogisticModel();
		model.train(new SessionSet(trainingSetFile));

		final File outfile = new File(args[2]);
		LOGGER.info("Writing results to \"{}\".", outfile);
		try (PrintWriter pw = new PrintWriter(outfile)) {
			pw.println("tag\tword\tgiver\tscore");
			for (Session testing : testingSet.sessions) {
				for (Round round : testing.rounds) {
					for (Utterance utt : round.utts) {
						String prevWord = "<s>";
						for (String word : utt.fullText) {
							double score = model.score(word, round.target);
							pw.println(word + "\t" + prevWord + "\t" + score);
							prevWord = word;
						}
					}
				}
			}
		}
	}

}
