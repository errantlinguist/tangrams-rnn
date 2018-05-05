package se.kth.speech.coin.tangrams.data;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;


public class Utterance {

	public Integer round;
	public String[] fullText;
	public String[] refText;
	public String speaker;
	public boolean isGiver;

	public Utterance(String line) {
		String[] cols = line.split("\t");
		round = Integer.parseInt(cols[0]);
		fullText = cols[4].toLowerCase().split(" ");
		refText = cols[5].toLowerCase().split(" ");
		speaker = cols[1];
	}

	public void setRole(boolean AisGiver) {
		isGiver = (AisGiver && speaker.equals("A")) || (!AisGiver && speaker.equals("B"));
	}

	public boolean isNegative() {
		for (String word : fullText) {
			if (word.equals("no") || word.equals("nope"))
				return true;
		}
		return false;
	}

	public String getFullTextString() {
		StringBuilder sb = new StringBuilder();
		for (String word : fullText) {
			sb.append(word + " ");
		}
		return sb.toString().trim();
	}

	public String prettyString() {
		return (isGiver ? "G" : "F") + ": " + getFullTextString();
	}

	public String getNormalizedTextString() {
		String text = getFullTextString();
		text = text.replaceAll("'s\\b", " is");
		text = text.replaceAll("'ve\\b", " have");
		text = text.replaceAll("'re\\b", " are");
		text = text.replaceAll("'d\\b", " would");
		text = text.replaceAll("'ll\\b", " will");
		text = text.replaceAll("can't", "can not");
		text = text.replaceAll("n't\\b", " not");
		text = text.replaceAll("i'm", "i am");
		//text = text.replaceAll("mm-hmm", "okay");
		//text = text.replaceAll("mm-kay", "okay");
		text = text.replaceAll("'kay", "okay");
		//text = text.replaceAll("cucurucu", "cock");
		return text;
	}

	public List<String> getNormalizedWords() {
		return Arrays.stream(getNormalizedTextString().split(" ")).filter(
				item -> !item.endsWith("-") && !item.startsWith("-") 
				).collect(Collectors.toList());
	}
	
}


