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

import java.awt.EventQueue;
import java.nio.file.Path;
import java.nio.file.Paths;

import javax.swing.JFrame;
import javax.swing.WindowConstants;

import org.apache.batik.anim.dom.SVGOMPathElement;
import org.apache.batik.swing.JSVGCanvas;
import org.apache.batik.swing.svg.JSVGComponent;
import org.apache.batik.swing.svg.SVGDocumentLoaderAdapter;
import org.apache.batik.swing.svg.SVGDocumentLoaderEvent;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.css.CSSStyleDeclaration;
import org.w3c.dom.svg.SVGAnimatedRect;
import org.w3c.dom.svg.SVGDocument;
import org.w3c.dom.svg.SVGRect;
import org.w3c.dom.svg.SVGSVGElement;

/**
 * @author <a href="mailto:errantlinguist+github@gmail.com>Todd Shore</a>
 * @since 7 Mar 2017
 *
 */
public final class SVGImageViewingTest {

	private static final StyleDeclarationParser STYLE_DECLARATION_PARSER = new StyleDeclarationParser();

	public static void main(final String[] args) {
		final Path infilePath = Paths.get(args[0]);
		final JFrame frame = new JFrame("Image viewer");
		// f.setLayout(new BorderLayout());
		// f.setPreferredSize(new Dimension(1000,100));
		final JSVGCanvas canvas = new JSVGCanvas();
		// canvas.setPreferredSize(new Dimension(1000,100));
		// canvas.set
		// canvas.setLayout(new BorderLayout());
		canvas.setDocumentState(JSVGComponent.ALWAYS_DYNAMIC);
		frame.add(canvas);
		EventQueue.invokeLater(() -> {
			canvas.addSVGDocumentLoaderListener(new SVGDocumentLoaderAdapter() {

				/*
				 * (non-Javadoc)
				 *
				 * @see org.apache.batik.swing.svg.SVGDocumentLoaderAdapter#
				 * documentLoadingCompleted(org.apache.batik.swing.svg. SVGDocumentLoaderEvent)
				 */
				@Override
				public void documentLoadingCompleted(final SVGDocumentLoaderEvent e) {
					final SVGDocument doc = e.getSVGDocument();
					setPathStyles(doc, "fill", "purple");

					final NodeList svgNodes = doc.getElementsByTagName("svg");
					for (int svgNodeIdx = 0; svgNodeIdx < svgNodes.getLength(); ++svgNodeIdx) {
						final SVGSVGElement svgNode = (SVGSVGElement) svgNodes.item(svgNodeIdx);
						final SVGAnimatedRect viewBox = svgNode.getViewBox();
						final SVGRect viewBoxVal = viewBox.getBaseVal();
						final float newWidth = viewBoxVal.getWidth() * 2;
						viewBoxVal.setWidth(newWidth);
						final float newHeight = viewBoxVal.getHeight() * 2;
						viewBoxVal.setHeight(newHeight);
						// svgNode.createSVGTransform().setScale(2.0f, 2.0f);
						System.out.println(svgNode);
						final NamedNodeMap svgAttrs = svgNode.getAttributes();
						final Node widthAttrNode = svgAttrs.getNamedItem("width");
						final String width = widthAttrNode.getTextContent();
						System.out.println("old width:" + width);
						// widthAttrNode.setTextContent("100%");
						// widthAttrNode.setTextContent("1000mm");
						// widthAttrNode.setTextContent(newWidth + "mm");
						System.out.println("new width:" + widthAttrNode.getTextContent());
						final Node heightAttrNode = svgAttrs.getNamedItem("height");
						final String height = heightAttrNode.getTextContent();
						System.out.println("old height:" + height);
						// heightAttrNode.setTextContent("100%");
						// heightAttrNode.setTextContent("2000mm");
						// heightAttrNode.setTextContent(newHeight + "mm");
						// svgAttrs.removeNamedItem("height");
						System.out.println("new height:" + heightAttrNode.getTextContent());
						// Node viewBoxAttr = svgAttrs.getNamedItem("viewBox");
						// String viewBoxAttrStr = viewBoxAttr.getTextContent();
						// viewBoxAttr.setTextContent("0 0 " + width + " " +
						// height);
					}

					final SVGSVGElement rootElem = doc.getRootElement();
					rootElem.createSVGTransform().setScale(2.0f, 2.0f);
					// rootElem.trans
					// rootElem.forceRedraw();

					// System.out.println("currentScale:" +
					// rootElem.getCurrentScale());
					// rootElem.
					// rootElem.createSVGTransform()
					// rootElem.getHeight();
					// rootElem.setCurrentScale(2.0f);

					// EventQueue.invokeLater(()-> {
					// JFrame conv = new JFrame("Converted");
					// JSVGCanvas convCanvas = new JSVGCanvas();
					// conv.add(convCanvas);
					// convCanvas.setDocumentState(JSVGComponent.ALWAYS_DYNAMIC);
					// convCanvas.setSVGDocument(doc);
					// convCanvas.addSVGDocumentLoaderListener(new
					// SVGDocumentLoaderAdapter(){
					//
					// });
					// conv.pack();
					//// conv.setLocation(null);
					// conv.setVisible(true);
					// });

					// try {
					// BufferedImage img = convertSVGToPNG(doc);
					// EventQueue.invokeLater(() -> {
					// JFrame c = new JFrame("Converted");
					// c.add(new JLabel(new ImageIcon(img)));
					// c.pack();
					// c.setLocationByPlatform(true);
					// c.setVisible(true);
					// });
					// } catch (IOException e1) {
					// throw new UncheckedIOException(e1);
					// } catch (TranscoderException e1) {
					// throw new RuntimeException(e1);
					// }

					// canvas.setSVGDocument(doc);
					// f.invalidate();
					// canvas.repaint();
				}

			});
			canvas.setURI(infilePath.toString());
			canvas.setVisible(true);

			frame.pack();
			frame.setLocationByPlatform(true);
			frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
			frame.setVisible(true);
		});
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
	 *            The new priority of the property (e.g. "important") or the empty
	 *            string if none.
	 */
	private static void setPathStyles(final Document doc, final String propertyName, final String value,
			final String priority) {
		final NodeList pathNodes = doc.getElementsByTagName("path");
		for (int pathNodeIdx = 0; pathNodeIdx < pathNodes.getLength(); ++pathNodeIdx) {
			final SVGOMPathElement pathNode = (SVGOMPathElement) pathNodes.item(pathNodeIdx);
			// final CSSStyleDeclaration style = pathNode.getStyle();
			// NOTE: For whatever reason "SVGOMPathElement.getStyle()" throws a
			// NullPointerException
			final NamedNodeMap pathNodeAttrs = pathNode.getAttributes();
			final Node styleAttrNode = pathNodeAttrs.getNamedItem("style");
			final String styleStr = styleAttrNode.getTextContent();
			final CSSStyleDeclaration styleDeclaration = STYLE_DECLARATION_PARSER.apply(styleStr);
			styleDeclaration.setProperty(propertyName, value, priority);
			styleAttrNode.setTextContent(styleDeclaration.getCssText());
			// parseStyle(styleStr);
			// styleAttrNode.setTextContent(styleStr + ";" + styleToAppend);

			// final Node transformAttr =
			// pathNodeAttrs.getNamedItem("transform");
			// final String transformStr =
			// transformAttr.getTextContent();
			// final String scaledTransformStr = transformStr + "
			// scale(1.0)";
			// transformAttr.setTextContent(scaledTransformStr);
		}
	}

}
