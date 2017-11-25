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

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.math3.stat.descriptive.moment.Mean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import se.kth.speech.coin.tangrams.wac.data.Referent;
import se.kth.speech.coin.tangrams.wac.data.Round;
import se.kth.speech.coin.tangrams.wac.data.RoundSet;
import se.kth.speech.coin.tangrams.wac.data.SessionSet;
import se.kth.speech.coin.tangrams.wac.data.SessionSetReader;
import se.kth.speech.coin.tangrams.wac.data.Vocabulary;

public class WordStats {

	private static final Logger LOGGER = LoggerFactory.getLogger(WordStats.class);

	public static void main(final String[] args) throws IOException, ClassificationException {
		final WordStats stats = new WordStats();
		final Path inpath = Paths.get(args[0]);
		final Path refTokenFilePath = Paths.get(args[1]);
		LOGGER.info("Will read sessions from \"{}\", using referring language read from \"{}\".", inpath,
				refTokenFilePath);
		final SessionSet set = new SessionSetReader(refTokenFilePath).apply(inpath);
		final Map<ModelParameter, Object> modelParams = ModelParameter.createDefaultParamValueMap();
		final LogisticModel model = new LogisticModel(modelParams);
		model.train(set);
		final Vocabulary vocab = model.getVocabulary();
		for (final Round round : new RoundSet(set, modelParams).getRounds()) {
			round.getWords(modelParams).forEach(word -> {
				if (vocab.has(word)) {
					for (final Referent ref : round.getReferents()) {
						final double score = model.score(word, ref);
						stats.add(round, word, score, ref.isTarget());
					}
				}
			});
		}
		stats.print();
	}

	private final Map<String, List<Double>> targetScores = new HashMap<>();
	private final Map<String, List<Double>> offScores = new HashMap<>();

	private final Map<String, List<Double>> scores = new HashMap<>();

	private final Map<String, Integer> count = new HashMap<>();

	public void add(final Round round, final String word, final double score, final boolean target) {
		// if (target && score < 0.2) {
		// System.out.println(word + " " + round.prettyDialog());
		// }
		if (target) {
			if (!targetScores.containsKey(word)) {
				targetScores.put(word, new ArrayList<>());
			}
			targetScores.get(word).add(score);
		} else {
			if (!offScores.containsKey(word)) {
				offScores.put(word, new ArrayList<>());
			}
			offScores.get(word).add(score);
		}
		if (!scores.containsKey(word)) {
			scores.put(word, new ArrayList<>());
		}
		scores.get(word).add(score);

		count.put(word, count.getOrDefault(word, 0) + 1);
	}

	public void print() {
		for (final String word : targetScores.keySet()) {
			final Mean meanTarget = new Mean();
			final Mean meanOff = new Mean();
			for (final Double score : targetScores.get(word)) {
				meanTarget.increment(score);
			}
			if (offScores.containsKey(word)) {
				for (final Double score : offScores.get(word)) {
					meanOff.increment(score);
				}
				System.out.println(
						word + " " + meanTarget.getResult() + " " + meanOff.getResult() + " " + count.get(word));
			}
		}
	}

}
