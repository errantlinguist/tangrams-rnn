package tangram.logistic;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;

import tangram.data.*;

public class AnalyzeCoefficients {
	
	public static void main(String[] args) throws IOException, Exception {
		Parameters.WEIGHT_BY_FREQ = true;
		Parameters.WEIGHT_BY_POWER = true;
		LogisticModel model = new LogisticModel();
		model.train(new SessionSet(new File("C:/data/tangram/training.txt")));
		//PrintWriter pw = new PrintWriter("word_analysis.tsv");
		//pw.println("word\tcount\tpower\tweight");
		for (String word : model.vocab.dict.keySet()) {
			double[][] coeff = model.wordModels.get(word).coefficients();
			for (int i = 0; i < coeff.length; i++) {
				if (Math.abs(coeff[i][0]) > 4)
					System.out.println(word + " " + i + " " + coeff[i][0]);
			}
		}
	}

}
