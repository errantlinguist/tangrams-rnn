package se.kth.speech.coin.tangrams.logistic;

public abstract class ClassifierException extends Exception {

	protected ClassifierException() {
		super();
	}

	protected ClassifierException(String message) {
		super(message);
	}

	protected ClassifierException(String message, Throwable cause) {
		super(message, cause);
	}

	protected ClassifierException(Throwable cause) {
		super(cause);
	}

	protected ClassifierException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}
}
