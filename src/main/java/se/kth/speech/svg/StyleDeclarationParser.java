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
package se.kth.speech.svg;

import java.io.IOException;
import java.io.StringReader;
import java.io.UncheckedIOException;
import java.util.function.Function;

import org.w3c.css.sac.InputSource;
import org.w3c.dom.css.CSSStyleDeclaration;

import com.steadystate.css.parser.CSSOMParser;
import com.steadystate.css.parser.SACParserCSS2;

/**
 * This class is thread-safe. <strong>NOTE:</strong> SVG uses a superset of CSS2
 * styling.
 *
 * @see <a href="https://www.w3.org/TR/SVG11/styling.html">Scalable Vector
 *      Graphics (SVG) 1.1 (Second Edition) &mdash; 6. Styling</a>
 * @author <a href="mailto:errantlinguist+github@gmail.com>Todd Shore</a>
 * @since 3 Jan 2018
 *
 */
final class StyleDeclarationParser implements Function<String, CSSStyleDeclaration> {

	private static final ThreadLocal<CSSOMParser> CSS_PARSER = new ThreadLocal<CSSOMParser>() {

		/*
		 * (non-Javadoc)
		 *
		 * @see java.lang.ThreadLocal#initialValue()
		 */
		@Override
		protected CSSOMParser initialValue() {
			return new CSSOMParser(new SACParserCSS2());
		}

	};

	@Override
	public CSSStyleDeclaration apply(final String styleDeclaration) {
		final InputSource source = new InputSource(new StringReader(styleDeclaration));
		final CSSOMParser parser = CSS_PARSER.get();
		try {
			return parser.parseStyleDeclaration(source);
		} catch (final IOException e) {
			// Shouldn't be thrown for a StringReader as created above
			throw new UncheckedIOException(e);
		}
	}

}
