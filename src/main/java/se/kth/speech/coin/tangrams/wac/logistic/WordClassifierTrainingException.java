/*
 * 	Copyright 2017 Todd Shore
 *
 *	Licensed under the Apache License, Version 2.0 (the "License");
 *	you may not use this file except in compliance with the License.
 *	You may obtain a copy of the License at
 *
 *		http://www.apache.org/licenses/LICENSE-2.0
 *
 *	Unless required by applicable law or agreed to in writing, software
 *	distributed under the License is distributed on an "AS IS" BASIS,
 *	WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *	See the License for the specific language governing permissions and
 *	limitations under the License.
 */
package se.kth.speech.coin.tangrams.wac.logistic;

/**
 * @author <a href="mailto:errantlinguist+github@gmail.com">Todd Shore</a>
 * @since Nov 21, 2017
 *
 */
public class WordClassifierTrainingException extends RuntimeException {

	/**
	 *
	 */
	private static final long serialVersionUID = -1812653455419224580L;

	private static String createDefaultMessage(final String word, final Throwable cause) {
		return String.format("A(n) %s occurred while training a classifier for the word \"%s\". Original exception message: \"%s\"",
				cause.getClass().getSimpleName(), word, cause.getLocalizedMessage());
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
		this(word, createDefaultMessage(word, cause), cause);
	}

	/**
	 * @return the word
	 */
	public String getWord() {
		return word;
	}

}
