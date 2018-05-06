package se.kth.speech.coin.tangrams.logistic;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

import org.apache.commons.math3.stat.descriptive.moment.Mean;
import org.deeplearning4j.models.embeddings.loader.WordVectorSerializer;
import org.deeplearning4j.models.embeddings.wordvectors.WordVectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.kth.speech.coin.tangrams.data.*;

public class AnalyzeWord2Vec {
	
	private LogisticModel model;
	private WordVectors wordVectors;

	public AnalyzeWord2Vec(File word2VecData, File trainingSetFile, SessionReader sessionReader, File outfile) throws IOException, PredictionException, TrainingException {
		Parameters.WEIGHT_BY_FREQ = true;
		Parameters.WEIGHT_BY_POWER = true;
		wordVectors = WordVectorSerializer.loadTxtVectors(word2VecData);
		model = new LogisticModel();
		model.train(new SessionSet(trainingSetFile, sessionReader));
		for (String word : new ArrayList<>(model.vocab.dict.keySet())) {
			if (!wordVectors.hasWord(word))
				model.vocab.dict.remove(word);
		}
		try (PrintWriter pw = new PrintWriter(outfile)) {
			//pw.println("word\tcount\tpower\tweight");
			for (String word : model.vocab.dict.keySet()) {
				List<String> closest = getClosest(word);
				pw.println(word + "\t" + weight(word) + "\t" + closest + "\t" + avgWeight(closest));
			}
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

	private static final Logger LOGGER = LoggerFactory.getLogger(AnalyzeWord2Vec.class);

	public static void main(String[] args) throws PredictionException, IOException, TrainingException {
		if (args.length != 4) {
			throw new IllegalArgumentException(String.format("Usage: %s <word2VecData> <trainingSetFile> <refLangMapFile> <outfile>", AnalyzeWord2Vec.class.getName()));
		}
		final File word2VecData = new File(args[0]);
		LOGGER.info("Will read word2vec data at \"{}\".", word2VecData);
		final File trainingSetFile = new File(args[1]);
		LOGGER.info("Will read training set file list at \"{}\".", trainingSetFile);
		final Path refLangMapFilePath = Paths.get(args[2]);
		LOGGER.info("Reading referring-language map at \"{}\".", refLangMapFilePath);
		final Map<List<String>, String[]> refLangMap = new UtteranceReferringTokenMapReader().apply(refLangMapFilePath);
		final SessionReader sessionReader = new SessionReader(fullText -> refLangMap.get(Arrays.asList(fullText)));
		final File outfile = new File(args[3]);
		LOGGER.info("Will write results to \"{}\".", outfile);
		new AnalyzeWord2Vec(word2VecData, trainingSetFile, sessionReader, outfile);
	}
	
	private double weight(String word) {
		return Math.log10(model.vocab.getCount(word, 0)) * model.power.get(word);
	}

}
