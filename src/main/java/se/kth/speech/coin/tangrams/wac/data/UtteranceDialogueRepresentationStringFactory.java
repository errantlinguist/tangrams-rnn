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
package se.kth.speech.coin.tangrams.wac.data;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collector;
import java.util.stream.Collectors;

import org.apache.commons.lang3.tuple.Pair;


/**
 * @author <a href="mailto:tcshore@kth.se">Todd Shore</a>
 * @since 19 May 2017
 *
 */
public final class UtteranceDialogueRepresentationStringFactory implements Function<Iterator<Utterance>, String> {

	private static final Collector<CharSequence, ?, String> DEFAULT_SENTENCE_JOINER = Collectors.joining(" ");

	private static final Collector<CharSequence, ?, String> DEFAULT_WORD_JOINER = Collectors.joining(" ");

	private static final char DIALOGUE_TURN_DELIMITER = '"';

	private static void append(final StringBuilder sb, final Entry<String, String> speakerTurnRepr) {
		sb.append(createSpeakerUttPrefix(speakerTurnRepr.getKey()));
		sb.append(DIALOGUE_TURN_DELIMITER);
		sb.append(speakerTurnRepr.getValue());
		sb.append(DIALOGUE_TURN_DELIMITER);
	}

	private static String createSpeakerUttPrefix(final String speakerId) {
		return "**" + speakerId + ":** ";
	}

	private final Collector<? super CharSequence, ?, String> sentenceJoiner;

	private final Locale uttLocale;

	private final Collector<? super CharSequence, ?, String> wordJoiner;

	public UtteranceDialogueRepresentationStringFactory() {
		this(DataLanguageDefaults.getLocale(), DEFAULT_WORD_JOINER, DEFAULT_SENTENCE_JOINER);
	}

	public UtteranceDialogueRepresentationStringFactory(final Locale uttLocale) {
		this(uttLocale, DEFAULT_WORD_JOINER, DEFAULT_SENTENCE_JOINER);
	}

	private UtteranceDialogueRepresentationStringFactory(final Locale uttLocale,
			final Collector<? super CharSequence, ?, String> wordJoiner,
			final Collector<? super CharSequence, ?, String> sentenceJoiner) {
		this.uttLocale = uttLocale;
		this.wordJoiner = wordJoiner;
		this.sentenceJoiner = sentenceJoiner;
	}

	@Override
	public String apply(final Iterator<Utterance> uttIter) {
		final StringBuilder sb = new StringBuilder(128);
		final List<Entry<String, String>> diagTurnReprs = createDialogTurnReprs(uttIter);
		final Iterator<Entry<String, String>> diagTurnReprIter = diagTurnReprs.iterator();
		if (diagTurnReprIter.hasNext()) {
			append(sb, diagTurnReprIter.next());

			while (diagTurnReprIter.hasNext()) {
				sb.append(' ');
				append(sb, diagTurnReprIter.next());
			}
		}
		return sb.toString();
	}

	/**
	 * @return the sentenceJoiner
	 */
	public Collector<? super CharSequence, ?, String> getSentenceJoiner() {
		return sentenceJoiner;
	}

	/**
	 * @return the wordJoiner
	 */
	public Collector<? super CharSequence, ?, String> getWordJoiner() {
		return wordJoiner;
	}

	private String capitalizeFirstChar(final String str) {
		// http://stackoverflow.com/a/3904607/1391325
		return str.substring(0, 1).toUpperCase(uttLocale) + str.substring(1);
	}

	private List<Entry<String, String>> createDialogTurnReprs(final Iterator<Utterance> uttIter) {
		final List<Entry<String, String>> result = new ArrayList<>();
		if (uttIter.hasNext()) {
			final Utterance firstUtt = uttIter.next();

			String speakerId = firstUtt.getSpeakerId();
			List<String> diagTurnUttReprs = new ArrayList<>();
			diagTurnUttReprs.add(capitalizeFirstChar(firstUtt.getTokens().stream().collect(wordJoiner)) + '.');

			while (uttIter.hasNext()) {
				final Utterance nextUtt = uttIter.next();
				final String nextSpeakerId = nextUtt.getSpeakerId();
				if (!Objects.equals(speakerId, nextSpeakerId)) {
					result.add(Pair.of(speakerId, diagTurnUttReprs.stream().collect(sentenceJoiner)));
					speakerId = nextSpeakerId;
					diagTurnUttReprs = new ArrayList<>();
				}
				diagTurnUttReprs.add(capitalizeFirstChar(nextUtt.getTokens().stream().collect(wordJoiner)) + '.');
			}

			if (!diagTurnUttReprs.isEmpty()) {
				result.add(Pair.of(speakerId, diagTurnUttReprs.stream().collect(sentenceJoiner)));
			}

		}
		return result;
	}

}
