package tangram.rnn.weights_discr;

import java.io.*;

import org.apache.commons.math3.stat.descriptive.moment.Mean;

import tangram.data.*;
import tangram.logistic.LogisticModel;
import tangram.rnn.WordEncoder;

public class MakeFeatures {

	public static String dataDir = "d:/data/tangram";
	public static String featDir = "d:/data/tangrams-rnn-wd";
	public static String modelDir = "rnn_weight_discr";
	
	public static int rnnVocabPrune = 20;
	
	static int datan = 0;

	public static void main(String[] args) throws Exception {
		Parameters.WEIGHT_BY_FREQ = true;
		Parameters.WEIGHT_BY_POWER = false;
		new File(featDir).mkdirs();
		new File(modelDir).mkdirs();
		SessionSet set = new SessionSet(new File(dataDir, "training.txt"));
		Vocabulary vocab = new RoundSet(set).getNormalizedVocabulary();
		vocab.prune(rnnVocabPrune);
		System.out.println("Vocabulary size: " + vocab.getSize());
		WordEncoder wordEncoder = new WordEncoder(vocab, true);
		wordEncoder.save(new File(modelDir, "words.txt"));
		vocab.save(new File(modelDir, "vocab.txt"));
		set.crossValidate((training,testing)-> {
			try { 
				System.out.println(testing.name);
				LogisticModel logisticModel = new LogisticModel();
				logisticModel.train(training);
				for (Round round : testing.rounds) {
					datan++;
					PrintWriter feat = new PrintWriter(new File(featDir, datan + "f.csv"));
					PrintWriter label = new PrintWriter(new File(featDir, datan + "l.csv"));
					for (Utterance utt : round.utts) {
						for (String word : utt.getNormalizedWords()) {
							feat.print(wordEncoder.getEncoding(word));
							feat.print(";");
							feat.print(utt.isGiver ? "1" : "0");
							feat.print(";");
							feat.print(logisticModel.freq(word));
							feat.println();
							Mean mean = new Mean();
							for (Referent ref : round.referents) {
								if (ref.target)
									continue;
								mean.increment(logisticModel.score(word, ref));
							}
							double discr = logisticModel.score(word, round.target) - mean.getResult();
							//System.out.println(word + " " + discr);
							label.println(discr);
						}
					}
					feat.close();
					label.close();
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		});
		System.out.println(datan);
		Training.run(datan);
	}

}

