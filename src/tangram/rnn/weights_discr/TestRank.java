package tangram.rnn.weights_discr;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.math3.stat.descriptive.moment.Mean;
import org.deeplearning4j.nn.multilayer.MultiLayerNetwork;
import org.deeplearning4j.util.ModelSerializer;
import org.nd4j.linalg.api.ndarray.INDArray;

import tangram.data.Referent;
import tangram.data.Round;
import tangram.data.Session;
import tangram.data.SessionSet;
import tangram.data.Utterance;
import tangram.logistic.LogisticModel;
import tangram.logistic.Result;
import tangram.rnn.WordEncoder;

public class TestRank {
	
	private static class RnnLogisticModel extends LogisticModel {

		private RnnModel rnnModel;

		public RnnLogisticModel() throws IOException {
			MultiLayerNetwork net = ModelSerializer.restoreMultiLayerNetwork(new File(MakeFeatures.modelDir, "model-100.net"));
			WordEncoder encoder = new WordEncoder(new File(MakeFeatures.modelDir, "words.txt"));
			rnnModel = new RnnModel(net, encoder, this);
		}
		
		public Map<Referent, Double> scores(Round round) throws Exception {
			INDArray rnnScores = rnnModel.score(round);
			final Map<Referent,Double> scores = new HashMap<>();
			
			for (Referent ref : round.referents) {
				Mean mean = new Mean();
				int wn = 0; 
				for (Utterance utt : round.utts) {
					for (String word : utt.getNormalizedWords()) {
						double score = score(word, ref);
						double rnnscore = rnnScores.getDouble(wn++, 0, 0);
						score *= Math.pow(rnnscore, 2);
						mean.increment(score);
					}
					scores.put(ref, mean.getResult());
				}
			}
			List<Referent> ranking = new ArrayList<>(round.referents);
			ranking.sort(new Comparator<Referent>() {
				@Override
				public int compare(Referent o1, Referent o2) {
					return scores.get(o2).compareTo(scores.get(o1));
				}
			});
			return scores;
		}
	
	}

	public static void main(String[] args) throws Exception {
		RnnLogisticModel rnnLogisticModel = new RnnLogisticModel();
		rnnLogisticModel.train(new SessionSet(new File(MakeFeatures.dataDir, "training.txt")));

		Result roundMean = new Result();
		SessionSet testingSet = new SessionSet(new File(MakeFeatures.dataDir, "testing.txt"));;
		for (Session session : testingSet.sessions) {
			Result sessionMean = new Result();
			for (Round round : session.rounds) {
				sessionMean.increment(rnnLogisticModel.scores(round));
			}
			System.out.println(sessionMean);
			roundMean.increment(sessionMean);
		}
		System.out.println("---------");
		System.out.println(roundMean);
	}
	
}
