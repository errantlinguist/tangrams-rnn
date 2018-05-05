package se.kth.speech.coin.tangrams.data;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

public class RoundSet {

	public List<Round> rounds = new ArrayList<>();
	
	public RoundSet(SessionSet set) {
		for (Session sess : set.sessions) {
			this.rounds.addAll(sess.rounds);
			for (Round round : rounds) {
				round.weight = 1d;
			}
		}
	}
	
	public RoundSet(List<Round> rounds) {
		this.rounds = rounds;
	}
	
	public Vocabulary getNormalizedVocabulary() {
		Vocabulary vocab = new Vocabulary();
		for (Round round : rounds) {
			Set<String> words = new HashSet<>();
			for (Utterance utt : round.utts) {
				for (String word : utt.getNormalizedWords()) {
					words.add(word);
				}
			}
			for (String word : words) {
				vocab.add(word);
			}
		}
		vocab.prune(Parameters.DISCOUNT);
		return vocab;
	}

	public Vocabulary getVocabulary() {
		Vocabulary vocab = new Vocabulary();
		for (Round round : rounds) {
			Set<String> words = new HashSet<>();
			for (String word : round.getWords()) {
				words.add(word);
			}
			for (String word : words) {
				vocab.add(word);
			}
		}
		vocab.prune(Parameters.DISCOUNT);
		return vocab;
	} 

	public Vocabulary getBigramVocabulary() {
		Vocabulary vocab = new Vocabulary();
		for (Round round : rounds) {
			for (Utterance utt : round.utts) {
				String lastWord = null;
				for (String word : utt.fullText) {
					vocab.add(word);
					if (lastWord != null)
						vocab.add(lastWord + " " + word);
					lastWord = word;
				}
			}
		}
		vocab.prune(Parameters.DISCOUNT);
		return vocab;
	}
	/*
	public List<Referent> getExamples(String hasWord) {
		List<Referent> result = new ArrayList<>();
		for (Round round : rounds) {
			if (round.hasWord(hasWord)) {
				result.addAll(round.referents);
			}
		}
		return result;
	}
	
	public List<Referent> getPosExamples(String hasWord) {
		List<Referent> result = new ArrayList<>();
		for (Round round : rounds) {
			if (round.hasWord(hasWord)) {
				result.add(round.target);
			}
		}
		return result;
	}
	
	public List<Referent> getNegExamples(String hasWord) {
		List<Referent> result = new ArrayList<>();
		for (Round round : rounds) {
			if (round.hasWord(hasWord)) {
				NEG:
				for (Referent ref : round.referents) {
					if (ref != round.target) {
						
						for (Round round2 : rounds) {
							if (round.session == round2.session && round != round2 && round2.target.id == ref.id && round2.hasWord(hasWord)) {
								//System.out.println("Skipping");
								continue NEG;
							}
						}
						
						result.add(ref);
					}
				}
			}
		}
		return result;
	}
	*/

	public List<Referent> getDiscountExamples(Collection<String> words) {
		List<Referent> result = new ArrayList<>();
		for (Round round : rounds) {
			if (round.hasDiscount(words)) {
				result.addAll(round.referents);
			}
		}
		return result;
	}

	private static Random rand = new Random(5);
	
	public RoundSet[] split() {
		List<Round> list = new ArrayList<>(rounds);
		list.sort(new Comparator<Round>() {
			@Override
			public int compare(Round o1, Round o2) {
				return rand.nextInt(2) == 0 ? -1 : 1;
			}
			
		});
		RoundSet sets[] = new RoundSet[2];
		sets[0] = new RoundSet(list.subList(0, list.size()/2));
		sets[1] = new RoundSet(list.subList(list.size()/2, list.size()));
		return sets;
	}
	
}
