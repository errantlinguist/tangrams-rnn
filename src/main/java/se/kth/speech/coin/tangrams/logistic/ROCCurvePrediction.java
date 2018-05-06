package se.kth.speech.coin.tangrams.logistic;

public final class ROCCurvePrediction {

	private final int round;

	private final int ref;

	private final String word;

	private final int nthWordOccurrence;

	private final double probTarget;

	private final boolean isTarget;

	private final int mentioned;

	ROCCurvePrediction(int round, int ref, int mentioned, String word, int nthWordOccurrence, double probTarget, boolean isTarget) {
		this.round = round;
		this.ref = ref;
		this.mentioned = mentioned;
		this.word = word;
		this.nthWordOccurrence = nthWordOccurrence;
		this.probTarget = probTarget;
		this.isTarget = isTarget;
	}

	public int getMentioned() {
		return mentioned;
	}

	public int getRound() {
		return round;
	}

	public int getRef() {
		return ref;
	}

	public String getWord() {
		return word;
	}

	public int getNthWordOccurrence() {
		return nthWordOccurrence;
	}

	public double getProbTarget() {
		return probTarget;
	}

	public boolean isTarget() {
		return isTarget;
	}
}
