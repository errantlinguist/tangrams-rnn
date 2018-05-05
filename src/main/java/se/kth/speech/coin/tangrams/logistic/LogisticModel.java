package se.kth.speech.coin.tangrams.logistic;

import java.util.*;

import org.apache.commons.math3.stat.descriptive.moment.StandardDeviation;
import org.apache.commons.math3.stat.descriptive.summary.Sum;

import se.kth.speech.coin.tangrams.data.Parameters;
import se.kth.speech.coin.tangrams.data.Referent;
import se.kth.speech.coin.tangrams.data.Round;
import se.kth.speech.coin.tangrams.data.RoundSet;
import se.kth.speech.coin.tangrams.data.Session;
import se.kth.speech.coin.tangrams.data.SessionSet;
import se.kth.speech.coin.tangrams.data.Vocabulary;
import weka.classifiers.functions.Logistic;
import weka.core.*;

public class LogisticModel {

	public Map<String,Logistic> wordModels = new HashMap<>();
	
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
	private Attribute MENTIONED;
	private Attribute TARGET;

	private ArrayList<Attribute> atts;

	private RoundSet trainingSet;

	protected Vocabulary vocab;
	
	protected Map<String,Double> power = new HashMap<>();

	//protected Map<String,Double> predict = new HashMap<>();
	
	private LogisticModel storedModel;
	
	public Vocabulary getVocabulary() {
		return vocab;
	}
		
	/**
	 * Trains the word models using all data from a SessionSet
	 */
	public void train(SessionSet set) throws PredictionException, TrainingException {
		
		trainingSet = new RoundSet(set);
		vocab = trainingSet.getVocabulary();
		
		atts = new ArrayList<>();
		
		atts.add(SHAPE = new Attribute("shape", new ArrayList<String>(Referent.shapes)));
		atts.add(SIZE = new Attribute("size"));
		atts.add(RED = new Attribute("red"));
		atts.add(GREEN = new Attribute("green"));
		atts.add(BLUE = new Attribute("blue"));
		
		atts.add(POSX = new Attribute("posx"));
		atts.add(POSY = new Attribute("posy"));
		
		atts.add(MIDX = new Attribute("midx"));
		atts.add(MIDY = new Attribute("midy"));
		
		//atts.add(MENTIONED = new Attribute("mentioned"));
		
		atts.add(TARGET = new Attribute("target", Arrays.asList(new String[] {"true", "false"})));
		
		train(vocab.getWords());
		//train(trainingSet.getBigramVocabulary().getWords());
	}
	
	public void storeModel() {
		storedModel = new LogisticModel();
		storedModel.vocab = this.vocab;
		storedModel.wordModels = this.wordModels;
		storedModel.trainingSet = new RoundSet(this.trainingSet.rounds);
		storedModel.power = new HashMap<>(power);
	}
	
	public void retrieveModel() {
		this.vocab = storedModel.vocab;
		this.wordModels = storedModel.wordModels;
		this.trainingSet = storedModel.trainingSet;
		this.power = storedModel.power;
	}
	
	/**
	 * Updates (trains) the models with the new round
	 */
	public void updateModel(Round round) throws PredictionException, TrainingException {
		//System.out.println("UPDATING");
		trainingSet.rounds.add(round);
		round.weight = Parameters.UPDATE_WEIGHT; 
		Vocabulary oldVocab = vocab;
		vocab = trainingSet.getVocabulary();
		// only update words with a maximum count of 200
		train(vocab.getUpdatedWordsSince(oldVocab, 200));
	}
	
	/**
	 * Trains models for the specified words
	 */
	private void train(List<String> words) throws PredictionException, TrainingException {

		//System.out.println("Training " + words);
		
		/*
		RoundSet[] parts = trainingSet.split();
		for (String word : parts[0].getVocabulary().getWords()) {
			Logistic classifier = buildClassifier(word, parts[0], 1);
			Mean mean = new Mean(); 
			for (Round round : parts[1].rounds) {
				if (round.hasWord(word)) {
					double score = score(round.target, classifier);
					mean.increment(score);
				}
			}
			predict.put(word, mean.getResult());
			System.out.println(word + " " + mean.getResult());
		}
		*/
		
		// Train a model for each word
		for (String word : words) {
			
			//System.out.println(word);
			
			//long t = System.currentTimeMillis();
			
			Logistic logistic = buildClassifier(word, trainingSet);
			
			//System.out.println(word + " in " + (System.currentTimeMillis() - t));
						
			wordModels.put(word, logistic);
			
			//t = System.currentTimeMillis();
			
			if (Parameters.WEIGHT_BY_POWER) {
				StandardDeviation stdev = new StandardDeviation();
				//int i = 0;
				for (Round round : trainingSet.rounds) {
					for (Referent ref : round.referents) {
						// We only compute the stdev for every 10th object, to speed up
						//if (i++ % 10 == 0)
						stdev.increment(score(word, ref));
					}
				}
				//System.out.println(word + " " + stdev.getResult());
				power.put(word, stdev.getResult());
			}
			//System.out.println("computed weight in " + (System.currentTimeMillis() - t));
			
		}
		
				
	}
		
	private Logistic buildClassifier(String word, RoundSet trainingSet) throws TrainingException {
		Logistic logistic = new Logistic();
		if (Parameters.USE_RIDGE)
			logistic.setRidge(100);
		Instances dataset = new Instances("Dataset", atts, 1000);
		
		/*
		List<Referent> posExl = trainingSet.getPosExamples(word);
		List<Referent> negExl = trainingSet.getNegExamples(word);
		System.out.println(word + " " + posExl.size() + " " + negExl.size());
		for (Referent ref : posExl) {
			Instance instance = toInstance(ref, true);
			double totalWeight = weight * Math.sqrt((double)negExl.size() / (double)posExl.size());
			instance.setWeight(totalWeight);
			dataset.add(instance);
		}
		for (Referent ref : negExl) {
			Instance instance = toInstance(ref, false);
			double totalWeight = weight * Math.sqrt((double)posExl.size() / (double)negExl.size());
			instance.setWeight(totalWeight);
			dataset.add(instance);
		}
		*/
		
		for (Round round : trainingSet.rounds) {
			//if ((word.contains(" ") && round.hasBigram(word)) || (!word.contains(" ") && round.hasWord(word))) {
			if (round.hasWord(word)) {
				for (Referent ref : round.referents) {
					Instance instance = toInstance(ref);
					double totalWeight = round.weight * (ref.target ? 19 : 1);
					instance.setWeight(totalWeight);
					dataset.add(instance);
				}
			}
		}
		
		dataset.setClass(TARGET);

		try {
			logistic.buildClassifier(dataset);
		} catch (Exception e) {
			throw new TrainingException(String.format("A(n) %s occurred while training a model for the word \"%s\".", e.getClass().getSimpleName(),word), e);
		}

		return logistic;
	}
	
	public DenseInstance toInstance(Referent ref) {
		DenseInstance instance = new DenseInstance(atts.size());
		instance.setValue(SHAPE, ref.shape);
		instance.setValue(SIZE, ref.size);
		instance.setValue(RED, ref.red);
		instance.setValue(GREEN, ref.green);
		instance.setValue(BLUE, ref.blue);
		//instance.setValue(HUE, ref.hue);
		instance.setValue(POSX, ref.posx);
		instance.setValue(POSY, ref.posy);
		instance.setValue(MIDX, ref.midx);
		instance.setValue(MIDY, ref.midy);
		//instance.setValue(MENTIONED, ref.mentioned);
		instance.setValue(TARGET, ref.target ? "true" : "false");
		return instance;
	}

	public DenseInstance toInstance(Referent ref, boolean target) {
		DenseInstance instance = toInstance(ref);
		instance.setValue(TARGET, target ? "true" : "false");
		return instance;
	}
	
	public double score(String word, Instance inst) throws PredictionException {
		if (wordModels.containsKey(word)) {
			Logistic model = wordModels.get(word);
			try {
				return score(inst, model);
			} catch (Exception e) {
				throw new PredictionException(String.format("A(n) %s occurred while doing prediction using the model for the word \"%s\".", e.getClass().getSimpleName(),word), e);
			}
		} else {
			return 0.5;
		}
	}
	
	private double score(Instance inst, Logistic model) throws Exception {
		double[] dist = model.distributionForInstance(inst);
		return dist[0];
	}
	
	public double score(String word, Referent ref) throws PredictionException {
		return score(word, toInstance(ref));
	}

	private double score(Referent ref, Logistic model) throws Exception {
		return score(toInstance(ref), model);
	}
	
	
	public double power(String word) {
		return power.getOrDefault(word, 0d);
	}
	
	public double freq(String word) {
		return Math.log10(vocab.getCount(word,3));
	}
	
	/**
	 * Returns a ranking of the referents in a round
	 */
	public List<Referent> rank(Round round) throws PredictionException {
		final Map<Referent,Double> scores = new HashMap<>();
		for (Referent ref : round.referents) {
			Instance inst = toInstance(ref);
			Sum sum = new Sum();
			for (String word : round.getWords()) {
				double score = score(word, inst);
				if (Parameters.WEIGHT_BY_FREQ)
					score *= Math.log10(vocab.getCount(word,3));
				if (Parameters.WEIGHT_BY_POWER)
					score *= power.getOrDefault(word, 0.0);
				//score *= predict.getOrDefault(word, 0.0);
				sum.increment(score);
			}
			scores.put(ref, sum.getResult());
		}
		List<Referent> ranking = new ArrayList<>(round.referents);
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
	public int targetRank(Round round) throws PredictionException {
		int rank = 0;
		for (Referent ref : rank(round)) {
			rank++;
			if (ref.target)
				return rank;
		}
		return rank;
	}

	/**
	 * Evaluates a SessionSet and returns the mean rank
	 */
	private Result eval(SessionSet set) throws PredictionException, TrainingException {
		Result result = new Result();
		storeModel();
		for (Session session : set.sessions) {
			Result resultR = new Result();
			for (Round round : session.rounds) {
				int rank = targetRank(round);
				resultR.increment(rank, rank == 1 ? 1d : 0d, 1d/rank);
				if (Parameters.UPDATE_MODEL)
					updateModel(round);
			}
			System.out.println(session.name + "\t" + resultR);
			result.increment(resultR);
			retrieveModel();
		}
		return result;
	}
	
	/**
	 * Performs cross validation on a SessionSet and returns the mean rank
	 */
	public static Result crossValidate(SessionSet set) {
		final Result result = new Result();
		set.crossValidate((training, testing) -> {
				LogisticModel model = new LogisticModel();
			try {
				model.train(training);
			} catch (PredictionException | TrainingException e) {
				throw new RuntimeException(String.format("A(n) occurred during training in cross-validation.", e.getClass().getSimpleName()), e);
			}
			Result resultR = null;
			try {
				resultR = model.eval(new SessionSet(testing));
			} catch (PredictionException | TrainingException e) {
				throw new RuntimeException(String.format("A(n) occurred during evaluation in cross-validation.", e.getClass().getSimpleName()), e);
			}
			System.out.println(testing.name + "\t" + resultR);
				result.increment(resultR);
		});
		return result;
	}
	
	public static Result validate(SessionSet training, SessionSet testing) throws PredictionException, TrainingException {
		LogisticModel model = new LogisticModel();
		model.train(training);
		Result result = model.eval(testing);
		return result;
	}

	public static void run(SessionSet set) {
		long t = System.currentTimeMillis();
		Result result = crossValidate(set);
		t = (System.currentTimeMillis() - t) / 1000;
		System.out.println(t + "\t" + Parameters.getSetting() + "\t" + result);
	}
	
	public static void run(SessionSet training, SessionSet testing) throws PredictionException, TrainingException {
		Result result = validate(training, testing);
		System.out.println(Parameters.getSetting() + "\t" + result);
	}

	public double range(String word, List<Referent> referents) throws PredictionException {
		double max = Double.MIN_VALUE;
		double min = Double.MAX_VALUE;
		for (Referent ref : referents) {
			double score = score(word, ref);
			min = Math.min(min, score);
			max = Math.max(max, score);
		}
		return max - min;
	}

	
}
