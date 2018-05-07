package se.kth.speech.coin.tangrams.logistic;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.math3.stat.descriptive.moment.Mean;
import org.apache.commons.math3.stat.descriptive.moment.StandardDeviation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.kth.speech.coin.tangrams.data.*;
import weka.classifiers.functions.Logistic;
import weka.core.Attribute;
import weka.core.DenseInstance;
import weka.core.Instance;
import weka.core.Instances;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * A copy of {@link LogisticModel} but with modifications to allow output of data required to calculate ROC curves for the models.
 *
 * @author tshore
 * @since 2018-05-05
 */
public class LogisticModelROCCurve {

	private static final Logger LOGGER = LoggerFactory.getLogger(LogisticModelROCCurve.class);

	public Map<String, Logistic> wordModels = new HashMap<>((int) Math.ceil(DatasetConstants.EXPECTED_UNIQUE_WORD_COUNT / 0.75f));

	protected Vocabulary vocab;

	protected Map<String, Double> power = new HashMap<>((int) Math.ceil(DatasetConstants.EXPECTED_UNIQUE_WORD_COUNT / 0.75f));

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
	private LogisticModelROCCurve storedModel;

	public static void main(String[] args) throws IOException {
		if (args.length != 3) {
			throw new IllegalArgumentException(String.format("Usage: %s <dataDir> <refLangMapFile> <outfile>", LogisticModelROCCurve.class.getName()));
		}


		final Path refLangMapFilePath = Paths.get(args[1]);
		LOGGER.info("Reading referring-language map at \"{}\".", refLangMapFilePath);
		final Map<List<String>, String[]> refLangMap = new UtteranceReferringTokenMapReader().apply(refLangMapFilePath);
		final SessionReader sessionReader = new SessionReader(fullText -> refLangMap.get(Arrays.asList(fullText)));
		final File dataDir = new File(args[0]);
		LOGGER.info("Reading session data underneath \"{}\".", dataDir);
		final Path outfile = Paths.get(args[2]);
		LOGGER.info("Will write results to \"{}\".", outfile);
		if (Files.exists(outfile)) {
			throw new IllegalArgumentException(String.format("Output file \"%s\" already exists.", outfile));
		} else {
			Files.createDirectories(outfile.getParent());
			final ResultWriter writer = new ResultWriter(Files.createFile(outfile));
			LogisticModelROCCurve.crossValidate(new SessionSet(dataDir, sessionReader), writer);
		}
	}

	/**
	 * Performs cross validation on a SessionSet and returns the mean rank
	 */
	public static void crossValidate(SessionSet set, Consumer<? super Map<String, List<ROCCurvePrediction>>> cvResultConsumer) {
		set.crossValidate((training, testing) -> {
			LogisticModelROCCurve model = new LogisticModelROCCurve();
			try {
				model.train(training);
			} catch (PredictionException | TrainingException e) {
				throw new RuntimeException(String.format("A(n) %s occurred during training in cross-validation.", e.getClass().getSimpleName()), e);
			}
			Map<String, List<ROCCurvePrediction>> resultR;
			try {
				resultR = model.eval(new SessionSet(testing));
			} catch (PredictionException | TrainingException e) {
				throw new RuntimeException(String.format("A(n) %s occurred during evaluation in cross-validation.", e.getClass().getSimpleName()), e);
			}
			cvResultConsumer.accept(resultR);
		});
	}

	//protected Map<String,Double> predict = new HashMap<>((int) Math.ceil(DatasetConstants.EXPECTED_UNIQUE_WORD_COUNT / 0.75f));

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

		atts.add(SHAPE = new Attribute("shape", new ArrayList<>(Referent.shapes)));
		atts.add(SIZE = new Attribute("size"));
		atts.add(RED = new Attribute("red"));
		atts.add(GREEN = new Attribute("green"));
		atts.add(BLUE = new Attribute("blue"));

		atts.add(POSX = new Attribute("posx"));
		atts.add(POSY = new Attribute("posy"));

		atts.add(MIDX = new Attribute("midx"));
		atts.add(MIDY = new Attribute("midy"));

		//atts.add(MENTIONED = new Attribute("mentioned"));

		atts.add(TARGET = new Attribute("target", Arrays.asList("true", "false")));

		train(vocab.getWords());
		//train(trainingSet.getBigramVocabulary().getWords());
	}

	public void storeModel() {
		storedModel = new LogisticModelROCCurve();
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
		assert words.stream().distinct().count() == words.size();
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
			logistic.setRidge(Parameters.RIDGE);
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
					double totalWeight = round.weight * (ref.isTarget() ? 19 : 1);
					instance.setWeight(totalWeight);
					dataset.add(instance);
				}
			}
		}

		dataset.setClass(TARGET);
		try {
			logistic.buildClassifier(dataset);
		} catch (Exception e) {
			throw new TrainingException(e);
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
		instance.setValue(TARGET, ref.isTarget() ? "true" : "false");
		return instance;
	}

	public double score(String word, Instance inst) throws PredictionException {
		if (wordModels.containsKey(word)) {
			Logistic model = wordModels.get(word);
			try {
				return score(inst, model);
			} catch (Exception e) {
				throw new PredictionException(String.format("A(n) %s occurred while doing prediction using the model for the word \"%s\".", e.getClass().getSimpleName(), word), e);
			}
		} else {
			return 0.5;
		}
	}

	private static double score(Instance inst, Logistic model) throws Exception {
		double[] dist = model.distributionForInstance(inst);
		return dist[0];
	}

	public double score(String word, Referent ref) throws PredictionException {
		return score(word, toInstance(ref));
	}

	public double power(String word) {
		return power.getOrDefault(word, 0d);
	}

	public double freq(String word) {
		return Math.log10(vocab.getCount(word, Parameters.DISCOUNT));
	}

	private double weightedScore(String word, Instance inst) throws PredictionException {
		double score = score(word, inst);
		if (Parameters.WEIGHT_BY_FREQ)
			score *= Math.log10(vocab.getCount(word, Parameters.DISCOUNT));
		if (Parameters.WEIGHT_BY_POWER)
			score *= power.getOrDefault(word, 0.0);
		//score *= predict.getOrDefault(word, 0.0);
		return score;
	}

	private static class ReferentInstance {

		private final Instance inst;

		private final boolean isTarget;

		private ReferentInstance(Instance inst, boolean isTarget) {
			this.inst = inst;
			this.isTarget = isTarget;
		}
	}

	/**
	 * Returns the rank of the target referent in a round
	 */
	public static int targetRank(List<ReferentInstance> refInsts) {
		int rank = 0;
		for (ReferentInstance refInst : refInsts) {
			rank++;
			if (refInst.isTarget)
				return rank;
		}
		return rank;
	}

	/**
	 * Returns the rank of the target referent in a round
	 */
	public static int targetRank(final Map<ReferentInstance, Double> scores) {
		final Comparator<Map.Entry<ReferentInstance, Double>> scoreComparator = Comparator.comparing(entry -> -entry.getValue());
		final List<ReferentInstance> sortedRefInsts = scores.entrySet().stream().sorted(scoreComparator).map(Map.Entry::getKey).collect(Collectors.toList());
		return targetRank(sortedRefInsts);
	}

	private List<ROCCurvePrediction> predict(Round round, final Map<String, Integer> wordOccurrences) throws PredictionException {
		//final List<Referent> posRefs = Arrays.asList(round.referents.stream().filter(Referent::isTarget).toArray(Referent[]::new));
		final ReferentInstance[] refInsts = round.referents.stream().map(ref -> new ReferentInstance(toInstance(ref), ref.isTarget())).toArray(ReferentInstance[]::new);
		//final ReferentInstance[] negInsts = round.referents.stream().filter(ref -> !ref.isTarget()).map(this::toInstance).toArray(Instance[]::new);
		final double meanTargetMentions = round.referents.stream().mapToInt(Referent::getMentioned).average().orElse(0.0);

		final Collection<SpeakerToken> speakerTokens = round.getSpeakerWords();
		final List<ROCCurvePrediction> result = new ArrayList<>(speakerTokens.size());
		for (final SpeakerToken speakerToken : speakerTokens) {
			final String word = speakerToken.getToken();
			final boolean isInstructor = speakerToken.isInstructor();
			final Logistic model = wordModels.get(word);
			if (model != null) {
				final Integer wordOccurrence = wordOccurrences.compute(word, (w, oldCount) -> {
					final Integer newCount;
					if (oldCount == null) {
						newCount = 1;
					} else {
						newCount = oldCount + 1;
					}
					return newCount;
				});

				final Map<ReferentInstance, Double> scores = new HashMap<>((int) Math.ceil(refInsts.length / 0.75f));
				final Mean meanPosScore = new Mean();
				final Mean meanNegScore = new Mean();
				for (final ReferentInstance refInst : refInsts) {
					final double score;
					try {
						score = score(refInst.inst, model);
					} catch (Exception e) {
						throw new PredictionException(String.format("A(n) %s occurred while doing prediction using the model for the word \"%s\".", e.getClass().getSimpleName(), word), e);
					}
					scores.put(refInst, score);
					if (refInst.isTarget) {
						meanPosScore.increment(score);
					} else {
						meanNegScore.increment(score);
					}
				}
				final int rank = targetRank(scores);
				result.add(new ROCCurvePrediction(round.n, isInstructor, word, wordOccurrence, meanPosScore.getResult(), meanNegScore.getResult(), meanTargetMentions, rank));
			}
		}
		return result;
	}

	/**
	 * Evaluates a SessionSet and returns the mean rank
	 */
	private Map<String, List<ROCCurvePrediction>> eval(SessionSet set) throws PredictionException, TrainingException {
		final Collection<Session> sessions = set.sessions;
		final Map<String, List<ROCCurvePrediction>> result = sessions.stream().collect(Collectors.toMap(Session::getName, sess -> new ArrayList<>()));
		storeModel();
		for (Session session : sessions) {
			final List<ROCCurvePrediction> resultR = result.get(session.getName());
			final Map<String, Integer> wordOccurrences = new HashMap<>((int) Math.ceil(DatasetConstants.EXPECTED_UNIQUE_WORD_COUNT / 0.75f));
			for (Round round : session.rounds) {
				List<ROCCurvePrediction> predictions = predict(round, wordOccurrences);
				resultR.addAll(predictions);
				if (Parameters.UPDATE_MODEL)
					updateModel(round);
			}
			retrieveModel();
		}
		return result;
	}

	private static class ResultWriter implements Consumer<Map<String, List<ROCCurvePrediction>>> {

		private static final CSVFormat FORMAT = CSVFormat.TDF;

		private final Path outfile;

		private final Lock writeLock = new ReentrantLock();

		private final AtomicInteger callCount;

		private ResultWriter(Path outfile) {
			this.outfile = outfile;
			this.callCount = new AtomicInteger(0);
		}

		@Override
		public void accept(Map<String, List<ROCCurvePrediction>> cvResults) {
			final int callNo = callCount.incrementAndGet();
			writeLock.lock();
			try (final CSVPrinter csvPrinter = openFile(callNo)) {
				for (Map.Entry<String, List<ROCCurvePrediction>> cvResult : cvResults.entrySet()) {
					final String sessionName = cvResult.getKey();
					final List<ROCCurvePrediction> preds = cvResult.getValue();
					for (ROCCurvePrediction pred : preds) {
						csvPrinter.printRecord(callNo, sessionName, pred.getRound(), pred.isInstructor(), pred.getWord(), pred.getNthWordOccurrence(), pred.getTargetMentioned(), pred.getProbTarget(), pred.getProbOthers(), pred.getRank());
					}
				}
			} catch (IOException e) {
				throw new UncheckedIOException(e);
			} finally {
				writeLock.unlock();
			}
		}

		private CSVPrinter openFile(int callNo) throws IOException {
			final CSVPrinter result;
			if (callNo > 1) {
				result = openFileAppending();
			} else {
				result = openFileInitial();
			}
			return result;
		}

		private CSVPrinter openFileInitial() throws IOException {
			return FORMAT.withHeader("CV_ITER", "EVAL_SESSION", "ROUND", "IS_INSTRUCTOR", "WORD", "WORD_OCCURRENCE", "MENTIONED", "PROB_TARGET", "PROB_OTHERS", "RANK").print(Files.newBufferedWriter(outfile, StandardOpenOption.CREATE));
		}

		private CSVPrinter openFileAppending() throws IOException {
			return FORMAT.print(Files.newBufferedWriter(outfile, StandardOpenOption.APPEND));
		}
	}

}
