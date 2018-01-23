package tangram.rnn.weights_discr;

import java.io.File;
import java.io.PrintWriter;
import java.util.Locale;

import org.apache.commons.codec.language.bm.Lang;
import org.deeplearning4j.nn.multilayer.MultiLayerNetwork;
import org.deeplearning4j.util.ModelSerializer;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;

import tangram.data.*;
import tangram.logistic.LogisticModel;
import tangram.logistic.TestDialog;
import tangram.logistic.TestDialog.DialogPrinter;
import tangram.rnn.WordEncoder;

public class TestDialogWeights {

	public static void main(String[] args) throws Exception {
		MultiLayerNetwork net = ModelSerializer.restoreMultiLayerNetwork(new File(MakeFeatures.modelDir, "model-100.net"));
		SessionSet testingSet = new SessionSet(new File(MakeFeatures.dataDir, "testing.txt"));
		WordEncoder encoder = new WordEncoder(new File(MakeFeatures.modelDir, "words.txt"));
		LogisticModel logisticModel = new LogisticModel();
		RnnModel rnnModel = new RnnModel(net, encoder, logisticModel);
		logisticModel.train(new SessionSet(new File(MakeFeatures.dataDir, "training.txt")));
		DialogPrinter dialogPrinter = new DialogPrinter() {
			@Override
			public void print(PrintWriter pw, Session session, Round round) throws Exception {
				INDArray output = rnnModel.score(round);
				int i = 0;
				for (Utterance utt : round.utts) {
					pw.println("<div>");
					pw.println(utt.speaker + ": ");
					for (String word : utt.getNormalizedWords()) {
						double rnnscore = output.getDouble(i++, 0, 0);
						double score = logisticModel.score(word, round.target);
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
			TestDialog.writeDialog(new File(MakeFeatures.dataDir, session.name + "/rnn.html"), session, dialogPrinter);
		}
	}
	
}
