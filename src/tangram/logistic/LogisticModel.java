package tangram.logistic;

import java.nio.file.Paths;
import java.util.*;

import org.apache.commons.math3.stat.descriptive.moment.Mean;

import tangram.data.Parameters;
import tangram.data.Referent;
import tangram.data.Round;
import tangram.data.RoundSet;
import tangram.data.Session;
import tangram.data.SessionSet;
import tangram.data.Vocabulary;
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
	private Attribute TARGET;

	private ArrayList<Attribute> atts;

	private Logistic discountModel;

	private RoundSet trainingSet;

	private Vocabulary vocab;
	
	public Vocabulary getVocabulary() {
		return vocab;
	}
		
	/**
	 * Trains the word models using all data from a SessionSet
	 */
	void train(SessionSet set) throws Exception {
		
		trainingSet = new RoundSet(set);
		vocab = trainingSet.getVocabulary();
		vocab.prune(Parameters.DISCOUNT);
		
		atts = new ArrayList<>();
		
		atts.add(SHAPE = new Attribute("shape", new ArrayList<String>(Referent.shapes)));
		atts.add(SIZE = new Attribute("size"));
		atts.add(RED = new Attribute("red"));
		atts.add(GREEN = new Attribute("green"));
		atts.add(BLUE = new Attribute("blue"));
		//atts.add(HUE = new Attribute("hue"));
		
		atts.add(POSX = new Attribute("posx"));
		atts.add(POSY = new Attribute("posy"));
		
		atts.add(MIDX = new Attribute("midx"));
		atts.add(MIDY = new Attribute("midy"));
		atts.add(TARGET = new Attribute("target", Arrays.asList(new String[] {"true", "false"})));
		
		train(vocab.getWords(), 1.0);
	}
	
	/**
	 * Updates (trains) the models with the new round
	 */
	private void updateModel(Round round) throws Exception {
		trainingSet.rounds.add(round);
		Vocabulary oldVocab = vocab;
		vocab = trainingSet.getVocabulary();
		vocab.prune(Parameters.DISCOUNT);
		train(vocab.getUpdatedWordsSince(oldVocab), Parameters.UPDATE_WEIGHT);
	}
	
	/**
	 * Trains models for the specified words
	 */
	private void train(List<String> words, double weight) throws Exception {

		//System.out.println("Training " + words);
		
		// Train a model for each word
		for (String word : words) {
			//System.out.println(word);
			
			Logistic logistic = new Logistic();
			
			Instances dataset = new Instances("Dataset", atts, 1000);
						
			for (Referent ref : trainingSet.getExamples(word)) {
				Instance instance = toInstance(ref);
				double totalWeight = weight * (ref.target ? 19 : 1);
				instance.setWeight(totalWeight);
				dataset.add(instance);
			}
			dataset.setClass(TARGET);
			
			logistic.buildClassifier(dataset);
			
			wordModels.put(word, logistic);
		}
		
		// Train the discount model
		
		discountModel = new Logistic();
		
		Instances dataset = new Instances("Dataset", atts, 1000);
		
		for (Referent ref : trainingSet.getDiscountExamples(vocab.getWords())) {
			Instance instance = toInstance(ref);
			double totalWeight = weight * (ref.target ? 19 : 1);
			instance.setWeight(totalWeight);
			dataset.add(instance);
		}
		
		dataset.setClass(TARGET);
		
		discountModel.buildClassifier(dataset);
				
	}
		
	public Instance toInstance(Referent ref) {
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
		
		instance.setValue(TARGET, ref.target ? "true" : "false");
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
		final Map<Referent,Double> scores = new HashMap<>();
		for (Referent ref : round.referents) {
			Instance inst = toInstance(ref);
			Mean mean = new Mean();
			for (String word : round.getWords()) {
				double score = score(word, inst);
				if (Parameters.WEIGHT_BY_FREQ)
					score *= Math.log10(vocab.getCount(word,3));	
				mean.increment(score);
			}
			scores.put(ref, mean.getResult());
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
	private int targetRank(Round round) throws Exception {
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
	private double eval(SessionSet set) throws Exception {
		Mean mean = new Mean();
		for (Session session : set.sessions) {
			for (Round round : session.rounds) {
				mean.increment(targetRank(round));
				if (Parameters.UPDATE_MODEL)
					updateModel(round);
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
				//System.out.println(testing.name + "\t" + Parameters.getSetting() + "\t" + meanRank);
				crossMean.increment(meanRank);
			} catch (Exception e) {
				e.printStackTrace();
			}
		});
		return crossMean.getResult();
	}
	
	public static void main(String[] args) throws Exception {
		SessionSet set = new SessionSet(Paths.get("/home/tshore/Projects/tangrams-restricted/Data/Ready"));
		System.out.println(set.size() + " sessions");
		run(set);
		Parameters.ONLY_GIVER = true;
		run(set);
		Parameters.ONLY_GIVER = false;
		Parameters.ONLY_REFLANG = true;
		run(set);
		Parameters.ONLY_GIVER = true;
		run(set);
		Parameters.UPDATE_MODEL= true;
		Parameters.UPDATE_WEIGHT= 1;
		run(set);
		Parameters.UPDATE_WEIGHT= 5;
		run(set);
	}

	private static void run(SessionSet set) {
		long t = System.currentTimeMillis();
		double score = crossValidate(set);
		t = (System.currentTimeMillis() - t) / 1000;
		System.out.println(t + "\t" + Parameters.getSetting() + "\t" + score);
	}

	
}
