package se.kth.speech.coin.tangrams.logistic;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;

import org.deeplearning4j.models.embeddings.loader.WordVectorSerializer;
import org.deeplearning4j.models.embeddings.wordvectors.WordVectors;

public class TestWord2Vec {

	public static void main(String[] args) throws Exception {
		WordVectors wordVectors = WordVectorSerializer.loadTxtVectors(new File("C:/Dropbox/dev/Chatbot-RNN/word2vec/glove.6B.50d.txt"));
		System.out.println("Ready");
		String line;
		BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
		while ((line = br.readLine()) != null) {
			if (line.contains(" ")) {
				String[] parts = line.trim().split(" ");
				System.out.println(wordVectors.similarity(parts[0], parts[1]));
			} else {
				System.out.println(wordVectors.wordsNearest(line.trim(), 10));	
			}
		}
	}
}

