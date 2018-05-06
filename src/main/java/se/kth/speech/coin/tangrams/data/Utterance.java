package se.kth.speech.coin.tangrams.data;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;


public class Utterance {

	public int getRound(){
		return round;
	}

	private final int round;

	public final String[] fullText;

	public final String[] refText;

	public final String speaker;

	public final boolean isGiver;

	public final float startTime;

	public final float endTime;

	public Utterance(final int round, float startTime, float endTime, final String speakerId, boolean isGiver, String[] fullText, String[] refText) {
		this.round = round;
		this.startTime = startTime;
		this.endTime = endTime;
		this.speaker = speakerId;
		this.isGiver = isGiver;
		this.fullText = fullText;
		this.refText = refText;

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


