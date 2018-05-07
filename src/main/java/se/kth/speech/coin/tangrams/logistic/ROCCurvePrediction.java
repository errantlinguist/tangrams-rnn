package se.kth.speech.coin.tangrams.logistic;

public final class ROCCurvePrediction {

	private final int round;

	private final String word;

	private final int nthWordOccurrence;

	private final double probTarget;

	private final double probOthers;

	private final double targetMentioned;
	private final boolean isInstructor;
	private final int rank;

	ROCCurvePrediction(int round, final boolean isInstructor, String word, int nthWordOccurrence, double probTarget, double probOthers, double targetMentioned, int rank) {
		this.round = round;
		this.isInstructor = isInstructor;
		this.word = word;
		this.nthWordOccurrence = nthWordOccurrence;
		this.probTarget = probTarget;
		this.probOthers = probOthers;
		this.targetMentioned = targetMentioned;
		this.rank = rank;
	}

	public int getRank() {
		return rank;
	}

	public boolean isInstructor() {
		return isInstructor;
	}

	public double getTargetMentioned() {
		return targetMentioned;
	}

	public int getRound() {
		return round;
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

	public double getProbOthers() {
		return probOthers;
	}
}
