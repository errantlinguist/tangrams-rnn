package se.kth.speech.coin.tangrams.logistic;

import java.io.File;

import se.kth.speech.coin.tangrams.data.Parameters;
import se.kth.speech.coin.tangrams.data.SessionSet;

public class TestLogisticModel {

	
	public static void main(String[] args) throws Exception {
		SessionSet training = new SessionSet(new File("d:/data/tangram/training.txt"));
		SessionSet testing = new SessionSet(new File("d:/data/tangram/testing.txt"));

		Parameters.WEIGHT_BY_POWER = true;
		Parameters.WEIGHT_BY_FREQ = true;
		
		LogisticModel.run(training, testing);	
		
	}
}
