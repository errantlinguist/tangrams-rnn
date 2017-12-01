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
 * @author <a href="mailto:tcshore@kth.se">Todd Shore</a>
 * @since Nov 24, 2017
 *
 */
public class ClassificationException extends RuntimeException { // NO_UCD (use default)

	/**
	 *
	 */
	private static final long serialVersionUID = 1651789774893161033L;

	/**
	 * @param message
	 * @param cause
	 */
	public ClassificationException(final String message, final Throwable cause) {
		super(message, cause);

	}

	/**
	 * @param message
	 * @param cause
	 * @param enableSuppression
	 * @param writableStackTrace
	 */
	public ClassificationException(final String message, final Throwable cause, final boolean enableSuppression,
			final boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);

	}

	/**
	 * @param cause
	 */
	public ClassificationException(final Throwable cause) {
		super(cause);

	}

}
