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
import java.net.URI;
import java.nio.file.Path;

import org.apache.batik.anim.dom.SAXSVGDocumentFactory;
import org.apache.batik.util.XMLResourceDescriptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.css.CSSStyleDeclaration;
import org.w3c.dom.svg.SVGDocument;
import org.w3c.dom.svg.SVGSVGElement;

/**
 * This class is thread-safe.
 *
 * @author <a href="mailto:errantlinguist+github@gmail.com>Todd Shore</a>
 * @since 3 Jan 2018
 *
 */
public final class SVGDocuments {

	private static final Logger LOGGER = LoggerFactory.getLogger(SVGDocuments.class);

	private static final StyleDeclarationParser STYLE_DECLARATION_PARSER = new StyleDeclarationParser();

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
	public static SVGDocument read(final Path infile) throws IOException {
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
	public static SVGDocument read(final String uri) throws IOException {
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
	public static SVGDocument read(final URI uri) throws IOException {
		return read(uri.toString());
	}

	public static void removeSize(final SVGSVGElement elem) {
		// This has been tested; The property changes are in fact persisted
		final String tag = elem.getTagName();
		LOGGER.debug("Original dimensions of element \"{}\" are {} * {}.", tag,
				elem.getWidth().getBaseVal().getValueAsString(), elem.getHeight().getBaseVal().getValueAsString());
		// https://xmlgraphics.apache.org/batik/faq.html#changes-are-not-rendered
		elem.removeAttributeNS(null, "width");
		elem.removeAttributeNS(null, "height");
	}

	/**
	 *
	 * @param doc
	 *            The {@link Document} containing the paths to which the given
	 *            property is to be set.
	 * @param propertyName
	 *            The name of the CSS property. See the CSS property index.
	 * @param value
	 *            The new value of the property.
	 */
	public static void setPathStyles(final Document doc, final String propertyName, final String value) {
		setPathStyles(doc, propertyName, value, "");
	}

	/**
	 *
	 * @param doc
	 *            The {@link Document} containing the paths to which the given
	 *            property is to be set.
	 * @param propertyName
	 *            The name of the CSS property. See the CSS property index.
	 * @param value
	 *            The new value of the property.
	 * @param priority
	 *            The new priority of the property (e.g. "important") or the empty
	 *            string if none.
	 */
	public static void setPathStyles(final Document doc, final String propertyName, final String value,
			final String priority) {
		final NodeList pathNodes = doc.getElementsByTagName("path");
		for (int pathNodeIdx = 0; pathNodeIdx < pathNodes.getLength(); ++pathNodeIdx) {
			final Node pathNode = pathNodes.item(pathNodeIdx);
			setStyle(pathNode, propertyName, value, priority);
		}
	}

	public static void setSize(final SVGSVGElement elem, final String width, final String height) {
		// This has been tested; The property changes are in fact persisted
		final String tag = elem.getTagName();
		LOGGER.debug("Original dimensions of element \"{}\" are {} * {}.", tag,
				elem.getWidth().getBaseVal().getValueAsString(), elem.getHeight().getBaseVal().getValueAsString());
		// https://xmlgraphics.apache.org/batik/faq.html#changes-are-not-rendered
		elem.setAttributeNS(null, "width", width);
		elem.setAttributeNS(null, "height", height);
		LOGGER.debug("New dimensions of element \"{}\" are {} * {}.", tag,
				elem.getWidth().getBaseVal().getValueAsString(), elem.getHeight().getBaseVal().getValueAsString());
	}

	/**
	 *
	 * @param node
	 *            The {@link Node} to set the property for.
	 * @param propertyName
	 *            The name of the CSS property. See the CSS property index.
	 * @param value
	 *            The new value of the property.
	 * @param priority
	 *            The new priority of the property (e.g. "important") or the empty
	 *            string if none.
	 */
	public static void setStyle(final Node node, final String propertyName, final String value, final String priority) {
		// final CSSStyleDeclaration style = pathNode.getStyle();
		// NOTE: For whatever reason "SVGOMPathElement.getStyle()" throws a
		// NullPointerException
		final NamedNodeMap pathNodeAttrs = node.getAttributes();
		final Node styleAttrNode = pathNodeAttrs.getNamedItem("style");
		final String styleStr = styleAttrNode.getTextContent();
		final CSSStyleDeclaration styleDeclaration = STYLE_DECLARATION_PARSER.apply(styleStr);
		styleDeclaration.setProperty(propertyName, value, priority);
		styleAttrNode.setTextContent(styleDeclaration.getCssText());
	}

	private SVGDocuments() {
	}

}
