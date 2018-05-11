package se.kth.speech.coin.tangrams.logistic;

public final class ROCCurvePrediction {

	private final int falsePositiveCount;

	private final boolean isInstructor;

	private final int nthWordOccurrence;

	private final double probOthers;

	private final double probTarget;

	private final int rank;

	private final int round;

	private final double targetMentioned;

	private final int truePositiveCount;

	private final String word;

	ROCCurvePrediction(final int round, final boolean isInstructor, final String word, final int nthWordOccurrence,
			final double probTarget, final double probOthers, final double targetMentioned, final int rank,
			final int truePositiveCount, final int falsePositiveCount) {
		this.round = round;
		this.isInstructor = isInstructor;
		this.word = word;
		this.nthWordOccurrence = nthWordOccurrence;
		this.probTarget = probTarget;
		this.probOthers = probOthers;
		this.targetMentioned = targetMentioned;
		this.rank = rank;
		this.truePositiveCount = truePositiveCount;
		this.falsePositiveCount = falsePositiveCount;
	}

	/**
	 * @return the falsePositiveCount
	 */
	public int getFalsePositiveCount() {
		return falsePositiveCount;
	}

	public int getNthWordOccurrence() {
		return nthWordOccurrence;
	}

	public double getProbOthers() {
		return probOthers;
	}

	public double getProbTarget() {
		return probTarget;
	}

	public int getRank() {
		return rank;
	}

	public int getRound() {
		return round;
	}

	public double getTargetMentioned() {
		return targetMentioned;
	}

	/**
	 * @return the truePositiveCount
	 */
	public int getTruePositiveCount() {
		return truePositiveCount;
	}

	public String getWord() {
		return word;
	}

	public boolean isInstructor() {
		return isInstructor;
	}
}
