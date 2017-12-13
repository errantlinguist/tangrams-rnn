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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinTask;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import java.util.stream.DoubleStream;
import java.util.stream.Stream;

import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import it.unimi.dsi.fastutil.objects.Object2LongMaps;
import se.kth.speech.HashedCollections;
import se.kth.speech.Lists;
import se.kth.speech.NumberTypeConversions;
import se.kth.speech.coin.tangrams.wac.data.Referent;
import se.kth.speech.coin.tangrams.wac.data.Round;
import se.kth.speech.coin.tangrams.wac.data.RoundSet;
import se.kth.speech.coin.tangrams.wac.data.SessionSet;
import se.kth.speech.coin.tangrams.wac.data.Vocabulary;
import weka.classifiers.Classifier;
import weka.classifiers.functions.Logistic;
import weka.core.Attribute;
import weka.core.BatchPredictor;
import weka.core.DenseInstance;
import weka.core.Instance;
import weka.core.Instances;

public final class LogisticModel { // NO_UCD (use default)

	public static final class FeatureAttributeData {

		private static Map<ReferentFeature, Attribute> createFeatureAttrMap(final List<String> shapeUniqueValues) {
			final Map<ReferentFeature, Attribute> result = new EnumMap<>(ReferentFeature.class);
			Arrays.stream(ReferentFeature.values())
					.forEach(feature -> result.put(feature, feature.createAttribute(shapeUniqueValues)));
			return result;
		}

		private final ArrayList<Attribute> attrList;

		private final Attribute classAttr;

		private final Map<ReferentFeature, Attribute> featureAttrs;

		private FeatureAttributeData() {
			this(Arrays.asList(Referent.getShapes().stream().toArray(String[]::new)));
		}

		private FeatureAttributeData(final List<String> shapeUniqueValues) {
			this(createFeatureAttrMap(shapeUniqueValues));
		}

		private FeatureAttributeData(final Map<ReferentFeature, Attribute> featureAttrs) {
			this(featureAttrs, ReferentFeature.TARGET);
		}

		private FeatureAttributeData(final Map<ReferentFeature, Attribute> featureAttrs,
				final ArrayList<Attribute> attrList, final Attribute classAttr) {
			this.featureAttrs = featureAttrs;
			this.attrList = attrList;
			this.classAttr = classAttr;
		}

		private FeatureAttributeData(final Map<ReferentFeature, Attribute> featureAttrs,
				final ReferentFeature classFeature) {
			this(featureAttrs, new ArrayList<>(featureAttrs.values()), featureAttrs.get(classFeature));
		}

		/**
		 * Creates a new {@link Instance} representing a given {@link Referent}.
		 *
		 * @param ref
		 *            The {@code Referent} to create an {@code Instance} for.
		 * @return A new {@code Instance}; There is no reason to cache Instance
		 *         values because {@link Instances#add(Instance)} always creates
		 *         a shallow copy thereof anyway, so the only possible benefit
		 *         of a cache would be avoiding the computational cost of object
		 *         construction at the cost of greater memory requirements.
		 */
		public Instance createInstance(final Referent ref) {
			final Map<ReferentFeature, Attribute> attrMap = featureAttrs;
			final DenseInstance instance = new DenseInstance(attrMap.size());
			Arrays.stream(ReferentFeature.values()).forEach(feature -> feature.setValue(instance, ref, attrMap));
			assert instance.numAttributes() == attrMap.size();
			return instance;
		}

		/**
		 * Creates a new {@link Instances} representing the given
		 * {@link Referent} instances.
		 *
		 * @param refs
		 *            The {@code Referent} instances to create {@code Instance}
		 *            objects for; Their index in the given {@link List} will be
		 *            the index of their corresponding {@code Instance} objects
		 *            in the result data structure.
		 * @return A new {@code Instances} object containing {@code Instance}
		 *         objects representing each given {@code Referent}.
		 */
		public Instances createInstances(final List<Referent> refs) {
			final Instances result = createNewInstances(refs.size());
			assert result.numAttributes() == featureAttrs.size();
			refs.stream().map(this::createInstance).forEachOrdered(result::add);
			assert result.size() == refs.size();
			return result;
		}

		/*
		 * (non-Javadoc)
		 *
		 * @see java.lang.Object#equals(java.lang.Object)
		 */
		@Override
		public boolean equals(final Object obj) {
			if (this == obj) {
				return true;
			}
			if (obj == null) {
				return false;
			}
			if (!(obj instanceof FeatureAttributeData)) {
				return false;
			}
			final FeatureAttributeData other = (FeatureAttributeData) obj;
			if (attrList == null) {
				if (other.attrList != null) {
					return false;
				}
			} else if (!attrList.equals(other.attrList)) {
				return false;
			}
			if (classAttr == null) {
				if (other.classAttr != null) {
					return false;
				}
			} else if (!classAttr.equals(other.classAttr)) {
				return false;
			}
			if (featureAttrs == null) {
				if (other.featureAttrs != null) {
					return false;
				}
			} else if (!featureAttrs.equals(other.featureAttrs)) {
				return false;
			}
			return true;
		}

		/*
		 * (non-Javadoc)
		 *
		 * @see java.lang.Object#hashCode()
		 */
		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + (attrList == null ? 0 : attrList.hashCode());
			result = prime * result + (classAttr == null ? 0 : classAttr.hashCode());
			result = prime * result + (featureAttrs == null ? 0 : featureAttrs.hashCode());
			return result;
		}

		/*
		 * (non-Javadoc)
		 *
		 * @see java.lang.Object#toString()
		 */
		@Override
		public String toString() {
			final StringBuilder builder = new StringBuilder(256);
			builder.append("FeatureAttributeData [featureAttrs=");
			builder.append(featureAttrs);
			builder.append(", attrList=");
			builder.append(attrList);
			builder.append(", classAttr=");
			builder.append(classAttr);
			builder.append("]");
			return builder.toString();
		}

		private Instances createNewInstances(final int capacity) {
			final Instances result = new Instances("Referents", attrList, capacity);
			result.setClass(classAttr);
			return result;
		}
	}

	public final class Scorer {

		/**
		 * The {@link ReferentClassification} to get the score for,
		 * i.e.&nbsp;the probability of this class being the correct one for a
		 * given word {@code Classifier} and referent.
		 */
		private final ReferentClassification classification;

		/**
		 *
		 * @param classification
		 *            The {@link ReferentClassification} to get the score for,
		 *            i.e.&nbsp;the probability of this class being the correct
		 *            one for a given word {@code Classifier} and referent.
		 */
		private Scorer(final ReferentClassification classification) {
			this.classification = classification;
		}

		/*
		 * (non-Javadoc)
		 *
		 * @see java.lang.Object#equals(java.lang.Object)
		 */
		@Override
		public boolean equals(final Object obj) {
			if (this == obj) {
				return true;
			}
			if (obj == null) {
				return false;
			}
			if (!(obj instanceof Scorer)) {
				return false;
			}
			final Scorer other = (Scorer) obj;
			if (!getOuterType().equals(other.getOuterType())) {
				return false;
			}
			if (classification != other.classification) {
				return false;
			}
			return true;
		}

		/*
		 * (non-Javadoc)
		 *
		 * @see java.lang.Object#hashCode()
		 */
		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + getOuterType().hashCode();
			result = prime * result + (classification == null ? 0 : classification.hashCode());
			return result;
		}

		/**
		 *
		 * @param wordClassifier
		 *            The word {@link Classifier classifier} to use.
		 * @param insts
		 *            The {@link Instances} to classify.
		 * @return The probabilities of the {@code Instances} being classified
		 *         correctly.
		 * @throws ClassificationException
		 *             If an {@link Exception} occurs during
		 *             {@link BatchPredictor#distributionForInstances(Instances)
		 *             classification}.
		 */
		public DoubleStream score(final BatchPredictor wordClassifier, final Instances insts) {
			final double[][] dists;
			try {
				dists = wordClassifier.distributionsForInstances(insts);
			} catch (final Exception e) {
				throw new ClassificationException(e);
			}
			return classification.getProbabilities(dists, insts);
		}

		/**
		 *
		 * @param wordClassifier
		 *            The word {@link Classifier classifier} to use.
		 * @param refs
		 *            The {@link Referent} instances to classify.
		 * @return The probabilities of the {@code Referent} instances being
		 *         classified correctly.
		 * @throws ClassificationException
		 *             If an {@link Exception} occurs during
		 *             {@link BatchPredictor#distributionForInstances(Instances)
		 *             classification}.
		 */
		public DoubleStream score(final BatchPredictor wordClassifier, final List<Referent> refs) {
			final Instances insts = featureAttrs.createInstances(refs);
			return score(wordClassifier, insts);
		}

		/**
		 *
		 * @param wordClassifier
		 *            The word {@link Classifier classifier} to use.
		 * @param inst
		 *            The {@link Instance} to classify.
		 * @return The probability of the {@code Instance} being classified
		 *         correctly.
		 * @throws ClassificationException
		 *             If an {@link Exception} occurs during
		 *             {@link Classifier#distributionForInstance(Instance)
		 *             classification}.
		 */
		public double score(final Classifier wordClassifier, final Instance inst) throws ClassificationException {
			double[] dist;
			try {
				// NOTE: This cannot be (much) slower than
				// "weka.core.BatchPredictor.distributionsForInstances(Instances)"
				// because the class Logistic simply calls the following method
				// for
				// each Instance in a given Instances collection, and creating
				// an
				// Instances is more expensive than a simple e.g. ArrayList
				dist = wordClassifier.distributionForInstance(inst);
			} catch (final Exception e) {
				throw new ClassificationException(e);
			}
			return classification.getProbability(dist, inst);
		}

		/**
		 *
		 * @param wordClassifier
		 *            The word {@link Classifier classifier} to use.
		 * @param ref
		 *            The {@link Referent} to classify.
		 * @return The probability of the {@code Referent} being classified
		 *         correctly.
		 * @throws ClassificationException
		 *             If an {@link Exception} occurs during
		 *             {@link Classifier#distributionForInstance(Instance)
		 *             classification}.
		 */
		public double score(final Classifier wordClassifier, final Referent ref) throws ClassificationException {
			return score(wordClassifier, featureAttrs.createInstance(ref));
		}

		/*
		 * (non-Javadoc)
		 *
		 * @see java.lang.Object#toString()
		 */
		@Override
		public String toString() {
			final StringBuilder builder = new StringBuilder(64);
			builder.append("Scorer [classification=");
			builder.append(classification);
			builder.append("]");
			return builder.toString();
		}

		private LogisticModel getOuterType() {
			return LogisticModel.this;
		}

	}

	public static final class WordClassifiers {

		/**
		 * The {@code Logistic} classifier instance used for classifying unseen
		 * word observations.
		 */
		private final Logistic discountModel;

		/**
		 * A {@link ConcurrentMap} of words mapped to the {@link Logistic
		 * classifier} associated with each.
		 */
		private final ConcurrentMap<String, Logistic> wordClassifiers;

		/**
		 *
		 * @param wordClassifiers
		 *            A {@link ConcurrentMap} of words mapped to the
		 *            {@link Logistic classifier} associated with each.
		 * @param discountModel
		 *            The {@code Logistic} classifier instance used for
		 *            classifying unseen word observations.
		 */
		private WordClassifiers(final ConcurrentMap<String, Logistic> wordClassifiers, final Logistic discountModel) {
			this.wordClassifiers = wordClassifiers;
			this.discountModel = discountModel;
		}

		/*
		 * (non-Javadoc)
		 *
		 * @see java.lang.Object#equals(java.lang.Object)
		 */
		@Override
		public boolean equals(final Object obj) {
			if (this == obj) {
				return true;
			}
			if (obj == null) {
				return false;
			}
			if (!(obj instanceof WordClassifiers)) {
				return false;
			}
			final WordClassifiers other = (WordClassifiers) obj;
			if (discountModel == null) {
				if (other.discountModel != null) {
					return false;
				}
			} else if (!discountModel.equals(other.discountModel)) {
				return false;
			}
			if (wordClassifiers == null) {
				if (other.wordClassifiers != null) {
					return false;
				}
			} else if (!wordClassifiers.equals(other.wordClassifiers)) {
				return false;
			}
			return true;
		}

		/**
		 * @return The {@code Logistic} classifier instance used for classifying
		 *         unseen word observations.
		 */
		public Logistic getDiscountClassifier() {
			return discountModel;
		}

		/**
		 *
		 * @param word
		 *            The word to get the corresponding {@link Classifier} for.
		 * @return The {@code Logistic} classifier instance representing the
		 *         given word or {@code null} if no {@code Classifier} was found
		 *         for the given word.
		 */
		public Logistic getWordClassifier(final String word) {
			return wordClassifiers.get(word);
		}

		/**
		 *
		 * @param word
		 *            The word to get the corresponding {@link Classifier} for.
		 * @return The {@code Logistic} classifier instance representing the
		 *         given word or {@link #getDiscountModel() the discount
		 *         classifier} if no {@code Logistic} was found for the given
		 *         word.
		 */
		public Logistic getWordClassifierOrDiscount(final String word) {
			return wordClassifiers.getOrDefault(word, getDiscountClassifier());
		}

		/*
		 * (non-Javadoc)
		 *
		 * @see java.lang.Object#hashCode()
		 */
		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + (discountModel == null ? 0 : discountModel.hashCode());
			result = prime * result + (wordClassifiers == null ? 0 : wordClassifiers.hashCode());
			return result;
		}

		/*
		 * (non-Javadoc)
		 *
		 * @see java.lang.Object#toString()
		 */
		@Override
		public String toString() {
			final StringBuilder builder = new StringBuilder(wordClassifiers.size() + 1 + 64);
			builder.append("WordClassifiers [map=");
			builder.append(wordClassifiers);
			builder.append(", discountModel=");
			builder.append(discountModel);
			builder.append("]");
			return builder.toString();
		}

	}

	private static class MapPopulator extends ForkJoinTask<String> {

		/**
		 *
		 */
		private static final long serialVersionUID = 2476606510654821423L;

		private String result;

		private final ConcurrentMap<? super String, Logistic> wordClassifiers;

		private final Supplier<Entry<String, Logistic>> wordClassifierTrainer;

		public MapPopulator(final Supplier<Entry<String, Logistic>> wordClassifierTrainer,
				final ConcurrentMap<? super String, Logistic> wordClassifiers) {
			this.wordClassifierTrainer = wordClassifierTrainer;
			this.wordClassifiers = wordClassifiers;
		}

		/*
		 * (non-Javadoc)
		 *
		 * @see java.util.concurrent.ForkJoinTask#getRawResult()
		 */
		@Override
		public String getRawResult() {
			return result;
		}

		/*
		 * (non-Javadoc)
		 *
		 * @see java.util.concurrent.ForkJoinTask#exec()
		 */
		@Override
		protected boolean exec() {
			final Entry<String, Logistic> trainingResults = wordClassifierTrainer.get();
			final String word = trainingResults.getKey();
			final Logistic newClassifier = trainingResults.getValue();
			final Logistic oldClassifier = wordClassifiers.put(word, newClassifier);
			assert oldClassifier == null ? true : !oldClassifier.equals(newClassifier);
			result = word;
			return true;
		}

		/*
		 * (non-Javadoc)
		 *
		 * @see java.util.concurrent.ForkJoinTask#setRawResult(java.lang.Object)
		 */
		@Override
		protected void setRawResult(final String value) {
			result = value;
		}

	}

	/**
	 * Trains models for the specified words asynchronously.
	 *
	 * @author <a href="mailto:tcshore@kth.se">Todd Shore</a>
	 * @since 7 Dec 2017
	 *
	 */
	private class Trainer extends ForkJoinTask<WordClassifiers> {

		/**
		 *
		 */
		private static final long serialVersionUID = 466815336422504998L;

		/**
		 * The existing {@link WordClassifiers} object representing
		 * previously-trained classifiers.
		 */
		private final WordClassifiers oldClassifiers;

		/**
		 * The {@link WordClassifiers} object representing newly-trained
		 * classifiers.
		 */
		private WordClassifiers result;

		/**
		 * The weight of each datapoint representing a single observation for a
		 * given word.
		 */
		private final double weight;

		/**
		 * The vocabulary words to train models for.
		 */
		private final List<String> words;

		/**
		 * Constructs a {@link Trainer} for training models for the specified
		 * words asynchronously.
		 *
		 * @param words
		 *            The vocabulary words to train models for.
		 * @param weight
		 *            The weight of each datapoint representing a single
		 *            observation for a given word.
		 * @param oldClassifiers
		 *            The existing {@link WordClassifiers} object representing
		 *            previously-trained classifiers.
		 */
		private Trainer(final List<String> words, final double weight, final WordClassifiers oldClassifiers) {
			this.words = words;
			this.weight = weight;
			this.oldClassifiers = oldClassifiers;
		}

		/*
		 * (non-Javadoc)
		 *
		 * @see java.util.concurrent.ForkJoinTask#getRawResult()
		 */
		@Override
		public WordClassifiers getRawResult() {
			return result;
		}

		/*
		 * (non-Javadoc)
		 *
		 * @see java.util.concurrent.ForkJoinTask#exec()
		 */
		@Override
		protected boolean exec() {
			// Train the discount model. NOTE: The discount model should not be
			// in the same map as the classifiers for actually-seen observations
			final ForkJoinTask<Entry<String, Logistic>> discountClassifierTrainer = ForkJoinTask
					.adapt(new WordClassifierTrainer(OOV_CLASS_LABEL,
							() -> createDiscountClassExamples(trainingSet, words), weight));
			discountClassifierTrainer.fork();
			// Train a model for each word
			final ConcurrentMap<String, Logistic> extantClassifiers = oldClassifiers.wordClassifiers;
			final MapPopulator[] wordClassifierTrainers = words.stream()
					.map(word -> new MapPopulator(
							new WordClassifierTrainer(word, () -> createWordClassExamples(trainingSet, word), weight),
							extantClassifiers))
					.toArray(MapPopulator[]::new);
			ForkJoinTask.invokeAll(wordClassifierTrainers);

			final Entry<String, Logistic> discountResults = discountClassifierTrainer.join();
			result = new WordClassifiers(extantClassifiers, discountResults.getValue());
			return true;
		}

		/*
		 * (non-Javadoc)
		 *
		 * @see java.util.concurrent.ForkJoinTask#setRawResult(java.lang.Object)
		 */
		@Override
		protected void setRawResult(final WordClassifiers value) {
			result = value;
		}

	}

	private class WordClassifierTrainer
			implements Callable<Entry<String, Logistic>>, Supplier<Entry<String, Logistic>> {

		/**
		 * A {@link Supplier} of {@link Weighted} {@link Referent} instances to
		 * use as training examples.
		 */
		private final Supplier<? extends Stream<Weighted<Referent>>> exampleSupplier;

		/**
		 * The weight of each datapoint representing a single observation for
		 * the given word.
		 */
		private final double weight;

		/**
		 * The word to train a {@link Logistic} classifier for.
		 */
		private final String word;

		/**
		 *
		 * @param word
		 *            The word to train a {@link Logistic} classifier for.
		 * @param exampleSupplier
		 *            A {@link Supplier} of {@link Weighted} {@link Referent}
		 *            instances to use as training examples.
		 * @param weight
		 *            The weight of each datapoint representing a single
		 *            observation for the given word.
		 */
		private WordClassifierTrainer(final String word,
				final Supplier<? extends Stream<Weighted<Referent>>> exampleSupplier, final double weight) {
			this.word = word;
			this.exampleSupplier = exampleSupplier;
			this.weight = weight;
		}

		/*
		 * (non-Javadoc)
		 *
		 * @see java.util.concurrent.Callable#call()
		 */
		@Override
		public Entry<String, Logistic> call() {
			return get();
		}

		@Override
		public Entry<String, Logistic> get() {
			final Logistic logistic = new Logistic();
			@SuppressWarnings("unchecked")
			final Weighted<Referent>[] examples = exampleSupplier.get().toArray(Weighted[]::new);
			final Instances dataset = featureAttrs.createNewInstances(examples.length);

			for (final Weighted<Referent> example : examples) {
				final Referent ref = example.getWrapped();
				final Instance inst = featureAttrs.createInstance(ref);
				// The instance's current weight is the weight of the instance's
				// class; now multiply it by the weight of the word the model is
				// being trained for
				final double totalWeight = weight * example.getWeight();
				inst.setWeight(totalWeight);
				dataset.add(inst);
			}

			try {
				logistic.buildClassifier(dataset);
			} catch (final Exception e) {
				throw new WordClassifierTrainingException(word, e);
			}
			return Pair.of(word, logistic);
		}

	}

	enum ReferentClassification {
		// @formatter:off
		/**
		 * Denotes that the given referent is classified as <em>not</em> being
		 * the &ldquo;target&rdquo; referent, i.e.&nbsp;the entity is not the
		 * one being referred to in the round being classified.
		 */
		FALSE(Boolean.FALSE.toString()),
		/**
		 * Denotes that the given referent is classified as being the
		 * &ldquo;target&rdquo; referent, i.e.&nbsp;the entity is actually the
		 * one being referred to in the round being classified.
		 */
		TRUE(Boolean.TRUE.toString());
		// @formatter:on

		private static final List<String> ORDERED_VALUES;

		private static final Map<ReferentClassification, Integer> VALUE_IDXS;

		static {
			final List<ReferentClassification> ordering = Arrays.asList(ReferentClassification.values());
			ORDERED_VALUES = Arrays
					.asList(ordering.stream().map(ReferentClassification::getClassValue).toArray(String[]::new));
			VALUE_IDXS = Lists.createIndexMap(ordering, ReferentClassification.class);
			assert ORDERED_VALUES.size() == VALUE_IDXS.size();
		}

		private String classValue;

		private ReferentClassification(final String classValue) {
			this.classValue = classValue.intern();
		}

		public String getClassValue() {
			return classValue;
		}

		private int getClassValueIdx() {
			return VALUE_IDXS.get(this);
		}

		private DoubleStream getProbabilities(final double[][] dists, final Instances insts) {
			final int classIdx = getClassValueIdx();
			return Arrays.stream(dists).mapToDouble(dist -> dist[classIdx]);
		}

		private double getProbability(final double[] dist, final Instance inst) {
			final int classIdx = getClassValueIdx();
			return dist[classIdx];
		}
	}

	enum ReferentFeature {
		BLUE {
			@Override
			protected void setValue(final Instance instance, final Referent ref,
					final Map<ReferentFeature, Attribute> attrMap) {
				instance.setValue(getAttr(attrMap), ref.getBlue());
			}
		},
		GREEN {
			@Override
			protected void setValue(final Instance instance, final Referent ref,
					final Map<ReferentFeature, Attribute> attrMap) {
				instance.setValue(getAttr(attrMap), ref.getGreen());
			}
		},
		MID_X {
			@Override
			protected void setValue(final Instance instance, final Referent ref,
					final Map<ReferentFeature, Attribute> attrMap) {
				instance.setValue(getAttr(attrMap), ref.getMidX());
			}
		},
		MID_Y {
			@Override
			protected void setValue(final Instance instance, final Referent ref,
					final Map<ReferentFeature, Attribute> attrMap) {
				instance.setValue(getAttr(attrMap), ref.getMidY());
			}
		},
		POSITION_X {
			@Override
			protected void setValue(final Instance instance, final Referent ref,
					final Map<ReferentFeature, Attribute> attrMap) {
				instance.setValue(getAttr(attrMap), ref.getPositionX());
			}
		},
		POSITION_Y {

			@Override
			protected void setValue(final Instance instance, final Referent ref,
					final Map<ReferentFeature, Attribute> attrMap) {
				instance.setValue(getAttr(attrMap), ref.getPositionY());
			}
		},
		RED {
			@Override
			protected void setValue(final Instance instance, final Referent ref,
					final Map<ReferentFeature, Attribute> attrMap) {
				instance.setValue(getAttr(attrMap), ref.getRed());
			}
		},
		SHAPE {
			@Override
			protected Attribute createAttribute(final List<String> shapeUniqueValues) {
				return new Attribute(name(), shapeUniqueValues);
			}

			@Override
			protected Object getValue(final Instance instance, final Map<ReferentFeature, Attribute> attrMap) {
				return getCategoricalValue(instance, attrMap);
			}

			@Override
			protected void setValue(final Instance instance, final Referent ref,
					final Map<ReferentFeature, Attribute> attrMap) {
				instance.setValue(getAttr(attrMap), ref.getShape());
			}
		},
		SIZE {
			@Override
			protected void setValue(final Instance instance, final Referent ref,
					final Map<ReferentFeature, Attribute> attrMap) {
				instance.setValue(getAttr(attrMap), ref.getSize());
			}
		},
		TARGET {
			@Override
			protected Attribute createAttribute(final List<String> shapeUniqueValues) {
				return new Attribute(name(), ReferentClassification.ORDERED_VALUES);
			}

			@Override
			protected Object getValue(final Instance instance, final Map<ReferentFeature, Attribute> attrMap) {
				return getCategoricalValue(instance, attrMap);
			}

			@Override
			protected void setValue(final Instance instance, final Referent ref,
					final Map<ReferentFeature, Attribute> attrMap) {
				instance.setValue(getAttr(attrMap), Boolean.toString(ref.isTarget()));
			}
		};

		protected Attribute createAttribute(final List<String> shapeUniqueValues) {
			return new Attribute(name());
		}

		protected final Attribute getAttr(final Map<ReferentFeature, Attribute> attrMap) {
			return attrMap.get(this);
		}

		protected final Object getCategoricalValue(final Instance instance,
				final Map<ReferentFeature, Attribute> attrMap) {
			final Attribute attr = getAttr(attrMap);
			assert attr.isNominal() || attr.isString();
			final double scalarValue = instance.value(attr);
			final int nominalValueIdx = Math.toIntExact(Math.round(scalarValue));
			return attr.value(nominalValueIdx);
		}

		protected Object getValue(final Instance instance, final Map<ReferentFeature, Attribute> attrMap) {
			return instance.value(getAttr(attrMap));
		}

		protected abstract void setValue(Instance instance, Referent ref, Map<ReferentFeature, Attribute> attrMap);
	}

	private static final int DEFAULT_INITIAL_WORD_CLASS_MAP_CAPACITY = HashedCollections.capacity(1130);

	/**
	 * The default {@link ReferentClassification} to use for scoring.
	 */
	private static final ReferentClassification DEFAULT_REF_CLASSIFICATION = ReferentClassification.TRUE;

	private static final Logger LOGGER = LoggerFactory.getLogger(LogisticModel.class);

	static final String OOV_CLASS_LABEL = "__OUT_OF_VOCABULARY__";

	private static long calculateRetryWaitTime(final int retryNumber) {
		assert retryNumber > 0 : String.format("Retry number was %d but must be positive.", retryNumber);
		final long result;
		switch (retryNumber) {
		case 1: {
			result = 1L;
			break;
		}
		case 2: {
			result = 5L;
			break;
		}
		default: {
			result = 10L;
			break;
		}
		}
		return result;
	}

	private static Stream<Weighted<Referent>> createClassWeightedReferents(final Round round) {
		final List<Referent> refs = round.getReferents();
		final Referent[] posRefs = refs.stream().filter(Referent::isTarget).toArray(Referent[]::new);
		final Referent[] negRefs = refs.stream().filter(ref -> !ref.isTarget()).toArray(Referent[]::new);
		final double negClassWeight = 1.0;
		final double posClassWeight = negRefs.length / (double) posRefs.length;
		return Stream.concat(createWeightedReferents(posRefs, posClassWeight),
				createWeightedReferents(negRefs, negClassWeight));
	}

	private static Stream<Weighted<Referent>> createDiscountClassExamples(final RoundSet rounds,
			final Collection<? super String> words) {
		return rounds.getDiscountRounds(words).flatMap(LogisticModel::createClassWeightedReferents);
	}

	private static Stream<Weighted<Referent>> createWeightedReferents(final Referent[] refs, final double weight) {
		return Arrays.stream(refs).map(ref -> new Weighted<>(ref, weight));
	}

	private static Stream<Weighted<Referent>> createWordClassExamples(final RoundSet rounds, final String word) {
		return rounds.getExampleRounds(word).flatMap(LogisticModel::createClassWeightedReferents);
	}

	private FeatureAttributeData featureAttrs;

	private final Map<ModelParameter, Object> modelParams;

	private final ForkJoinPool taskPool;

	/**
	 * The {@link RoundSet} to use as training data.
	 */
	private RoundSet trainingSet;

	private Vocabulary vocab;

	private WordClassifiers wordClassifiers;

	public LogisticModel() {
		this(ModelParameter.createDefaultParamValueMap());
	}

	public LogisticModel(final Map<ModelParameter, Object> modelParams) { // NO_UCD
																			// (use
																			// default)
		this(modelParams, ForkJoinPool.commonPool());
	}

	public LogisticModel(final Map<ModelParameter, Object> modelParams, final ForkJoinPool taskPool) { // NO_UCD
																										// (use
																										// default)
		this(modelParams, taskPool, DEFAULT_INITIAL_WORD_CLASS_MAP_CAPACITY);
	}

	public LogisticModel(final Map<ModelParameter, Object> modelParams, final ForkJoinPool taskPool, // NO_UCD
																										// (use
																										// private)
			final int initialMapCapacity) {
		this.modelParams = modelParams;
		this.taskPool = taskPool;
		wordClassifiers = new WordClassifiers(new ConcurrentHashMap<>(initialMapCapacity), new Logistic());
		featureAttrs = new FeatureAttributeData();
		vocab = new Vocabulary(Object2LongMaps.emptyMap());
	}

	/**
	 *
	 * @return A new {@link RankScorer} for {@link #DEFAULT_REF_CLASSIFICATION
	 *         the default classification} using this {@link LogisticModel}.
	 */
	public RankScorer createRankScorer() {
		return createRankScorer(DEFAULT_REF_CLASSIFICATION);
	}

	/**
	 *
	 * @param classification
	 *            The {@link ReferentClassification} to get the score for,
	 *            i.e.&nbsp;the probability of this class being the correct one
	 *            for the given word {@code Classifier} and {@code Instance}.
	 * @return A new {@link RankScorer} for the given
	 *         {@code ReferentClassification} using this {@link LogisticModel}.
	 */
	public RankScorer createRankScorer(final ReferentClassification classification) {
		final Scorer scorer = createScorer(classification);
		return new RankScorer(this, scorer);
	}

	/**
	 *
	 * @return A new {@link Scorer} for {@link #DEFAULT_REF_CLASSIFICATION the
	 *         default classification} using this {@link LogisticModel}.
	 */
	public Scorer createScorer() {
		return new Scorer(DEFAULT_REF_CLASSIFICATION);
	}

	/**
	 * @param classification
	 *            The {@link ReferentClassification} to get the score for,
	 *            i.e.&nbsp;the probability of this class being the correct one
	 *            for the given word {@code Classifier} and {@code Instance}.
	 * @return A new {@link Scorer} for the given {@code ReferentClassification}
	 *         using this {@link LogisticModel}.
	 */
	public Scorer createScorer(final ReferentClassification classification) {
		return new Scorer(classification);
	}

	/**
	 *
	 * @return A new {@link WordProbabilityScorer} for
	 *         {@link #DEFAULT_REF_CLASSIFICATION the default classification}
	 *         using this {@link LogisticModel}.
	 */
	public WordProbabilityScorer createWordProbabilityScorer() {
		return createWordProbabilityScorer(DEFAULT_REF_CLASSIFICATION);
	}

	/**
	 *
	 * @param classification
	 *            The {@link ReferentClassification} to get the score for,
	 *            i.e.&nbsp;the probability of this class being the correct one
	 *            for the given word {@code Classifier} and {@code Instance}.
	 * @return A new {@link WordProbabilityScorer} for the given
	 *         {@code ReferentClassification} using this {@link LogisticModel}.
	 */
	public WordProbabilityScorer createWordProbabilityScorer(final ReferentClassification classification) {
		final Scorer scorer = createScorer(classification);
		return new WordProbabilityScorer(this, scorer);
	}

	/**
	 * @return the featureAttrs
	 */
	public FeatureAttributeData getFeatureAttrs() {
		return featureAttrs;
	}

	public Vocabulary getVocabulary() {
		return vocab;
	}

	/**
	 * @return the map
	 */
	public WordClassifiers getWordClassifiers() {
		return wordClassifiers;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		final StringBuilder builder = new StringBuilder();
		builder.append("LogisticModel [featureAttrs=");
		builder.append(featureAttrs);
		builder.append(", modelParams=");
		builder.append(modelParams);
		builder.append(", taskPool=");
		builder.append(taskPool);
		builder.append(", trainingSet=");
		builder.append(trainingSet);
		builder.append(", vocab=");
		builder.append(vocab);
		builder.append(", map=");
		builder.append(wordClassifiers);
		builder.append("]");
		return builder.toString();
	}

	private int estimateUpdatedVocabSize() {
		return vocab.getWordCount() + 5;
	}

	private ForkJoinTask<WordClassifiers> submitTrainingTask(final Trainer trainer) {
		ForkJoinTask<WordClassifiers> result = null;

		do {
			try {
				result = taskPool.submit(trainer);
			} catch (final RejectedExecutionException e) {
				int tryCount = 1;
				long waitTimeMins = calculateRetryWaitTime(tryCount);
				LOGGER.info(String.format(
						"A(n) %s occurred while trying to submit a training task to the task pool; Will wait %d minute(s) before trying again.",
						e.getClass().getSimpleName(), waitTimeMins), e);
				boolean isReady = taskPool.awaitQuiescence(waitTimeMins, TimeUnit.MINUTES);
				while (!isReady) {
					waitTimeMins = calculateRetryWaitTime(++tryCount);
					LOGGER.info("Still not quiescent; Waiting {} more minute(s) before try number {}.", waitTimeMins,
							tryCount);
					isReady = taskPool.awaitQuiescence(waitTimeMins, TimeUnit.MINUTES);
				}
			}
		} while (result == null);

		return result;
	}

	/**
	 * Trains models for the specified words asynchronously.
	 *
	 * @param words
	 *            The vocabulary words to train models for.
	 * @param weight
	 *            The weight of each datapoint representing a single observation
	 *            for a given word.
	 * @return A {@link ForkJoinTask} which asynchronously creates the new
	 *         {@link WordClassifiers}.
	 */
	private ForkJoinTask<WordClassifiers> trainWordClassifiers(final List<String> words, final double weight) {
		final Trainer trainer = new Trainer(words, weight, wordClassifiers);
		return submitTrainingTask(trainer);
	}

	/**
	 * @return the modelParams
	 */
	Map<ModelParameter, Object> getModelParams() {
		return modelParams;
	}

	/**
	 * Trains the word models using all data from a {@link SessionSet}.
	 *
	 * @param set
	 *            The {@code SessionSet} to use as training data.
	 */
	void train(final SessionSet set) {
		final boolean onlyInstructor = (Boolean) modelParams.get(ModelParameter.ONLY_INSTRUCTOR);
		trainingSet = new RoundSet(set, onlyInstructor);
		vocab = trainingSet.createVocabulary(Math.max(1000, estimateUpdatedVocabSize()));
		// NOTE: Values are retrieved directly from the map instead of e.g.
		// assigning
		// them to a final field because it's possible that the map values
		// change at another place in the code and performance isn't an issue
		// here anyway
		vocab.prune((Integer) modelParams.get(ModelParameter.DISCOUNT));
		featureAttrs = new FeatureAttributeData();
		final ForkJoinTask<WordClassifiers> wordClassifierTrainingTask = trainWordClassifiers(vocab.getWords(), 1.0);
		wordClassifiers = wordClassifierTrainingTask.join();
	}

	/**
	 * Updates (trains) the models with the new round
	 */
	void updateModel(final Round round) {
		trainingSet.getRounds().add(round);
		final Vocabulary oldVocab = vocab;
		vocab = trainingSet.createVocabulary(estimateUpdatedVocabSize());
		// NOTE: Values are retrieved directly from the map instead of e.g.
		// assigning
		// them to a final field because it's possible that the map values
		// change at another place in the code and performance isn't an issue
		// here anyway
		vocab.prune((Integer) modelParams.get(ModelParameter.DISCOUNT));
		final Number updateWeight = (Number) modelParams.get(ModelParameter.UPDATE_WEIGHT);
		final ForkJoinTask<WordClassifiers> wordClassifierTrainingTask = trainWordClassifiers(vocab.getUpdatedWordsSince(oldVocab),
				NumberTypeConversions.finiteDoubleValue(updateWeight.doubleValue()));
		wordClassifiers = wordClassifierTrainingTask.join();
	}

}
