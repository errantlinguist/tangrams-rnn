package se.kth.speech.coin.tangrams.logistic;

import java.io.File;
import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.kth.speech.coin.tangrams.data.Parameters;
import se.kth.speech.coin.tangrams.data.SessionSet;

public class TestLogisticModel {

	private static final Logger LOGGER = LoggerFactory.getLogger(TestLogisticModel.class);
	
	public static void main(String[] args) throws IOException, PredictionException, TrainingException {
		if (args.length != 2) {
			throw new IllegalArgumentException(String.format("Usage: %s <trainingSetFile> <testingSetFile>", TestLogisticModel.class.getName()));
		}
		final File trainingSetFile = new File(args[0]);
		LOGGER.info("Reading training set file list at \"{}\".", trainingSetFile);
		SessionSet training = new SessionSet(trainingSetFile);
		final File testingSetFile = new File(args[1]);
		LOGGER.info("Reading testing set file list at \"{}\".", testingSetFile);
		SessionSet testing = new SessionSet(testingSetFile);

		Parameters.WEIGHT_BY_POWER = true;
		Parameters.WEIGHT_BY_FREQ = true;
		
		LogisticModel.run(training, testing);	
		
	}
}
