package tangram.logistic;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;

import tangram.data.*;

public class AnalyzeVocabulary {
	
	public static void main(String[] args) throws IOException, Exception {
		Parameters.WEIGHT_BY_FREQ = true;
		Parameters.WEIGHT_BY_POWER = true;
		LogisticModel model = new LogisticModel();
		model.train(new SessionSet(new File("C:/data/tangram/training.txt")));
		PrintWriter pw = new PrintWriter("word_analysis.tsv");
		pw.println("word\tcount\tpower\tweight");
		for (String word : model.vocab.dict.keySet()) {
			if (model.vocab.getCount(word, 0) > 0) 
				pw.println(word + "\t" + model.vocab.getCount(word, 0) + "\t" + model.power.get(word) + "\t" + 
			(Math.log10(model.vocab.getCount(word, 0)) * model.power.get(word)));
		}
	}

}
