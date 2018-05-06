package se.kth.speech.coin.tangrams.logistic;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

import org.apache.commons.math3.stat.descriptive.moment.StandardDeviation;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.kth.speech.coin.tangrams.data.*;

public class AnalyzeCategories {
	
	public Map<String,Double> colorMap = new HashMap<>();
	public Map<String,Double> shapeMap = new HashMap<>();
	public Map<String,Double> sizeMap = new HashMap<>();
	public Map<String,Double> posMap = new HashMap<>();
	private LogisticModel model;
	
	public AnalyzeCategories(LogisticModel model) throws PredictionException {
		this.model = model;
		for (String word : model.vocab.dict.keySet()) {
			Referent ref = new Referent();
			StandardDeviation stdev = new StandardDeviation();
			ref.red = 1;
			stdev.increment(model.score(word, ref));
			ref.red = 0;
			ref.green = 1;
			stdev.increment(model.score(word, ref));
			ref.green = 0;
			ref.blue = 1;
			stdev.increment(model.score(word, ref));
			ref.blue = 0;
			double colorScore = stdev.getResult();
			colorMap.put(word,colorScore);
			stdev = new StandardDeviation();
			for (String shape : Referent.shapes) {
				ref.shape = shape;
				stdev.increment(model.score(word, ref));
			}
			double shapeScore = stdev.getResult();
			shapeMap.put(word, shapeScore);
			stdev = new StandardDeviation();
			for (ref.size = 0f; ref.size <= 0.04f; ref.size += 0.02f) {
				stdev.increment(model.score(word, ref));
			}
			double sizeScore = stdev.getResult();
			sizeMap.put(word, sizeScore);
			stdev = new StandardDeviation();
			for (float x = 0f; x <= 1f; x += 0.5f) {
				for (float y = 0f; y <= 1f; y += 0.5f) {
					ref.setPos(x, y);
					stdev.increment(model.score(word, ref));
				}
			}
			double posScore = stdev.getResult();
			posMap.put(word, posScore);
			//System.out.println(word + " " + colorScore + " " + shapeScore + " " + sizeScore + " " + posScore);
		}
	}
	
	
	public AnalyzeCategories(File file) throws IOException {
		BufferedReader br = new BufferedReader(new FileReader(file));
		String line;
		while ((line = br.readLine()) != null) {
			String[] cols = line.split("\t");
			shapeMap.put(cols[0], Double.parseDouble(cols[1]));
			colorMap.put(cols[0], Double.parseDouble(cols[2]));
			sizeMap.put(cols[0], Double.parseDouble(cols[3]));
			posMap.put(cols[0], Double.parseDouble(cols[4]));
		}
	}

	private static final Logger LOGGER = LoggerFactory.getLogger(AnalyzeCategories.class);

	public static void main(String[] args) throws IOException, PredictionException, TrainingException {
		if (args.length != 3) {
			throw new IllegalArgumentException(String.format("Usage: %s <trainingSetFile> <refLangMapFile> <outfile>", AnalyzeCategories.class.getName()));
		}
		final File trainingSetFile = new File(args[0]);
		LOGGER.info("Reading training set file list at \"{}\".", trainingSetFile);
		final Path refLangMapFilePath = Paths.get(args[1]);
		LOGGER.info("Reading referring-language map at \"{}\".", refLangMapFilePath);
		final Map<List<String>, String[]> refLangMap = new UtteranceReferringTokenMapReader().apply(refLangMapFilePath);
		final SessionReader sessionReader = new SessionReader(fullText -> refLangMap.get(Arrays.asList(fullText)));

		Parameters.WEIGHT_BY_FREQ = true;
		Parameters.WEIGHT_BY_POWER = true;

		LogisticModel model = new LogisticModel();
		model.train(new SessionSet(trainingSetFile, sessionReader));
		AnalyzeCategories cat = new AnalyzeCategories(model);

		final File outfile = new File(args[2]);
		LOGGER.info("Writing results to \"{}\".", outfile);
		cat.save(outfile);
	}

	private void save(File fn) throws FileNotFoundException {
		PrintWriter pw = new PrintWriter(fn);
		for (String word : model.vocab.getWords()) {
			pw.printf("%s\t%.3f\t%.3f\t%.3f\t%.3f\n", word, shapeMap.get(word), colorMap.get(word), sizeMap.get(word), posMap.get(word));
		}
		pw.close();
	}
	
	private static double getWeight(Map<String,Double> map, List<String> words) {
		double max = Double.MIN_VALUE;
		for (String word : words) {
			Double val = map.get(word);
			if (val != null)
				max = Math.max(val, max);
		}
		return max;
	}
	
	public double getShapeWeight(List<String> words) {
		return getWeight(shapeMap, words);
	}

	public double getColorWeight(List<String> words) {
		return getWeight(colorMap, words);
	}
	
	public double getSizeWeight(List<String> words) {
		return getWeight(sizeMap, words);
	}
	
	public double getPosWeight(List<String> words) {
		return getWeight(posMap, words);
	}


}
