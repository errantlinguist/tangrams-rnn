package se.kth.speech.coin.tangrams.logistic;

public final class PredictionException extends ClassifierException {

	public PredictionException() {
		super();
	}

	public PredictionException(String message) {
		super(message);
	}

	public PredictionException(String message, Throwable cause) {
		super(message, cause);
	}

	public PredictionException(Throwable cause) {
		super(cause);
	}

	protected PredictionException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}
}
