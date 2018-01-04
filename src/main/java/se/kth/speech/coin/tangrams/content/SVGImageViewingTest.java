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

import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.Toolkit;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

import javax.swing.JFrame;
import javax.swing.WindowConstants;

import org.apache.batik.swing.JSVGCanvas;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.css.CSSStyleDeclaration;
import org.w3c.dom.svg.SVGDocument;

/**
 * @author <a href="mailto:errantlinguist+github@gmail.com>Todd Shore</a>
 * @since 7 Mar 2017
 *
 */
public final class SVGImageViewingTest {

	// private static final Logger LOGGER =
	// LoggerFactory.getLogger(SVGImageViewingTest.class);

	private static final StyleDeclarationParser STYLE_DECLARATION_PARSER = new StyleDeclarationParser();

	public static void main(final String[] args) throws IOException {
		if (args.length != 1) {
			System.err.println(String.format("Usage: %s <infile>", SVGImageViewingTest.class.getName()));
			System.exit(64);
		} else {
			final Path infilePath = Paths.get(args[0]);
			final SVGDocument doc = SVGDocuments.read(infilePath.toUri().toString());
			// Change the properties of the SVG document before rendering
			setPathStyles(doc, "fill", "purple");
			final JFrame frame = new JFrame("Image viewer");
			final JSVGCanvas canvas = new JSVGCanvas();
			// canvas.setDocumentState(JSVGComponent.ALWAYS_DYNAMIC);
			canvas.setDocument(doc);
			final Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
			final int minDim = Math.min(screenSize.width, screenSize.height);
			final int imgDim = Math.toIntExact(Math.round(Math.floor(minDim * 0.8f)));
			// NOTE: The "width" and "height" attributes of the SVG document
			// need not be
			// changed in order to change the display size
			canvas.setPreferredSize(new Dimension(imgDim, imgDim));
			frame.add(canvas);

			EventQueue.invokeLater(() -> {
				frame.pack();
				frame.setLocationByPlatform(true);
				frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
				frame.setVisible(true);
			});
		}
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
	private static void setPathStyles(final Document doc, final String propertyName, final String value) {
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
	 *            The new priority of the property (e.g. "important") or the
	 *            empty string if none.
	 */
	private static void setPathStyles(final Document doc, final String propertyName, final String value,
			final String priority) {
		final NodeList pathNodes = doc.getElementsByTagName("path");
		for (int pathNodeIdx = 0; pathNodeIdx < pathNodes.getLength(); ++pathNodeIdx) {
			final Node pathNode = pathNodes.item(pathNodeIdx);
			setStyle(pathNode, propertyName, value, priority);
		}
	}

	// private static void setSize(final SVGDocument doc, final float width,
	// final float height, final String unit) {
	// final SVGSVGElement rootElem = doc.getRootElement();
	// setSize(rootElem, width + unit, height + unit);
	// }
	//
	// private static void setSize(final SVGSVGElement elem, final String width,
	// final String height) {
	// // This has been tested; The property changes are in fact persisted
	// final String tag = elem.getTagName();
	// LOGGER.info("Original dimensions of element \"{}\" are {} * {}.", tag,
	// elem.getWidth().getBaseVal().getValueAsString(),
	// elem.getHeight().getBaseVal().getValueAsString());
	// // https://xmlgraphics.apache.org/batik/faq.html#changes-are-not-rendered
	// elem.setAttributeNS(null, "width", width);
	// elem.setAttributeNS(null, "height", height);
	// LOGGER.info("New dimensions of element \"{}\" are {} * {}.", tag,
	// elem.getWidth().getBaseVal().getValueAsString(),
	// elem.getHeight().getBaseVal().getValueAsString());
	// }

	/**
	 *
	 * @param node
	 *            The {@link Node} to set the property for.
	 * @param propertyName
	 *            The name of the CSS property. See the CSS property index.
	 * @param value
	 *            The new value of the property.
	 * @param priority
	 *            The new priority of the property (e.g. "important") or the
	 *            empty string if none.
	 */
	private static void setStyle(final Node node, final String propertyName, final String value,
			final String priority) {
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

}
