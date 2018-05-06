package se.kth.speech.coin.tangrams.logistic;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

import org.apache.commons.math3.stat.descriptive.moment.Mean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.kth.speech.coin.tangrams.data.*;

public class WordStats {

	private static final Logger LOGGER = LoggerFactory.getLogger(WordStats.class);

	private Map<String,List<Double>> targetScores = new HashMap<>();
	private Map<String,List<Double>> offScores = new HashMap<>();
	private Map<String,List<Double>> scores = new HashMap<>();
	private Map<String,Integer> count = new HashMap<>();
	
	public void add(Round round, String word, double score, boolean target) {
		//if (target && score < 0.2) {
		//	System.out.println(word + " " + round.prettyDialog());
		//}
		if (target) {
			if (!targetScores.containsKey(word))
				targetScores.put(word, new ArrayList<>());
			targetScores.get(word).add(score);
		} else {
			if (!offScores.containsKey(word))
				offScores.put(word, new ArrayList<>());
			offScores.get(word).add(score);
		}
		if (!scores.containsKey(word))
			scores.put(word, new ArrayList<>());
		scores.get(word).add(score);
		
		count.put(word, count.getOrDefault(word, 0) + 1);
	}

	public void print() {
		for (String word : targetScores.keySet()) {
			Mean meanTarget = new Mean();
			Mean meanOff = new Mean();
			for (Double score : targetScores.get(word)) {
				meanTarget.increment(score);
			}
			if (offScores.containsKey(word)) {
				for (Double score : offScores.get(word)) {
					meanOff.increment(score);
				}
				System.out.println(word + " " + meanTarget.getResult() + " " + meanOff.getResult() + " " + count.get(word));
			}
		}
	}
	
	public static void main(String[] args) throws IOException, PredictionException, TrainingException {
		if (args.length != 2) {
			throw new IllegalArgumentException(String.format("Usage: %s <sessionDir> <refLangMapFile>", WordStats.class.getName()));
		}
		final File sessionDir = new File(args[0]);
		LOGGER.info("Reading sessions underneath \"{}\".", sessionDir);
		WordStats stats = new WordStats();
		final Path refLangMapFilePath = Paths.get(args[1]);
		LOGGER.info("Reading referring-language map at \"{}\".", refLangMapFilePath);
		final Map<List<String>, String[]> refLangMap = new UtteranceReferringTokenMapReader().apply(refLangMapFilePath);
		final SessionReader sessionReader = new SessionReader(fullText -> refLangMap.get(Arrays.asList(fullText)));
		SessionSet set = new SessionSet(sessionDir, sessionReader);
		LogisticModel model = new LogisticModel();
		model.train(set);
		Vocabulary vocab = model.getVocabulary();
		for (Round round : new RoundSet(set).rounds) {
			for (String word : round.getWords()) {
				if (vocab.has(word)) {
					for (Referent ref : round.referents) {
						double score = model.score(word, ref);
						stats.add(round, word, score, ref.isTarget());
					}
				}
			}
		}
		stats.print();
	}

}
