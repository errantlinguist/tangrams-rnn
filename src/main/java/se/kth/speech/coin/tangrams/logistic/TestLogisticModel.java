package se.kth.speech.coin.tangrams.logistic;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.kth.speech.coin.tangrams.data.Parameters;
import se.kth.speech.coin.tangrams.data.SessionReader;
import se.kth.speech.coin.tangrams.data.SessionSet;
import se.kth.speech.coin.tangrams.data.UtteranceReferringTokenMapReader;

public class TestLogisticModel {

	private static final Logger LOGGER = LoggerFactory.getLogger(TestLogisticModel.class);
	
	public static void main(String[] args) throws IOException, PredictionException, TrainingException {
		if (args.length != 3) {
			throw new IllegalArgumentException(String.format("Usage: %s <trainingSetFile> <testingSetFile> <refLangMapFile>", TestLogisticModel.class.getName()));
		}


		Parameters.WEIGHT_BY_POWER = true;
		Parameters.WEIGHT_BY_FREQ = true;

		final Path refLangMapFilePath = Paths.get(args[2]);
		LOGGER.info("Reading referring-language map at \"{}\".", refLangMapFilePath);
		final Map<List<String>, String[]> refLangMap = new UtteranceReferringTokenMapReader().apply(refLangMapFilePath);
		final SessionReader sessionReader = new SessionReader(fullText -> refLangMap.get(Arrays.asList(fullText)));
		final File trainingSetFile = new File(args[0]);
		LOGGER.info("Reading training set file list at \"{}\".", trainingSetFile);
		SessionSet training = new SessionSet(trainingSetFile, sessionReader);
		final File testingSetFile = new File(args[1]);
		LOGGER.info("Reading testing set file list at \"{}\".", testingSetFile);
		SessionSet testing = new SessionSet(testingSetFile, sessionReader);
		
		LogisticModel.run(training, testing);	
		
	}
}
