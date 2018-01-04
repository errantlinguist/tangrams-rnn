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
package se.kth.speech.coin.tangrams.content;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Path;

import org.apache.batik.anim.dom.SAXSVGDocumentFactory;
import org.apache.batik.util.XMLResourceDescriptor;
import org.w3c.dom.svg.SVGDocument;

/**
 * This class is thread-safe.
 *
 * @author <a href="mailto:errantlinguist+github@gmail.com>Todd Shore</a>
 * @since 3 Jan 2018
 *
 */
final class SVGDocuments {

	private static final ThreadLocal<SAXSVGDocumentFactory> SVG_DOC_FACTORY = new ThreadLocal<SAXSVGDocumentFactory>() {

		private final String xmlParserName = XMLResourceDescriptor.getXMLParserClassName();

		/*
		 * (non-Javadoc)
		 *
		 * @see java.lang.ThreadLocal#initialValue()
		 */
		@Override
		protected SAXSVGDocumentFactory initialValue() {
			return new SAXSVGDocumentFactory(xmlParserName);
		}

	};

	/**
	 * Uses a {@link SAXSVGDocumentFactory} to parse the given URI into a DOM.
	 *
	 * @param infile
	 *            A {@link Path} identifying the SVG file to read.
	 * @return A {@link SVGDocument} instance that represents the SVG file.
	 * @throws IOException
	 *             if an error occurred while reading the document.
	 */
	static SVGDocument read(final Path infile) throws IOException {
		return read(infile.toUri());
	}

	/**
	 * Uses a {@link SAXSVGDocumentFactory} to parse the given URI into a DOM.
	 *
	 * @param uri
	 *            A URI identifying the SVG file to read.
	 * @return A {@link SVGDocument} instance that represents the SVG file.
	 * @throws IOException
	 *             if an error occurred while reading the document.
	 */
	static SVGDocument read(final String uri) throws IOException {
		final SAXSVGDocumentFactory factory = SVG_DOC_FACTORY.get();
		return factory.createSVGDocument(uri);
	}

	/**
	 * Uses a {@link SAXSVGDocumentFactory} to parse the given URI into a DOM.
	 *
	 * @param uri
	 *            A {@link URI} identifying the SVG file to read.
	 * @return A {@link SVGDocument} instance that represents the SVG file.
	 * @throws IOException
	 *             if an error occurred while reading the document.
	 */
	static SVGDocument read(final URI uri) throws IOException {
		return read(uri.toString());
	}

	private SVGDocuments() {
	}

}
