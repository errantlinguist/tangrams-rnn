package se.kth.speech.coin.tangrams.rnn.weights_discr;

import java.io.*;

import org.apache.commons.math3.stat.descriptive.moment.Mean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.kth.speech.coin.tangrams.data.*;
import se.kth.speech.coin.tangrams.logistic.LogisticModel;
import se.kth.speech.coin.tangrams.logistic.PredictionException;
import se.kth.speech.coin.tangrams.logistic.TrainingException;
import se.kth.speech.coin.tangrams.rnn.WordEncoder;

public class MakeFeatures {

	private static final Logger LOGGER = LoggerFactory.getLogger(MakeFeatures.class);

//	public static String dataDir = "d:/data/tangram";
//	public static String featDir = "d:/data/tangrams-rnn-wd";
//	public static String modelDir = "rnn_weight_discr";
	
	public static int rnnVocabPrune = 20;
	
	static int datan = 0;

	public static void main(String[] args) throws IOException, InterruptedException {
		if (args.length != 3) {
			throw new IllegalArgumentException(String.format("Usage: %s <dataDir> <featDir> <modelDir>", MakeFeatures.class.getName()));
		}
		final File dataDir = new File(args[0]);
		LOGGER.info("Data dir: {}", dataDir);
		final File featDir = new File(args[1]);
		LOGGER.info("Feature dir: {}", featDir);
		featDir.mkdirs();
		final File modelDir = new File(args[2]);
		LOGGER.info("Model dir: {}", modelDir);
		modelDir.mkdirs();

		Parameters.WEIGHT_BY_FREQ = true;
		Parameters.WEIGHT_BY_POWER = false;

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
			} catch (FileNotFoundException | PredictionException | TrainingException e) {
				throw new RuntimeException(e);
			}
		});
		System.out.println(datan);
		Training.run(datan, featDir, modelDir);
	}

}

