package tangram.logistic;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.apache.commons.math3.stat.descriptive.moment.Mean;
import org.deeplearning4j.models.embeddings.loader.WordVectorSerializer;
import org.deeplearning4j.models.embeddings.wordvectors.WordVectors;

import tangram.data.*;

public class AnalyzeWord2Vec {
	
	private LogisticModel model;
	private WordVectors wordVectors;

	public AnalyzeWord2Vec() throws Exception {
		Parameters.WEIGHT_BY_FREQ = true;
		Parameters.WEIGHT_BY_POWER = true;
		wordVectors = WordVectorSerializer.loadTxtVectors(new File("C:/Dropbox/dev/Chatbot-RNN/word2vec/glove.6B.50d.txt"));
		model = new LogisticModel();
		model.train(new SessionSet(new File("C:/data/tangram/training.txt")));
		for (String word : new ArrayList<>(model.vocab.dict.keySet())) {
			if (!wordVectors.hasWord(word))
				model.vocab.dict.remove(word);
		}
		PrintWriter pw = new PrintWriter("word_analysis.tsv");
		//pw.println("word\tcount\tpower\tweight");
		for (String word : model.vocab.dict.keySet()) {
			List<String> closest = getClosest(word);
			pw.println(word + "\t" + weight(word) + "\t" + closest + "\t" + avgWeight(closest)); 
		}
	}
	
	private double avgWeight(List<String> closest) {
		Mean mean = new Mean();
		for (String word : closest) {
			mean.increment(weight(word));
		}
		return mean.getResult();
	}

	private List<String> getClosest(String word) {
		List<String> list = new ArrayList<>();
		list.addAll(model.vocab.dict.keySet());
		Collections.sort(list, new Comparator<String>() {
			@Override
			public int compare(String arg0, String arg1) {
				return wordVectors.similarity(arg0, word) < wordVectors.similarity(arg1, word) ? 1 : -1;
			}
			
		});
		return list.subList(1, 6);
	}

	public static void main(String[] args) throws Exception {
		new AnalyzeWord2Vec();
	}
	
	private double weight(String word) {
		return Math.log10(model.vocab.getCount(word, 0)) * model.power.get(word);
	}

}
