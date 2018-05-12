package se.kth.speech.coin.tangrams.logistic;

public final class ROCCurvePrediction {

	private final boolean isInstructor;

	private final boolean isTarget;

	private final double mentioned;

	private final int nthWordOccurrence;

	private final double probTrue;

	private final int round;

	private final String word;

	ROCCurvePrediction(final int round, final boolean isInstructor, final String word, final int nthWordOccurrence,
			final double probTrue, final boolean isTarget, final double mentioned) {
		this.round = round;
		this.isInstructor = isInstructor;
		this.word = word;
		this.nthWordOccurrence = nthWordOccurrence;
		this.probTrue = probTrue;
		this.isTarget = isTarget;
		this.mentioned = mentioned;
	}

	/**
	 * @return the mentioned
	 */
	public double getMentioned() {
		return mentioned;
	}

	public int getNthWordOccurrence() {
		return nthWordOccurrence;
	}

	/**
	 * @return the probTrue
	 */
	public double getProbTrue() {
		return probTrue;
	}

	public int getRound() {
		return round;
	}

	public String getWord() {
		return word;
	}

	public boolean isInstructor() {
		return isInstructor;
	}

	/**
	 * @return the isTarget
	 */
	public boolean isTarget() {
		return isTarget;
	}
}
