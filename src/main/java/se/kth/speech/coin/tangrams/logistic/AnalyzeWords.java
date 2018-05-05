package se.kth.speech.coin.tangrams.logistic;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;

import se.kth.speech.coin.tangrams.data.*;


public class AnalyzeWords {
	
	public static void main(String[] args) throws IOException, Exception {
		Parameters.WEIGHT_BY_FREQ = true;
		Parameters.WEIGHT_BY_POWER = true;
		SessionSet testingSet = new SessionSet(new File("C:/data/tangram/testing.txt"));
		LogisticModel model = new LogisticModel();
		model.train(new SessionSet(new File("C:/data/tangram/training.txt")));
		PrintWriter pw = new PrintWriter("word_analysis.tsv");
		pw.println("tag\tword\tgiver\tscore");
		for (Session testing : testingSet.sessions) {
			for (Round round : testing.rounds) {
				for (Utterance utt : round.utts) {
					String prevWord = "<s>";
					for (String word : utt.fullText) {
						double score = model.score(word, round.target);
						pw.println(word + "\t" + prevWord + "\t" + score);
						prevWord = word;
					}
				}
			}
		}
	}

}
