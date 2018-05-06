package se.kth.speech.coin.tangrams.rnn.weights_discr;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Locale;

import org.deeplearning4j.nn.multilayer.MultiLayerNetwork;
import org.deeplearning4j.util.ModelSerializer;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.kth.speech.coin.tangrams.data.*;
import se.kth.speech.coin.tangrams.logistic.LogisticModel;
import se.kth.speech.coin.tangrams.logistic.PredictionException;
import se.kth.speech.coin.tangrams.logistic.TestDialog;
import se.kth.speech.coin.tangrams.logistic.TestDialog.DialogPrinter;
import se.kth.speech.coin.tangrams.logistic.TrainingException;
import se.kth.speech.coin.tangrams.rnn.WordEncoder;

public class TestDialogWeights {

	private static final Logger LOGGER = LoggerFactory.getLogger(TestDialogWeights.class);

	public static void main(String[] args) throws IOException, PredictionException, TrainingException {
		if (args.length != 4) {
			throw new IllegalArgumentException(String.format("Usage: %s <dataDir> <featDir> <modelDir> <sessionScreenshotDir>", TestDialogWeights.class.getName()));
		}
		final File dataDir = new File(args[0]);
		LOGGER.info("Data dir: {}", dataDir);
		final File featDir = new File(args[1]);
		LOGGER.info("Feature dir: {}", featDir);
		final File modelDir = new File(args[2]);
		LOGGER.info("Model dir: {}", modelDir);
		final File sessionScreenshotDir = new File(args[3]);
		LOGGER.info("Will look for session screenshots underneath \"{}\".", sessionScreenshotDir);


		MultiLayerNetwork net = ModelSerializer.restoreMultiLayerNetwork(new File(modelDir, "model-100.net"));
		SessionSet testingSet = new SessionSet(new File(dataDir, "testing.txt"));
		WordEncoder encoder = new WordEncoder(new File(modelDir, "words.txt"));
		LogisticModel logisticModel = new LogisticModel();
		RnnModel rnnModel = new RnnModel(net, encoder, logisticModel);
		logisticModel.train(new SessionSet(new File(dataDir, "training.txt")));
		DialogPrinter<PredictionException> dialogPrinter = new DialogPrinter<PredictionException>() {
			@Override
			public void print(PrintWriter pw, Session session, Round round) throws PredictionException {
				INDArray output = rnnModel.score(round);
				int i = 0;
				for (Utterance utt : round.utts) {
					pw.println("<div>");
					pw.println(utt.speaker + ": ");
					for (String word : utt.getNormalizedWords()) {
						double rnnscore = output.getDouble(i++, 0, 0);
						double score = 0;
						score = logisticModel.score(word, round.target);

						if (word.equals("red")) {
							System.out.println(rnnscore);
							System.out.println(round.prettyDialog(true));
						}
						pw.println(String.format(Locale.US, "<span style=\"color:%s\">%s</span> (%.2f) ", TestDialog.getHTMLColorString(score), word, rnnscore));
					}
					pw.println("</div>");
				}
			}
		};
		for (Session session : testingSet.sessions) {
			System.out.println(session.name);
			final File sessionOutdir = new File(dataDir, session.name);
			TestDialog.writeDialog(new File(sessionOutdir, "rnn.html"), session, dialogPrinter, sessionScreenshotDir);
		}
	}
	
}
