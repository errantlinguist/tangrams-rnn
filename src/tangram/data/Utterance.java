package tangram.data;

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
	
}


