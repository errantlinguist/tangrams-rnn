package se.kth.speech.coin.tangrams.rnn.weights_discr;

import org.deeplearning4j.nn.multilayer.MultiLayerNetwork;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;

import se.kth.speech.coin.tangrams.data.Round;
import se.kth.speech.coin.tangrams.data.Utterance;
import se.kth.speech.coin.tangrams.logistic.LogisticModel;
import se.kth.speech.coin.tangrams.rnn.WordEncoder;

public class RnnModel {

	MultiLayerNetwork net;
	WordEncoder encoder;
	LogisticModel model;
	
	public RnnModel(MultiLayerNetwork net, WordEncoder encoder, LogisticModel model) {
		this.net = net;
		this.encoder = encoder;
		this.model = model;
	}
	 
	public INDArray score(Round round) throws Exception {
		int nInput = encoder.size() + 2;
		INDArray input = Nd4j.zeros(round.getNormalizedTextWordCount(),nInput);
		int i = 0;
		for (Utterance utt : round.utts) {
			for (String word : utt.getNormalizedWords()) {
				input.putScalar(new int[]{i,encoder.getIndex(word)}, 1);
				if (utt.isGiver)
					input.putScalar(new int[]{i,nInput-2}, 1);
				input.putScalar(new int[]{i,nInput-1}, model.freq(word));
				//double range = model.range(word, round.referents);
				//input.putScalar(new int[]{i,nInput-1}, range);
				i++;
			}
		}
		INDArray output = net.output(input);
		return output;
	}

}
