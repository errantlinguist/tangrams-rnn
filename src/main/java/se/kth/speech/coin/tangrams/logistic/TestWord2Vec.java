package se.kth.speech.coin.tangrams.logistic;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;

import org.deeplearning4j.models.embeddings.loader.WordVectorSerializer;
import org.deeplearning4j.models.embeddings.wordvectors.WordVectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TestWord2Vec {

	private static final Logger LOGGER = LoggerFactory.getLogger(TestWord2Vec.class);

	public static void main(String[] args) throws IOException {
		if (args.length != 1) {
			throw new IllegalArgumentException(String.format("Usage: %s <word2VecData>", TestWord2Vec.class.getName()));
		}
		final File word2VecData = new File(args[0]);
		LOGGER.info("Will read word2vec data at \"{}\".", word2VecData);
		WordVectors wordVectors = WordVectorSerializer.loadTxtVectors(word2VecData);
		System.out.println("Ready");
		String line;
		try (BufferedReader br = new BufferedReader(new InputStreamReader(System.in))) {
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
}

