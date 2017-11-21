package tangram.data;

public class Utterance {
	
	private static final String[] EMPTY_TOKEN_ARRAY = new String[0];

	public Integer round;
	public String[] fullText;
	public String[] refText;
	public String speaker;
	public boolean isGiver;
	
	public Utterance(String line) {
		String[] cols = line.split("\t");
		round = Integer.parseInt(cols[0]);
		fullText = cols[4].toLowerCase().split(" ");
		try {
			refText = cols[5].toLowerCase().split(" ");
		} catch (ArrayIndexOutOfBoundsException e) {
			refText = EMPTY_TOKEN_ARRAY;
		}
		
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


