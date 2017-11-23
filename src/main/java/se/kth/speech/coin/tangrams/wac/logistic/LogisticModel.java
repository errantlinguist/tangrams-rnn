/*
 * 	Copyright 2017 Todd Shore
 *
 *	Licensed under the Apache License, Version 2.0 (the "License");
 *	you may not use this file except in compliance with the License.
 *	You may obtain a copy of the License at
 *
 *		http://www.apache.org/licenses/LICENSE-2.0
 *
 *	Unless required by applicable law or agreed to in writing, software
 *	distributed under the License is distributed on an "AS IS" BASIS,
 *	WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *	See the License for the specific language governing permissions and
 *	limitations under the License.
 */
package se.kth.speech.coin.tangrams.wac.logistic;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Executor;
import java.util.concurrent.ForkJoinPool;
import java.util.function.Supplier;
import java.util.stream.Stream;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.math3.stat.descriptive.moment.Mean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import se.kth.speech.coin.tangrams.wac.data.Parameters;
import se.kth.speech.coin.tangrams.wac.data.Referent;
import se.kth.speech.coin.tangrams.wac.data.Round;
import se.kth.speech.coin.tangrams.wac.data.RoundSet;
import se.kth.speech.coin.tangrams.wac.data.Session;
import se.kth.speech.coin.tangrams.wac.data.SessionSet;
import se.kth.speech.coin.tangrams.wac.data.SessionSetReader;
import se.kth.speech.coin.tangrams.wac.data.Vocabulary;
import weka.classifiers.functions.Logistic;
import weka.core.Attribute;
import weka.core.DenseInstance;
import weka.core.Instance;
import weka.core.Instances;

public class LogisticModel {
	
	private class WordClassifierTrainer implements Supplier<Entry<String,Logistic>> {

		private final String word;

		private final double weight;

		private final Supplier<? extends Collection<Referent>> exampleSupplier;

		private WordClassifierTrainer(final String word, final Supplier<? extends Collection<Referent>> exampleSupplier, final double weight) {
			this.word = word;
			this.exampleSupplier = exampleSupplier;
			this.weight = weight;
		}

		@Override
		public Entry<String,Logistic> get() {
			Logistic logistic = new Logistic();
			final Collection<Referent> examples = exampleSupplier.get();
			Instances dataset = new Instances("Dataset", atts, examples.size());
			dataset.setClass(TARGET);
			
			for (Referent ref : examples) {
				Instance instance = toInstance(ref);
				double totalWeight = weight * (ref.isTarget() ? 19 : 1);
				instance.setWeight(totalWeight);
				dataset.add(instance);
			}
			
			try {
				logistic.buildClassifier(dataset);
			} catch (Exception e) {
				throw new WordClassifierTrainingException(word, e);
			}
			
			return Pair.of(word, logistic);
		}

	}
	
	private static final Logger LOGGER = LoggerFactory.getLogger(LogisticModel.class);

	private final ConcurrentMap<String, Logistic> wordModels;

	private Attribute SHAPE;
	private Attribute SIZE;
	private Attribute GREEN;
	private Attribute RED;
	private Attribute BLUE;
	private Attribute HUE;
	private Attribute POSX;
	private Attribute POSY;
	private Attribute MIDX;
	private Attribute MIDY;
	private Attribute TARGET;

	private ArrayList<Attribute> atts;

	private Logistic discountModel;

	private RoundSet trainingSet;

	private Vocabulary vocab;

	private final Executor asynchronousJobExecutor;

	public LogisticModel() {
		this(ForkJoinPool.commonPool(), 1000);
	}

	public LogisticModel(final Executor asynchronousJobExecutor, final int estimatedWordClassCount) {
		this.asynchronousJobExecutor = asynchronousJobExecutor;
		wordModels = new ConcurrentHashMap<>(estimatedWordClassCount);
	}

	public Vocabulary getVocabulary() {
		return vocab;
	}
	
	/**
	 * Trains the word models using all data from a SessionSet
	 */
	void train(SessionSet set) {
		final CompletableFuture<Void> trainingJob = trainAsynchronously(set);
		trainingJob.join();
	}
	
	private static final List<String> REFERENT_CLASSIFICATION_VALUES = Arrays.asList(Stream.of(Boolean.TRUE, Boolean.FALSE).map(Object::toString).map(String::intern).toArray(String[]::new));
	
	/**
	 * Trains the word models using all data from a SessionSet
	 */
	CompletableFuture<Void> trainAsynchronously(SessionSet set) {

		trainingSet = new RoundSet(set);
		vocab = trainingSet.getVocabulary();
		vocab.prune(Parameters.DISCOUNT);

		atts = new ArrayList<>();

		atts.add(SHAPE = new Attribute("shape", new ArrayList<String>(Referent.getShapes())));
		atts.add(SIZE = new Attribute("size"));
		atts.add(RED = new Attribute("red"));
		atts.add(GREEN = new Attribute("green"));
		atts.add(BLUE = new Attribute("blue"));
		// atts.add(HUE = new Attribute("hue"));

		atts.add(POSX = new Attribute("posx"));
		atts.add(POSY = new Attribute("posy"));

		atts.add(MIDX = new Attribute("midx"));
		atts.add(MIDY = new Attribute("midy"));
		atts.add(TARGET = new Attribute("target", REFERENT_CLASSIFICATION_VALUES));

		return trainAsynchronously(vocab.getWords(), 1.0);
	}

	/**
	 * Updates (trains) the models with the new round
	 */
	private void updateModel(Round round) {
		trainingSet.getRounds().add(round);
		Vocabulary oldVocab = vocab;
		vocab = trainingSet.getVocabulary();
		vocab.prune(Parameters.DISCOUNT);
		train(vocab.getUpdatedWordsSince(oldVocab), Parameters.UPDATE_WEIGHT);
	}
	
	/**
	 * Trains models for the specified words
	 */
	private void train(List<String> words, double weight) {
		final CompletableFuture<Void> trainingJob = trainAsynchronously(words, weight);
		trainingJob.join();
	}

	/**
	 * Trains models for the specified words
	 */
	private CompletableFuture<Void> trainAsynchronously(List<String> words, double weight) {

		// System.out.println("Training " + words);

		// Train a model for each word
		final Stream<CompletableFuture<Void>> wordClassifierTrainingJobs = words.stream().map(word -> CompletableFuture.supplyAsync(new WordClassifierTrainer(word, () -> trainingSet.getExamples(word), weight), asynchronousJobExecutor).thenAccept(wordClassifier -> wordModels.put(wordClassifier.getKey(), wordClassifier.getValue())));
		// Train the discount model
		final CompletableFuture<Void> discountClassifierTrainingJob = CompletableFuture.supplyAsync(new WordClassifierTrainer("__OUT_OF_VOCABULARY__", () -> trainingSet.getDiscountExamples(vocab.getWords()), weight), asynchronousJobExecutor).thenAccept(wordClassifier -> discountModel = wordClassifier.getValue());
		return CompletableFuture.allOf(Stream.concat(wordClassifierTrainingJobs, Stream.of(discountClassifierTrainingJob)).toArray(CompletableFuture[]::new));
	}

	public Instance toInstance(Referent ref) {
		DenseInstance instance = new DenseInstance(atts.size());
		instance.setValue(SHAPE, ref.getShape());
		instance.setValue(SIZE, ref.getSize());
		instance.setValue(RED, ref.getRed());
		instance.setValue(GREEN, ref.getGreen());
		instance.setValue(BLUE, ref.getBlue());
		// instance.setValue(HUE, ref.hue);
		instance.setValue(POSX, ref.getPositionX());
		instance.setValue(POSY, ref.getPositionY());
		instance.setValue(MIDX, ref.getMidX());
		instance.setValue(MIDY, ref.getMidY());

		instance.setValue(TARGET, Boolean.toString(ref.isTarget()));
		return instance;
	}

	public double score(String word, Instance inst) throws Exception {
		Logistic model = wordModels.getOrDefault(word, discountModel);
		double[] dist = model.distributionForInstance(inst);
		return dist[0];
	}

	public double score(String word, Referent ref) throws Exception {
		return score(word, toInstance(ref));
	}

	/**
	 * Returns a ranking of the referents in a round
	 */
	public List<Referent> rank(Round round) throws Exception {
		final Map<Referent, Double> scores = new HashMap<>();
		for (Referent ref : round.getReferents()) {
			Instance inst = toInstance(ref);
			Mean mean = new Mean();
			for (String word : round.getWords()) {
				double score = score(word, inst);
				if (Parameters.WEIGHT_BY_FREQ)
					score *= Math.log10(vocab.getCount(word, 3));
				mean.increment(score);
			}
			scores.put(ref, mean.getResult());
		}
		List<Referent> ranking = new ArrayList<>(round.getReferents());
		ranking.sort(new Comparator<Referent>() {
			@Override
			public int compare(Referent o1, Referent o2) {
				return scores.get(o2).compareTo(scores.get(o1));
			}
		});
		return ranking;
	}

	/**
	 * Returns the rank of the target referent in a round
	 */
	private int targetRank(Round round) throws Exception {
		int rank = 0;
		for (Referent ref : rank(round)) {
			rank++;
			if (ref.isTarget())
				return rank;
		}
		return rank;
	}

	/**
	 * Evaluates a SessionSet and returns the mean rank
	 */
	private double eval(SessionSet set) throws Exception {
		Mean mean = new Mean();
		for (Session session : set.getSessions()) {
			for (Round round : session.getRounds()) {
				mean.increment(targetRank(round));
				if (Parameters.UPDATE_MODEL) {
					updateModel(round);
				}
			}
		}
		return mean.getResult();
	}

	/**
	 * Performs cross validation on a SessionSet and returns the mean rank
	 */
	public static double crossValidate(SessionSet set) {
		final Mean crossMean = new Mean();
		set.crossValidate((training, testing) -> {
			try {
				LogisticModel model = new LogisticModel();
				model.train(training);
				double meanRank = model.eval(new SessionSet(testing));
				// System.out.println(testing.name + "\t" +
				// Parameters.getSetting() + "\t" + meanRank);
				crossMean.increment(meanRank);
			} catch (Exception e) {
				e.printStackTrace();
			}
		});
		return crossMean.getResult();
	}

	public static void main(String[] args) throws Exception {
		if (args.length < 1) {
			throw new IllegalArgumentException(String.format("Usage: %s INPATH", LogisticModel.class.getSimpleName()));
		} else {
			final Path inpath = Paths.get(args[0]);
			LOGGER.info("Reading sessions from \"{}\".", inpath);
			SessionSet set = new SessionSetReader().apply(inpath);
			LOGGER.info("Will run cross-validation using {} session(s).", set.size());
			LOGGER.info("Cross-validating using default parameters.");
			System.out.println("TIME" + "\t" + Parameters.getHeader() + "\t" + "SCORE");
			run(set);
			Parameters.ONLY_GIVER = true;
			LOGGER.info("Cross-validating using only instructor language.");
			run(set);
			Parameters.ONLY_GIVER = false;
			Parameters.ONLY_REFLANG = true;
			LOGGER.info("Cross-validating using only referring language.");
			run(set);
			Parameters.ONLY_GIVER = true;
			LOGGER.info("Cross-validating using only referring instructor language.");
			run(set);
			Parameters.UPDATE_MODEL = true;
			Parameters.UPDATE_WEIGHT = 1;
			LOGGER.info("Cross-validating using model which updates itself with intraction data using a weight of {} for the new data.", Parameters.UPDATE_WEIGHT);
			run(set);
			Parameters.UPDATE_WEIGHT = 5;
			LOGGER.info("Cross-validating using model which updates itself with intraction data using a weight of {} for the new data.", Parameters.UPDATE_WEIGHT);
			run(set);			
		}
	}

	private static void run(SessionSet set) {
		long t = System.currentTimeMillis();
		double score = crossValidate(set);
		t = (System.currentTimeMillis() - t) / 1000;
		System.out.println(t + "\t" + Parameters.getSetting() + "\t" + score);
	}

}
