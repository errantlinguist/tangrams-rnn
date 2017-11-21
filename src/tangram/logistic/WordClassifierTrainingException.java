/**
 *
 */
package tangram.logistic;

/**
 * @author <a href="mailto:errantlinguist+github@gmail.com">Todd Shore</a>
 * @since Nov 21, 2017
 *
 */
public final class WordClassifierTrainingException extends RuntimeException {

	/**
	 *
	 */
	private static final long serialVersionUID = -1812653455419224580L;

	private static String createDefaultMessage(final String word, final Throwable cause) {
		return String.format("A(n) %s occurred while training a classifier for the word \"%s\"",
				cause.getClass().getSimpleName(), word);
	}

	private final String word;

	public WordClassifierTrainingException(final String word, final String message, final Throwable cause) {
		super(message, cause);
		this.word = word;
	}

	/**
	 * @param message
	 * @param cause
	 */
	public WordClassifierTrainingException(final String word, final Throwable cause) {
		this(createDefaultMessage(word, cause), word, cause);
	}

	/**
	 * @return the word
	 */
	public String getWord() {
		return word;
	}

}
