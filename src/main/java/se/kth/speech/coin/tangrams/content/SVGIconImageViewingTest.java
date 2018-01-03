/*
 *  This file is part of client.
 *
 *  client is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
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
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.svg.SVGAnimatedRect;
import org.w3c.dom.svg.SVGDocument;
import org.w3c.dom.svg.SVGRect;
import org.w3c.dom.svg.SVGSVGElement;

/**
 * @author <a href="mailto:tcshore@kth.se">Todd Shore</a>
 * @since 7 Mar 2017
 *
 */
public final class SVGIconImageViewingTest {

	public static void main(final String[] args) {
		final Path infilePath = Paths.get(args[0]);
		final JFrame f = new JFrame("Image viewer");
		// f.setLayout(new BorderLayout());
		// f.setPreferredSize(new Dimension(1000,100));
		final JSVGCanvas canvas = new JSVGCanvas();
		// canvas.setPreferredSize(new Dimension(1000,100));
		// canvas.set
		// canvas.setLayout(new BorderLayout());
		canvas.setDocumentState(JSVGComponent.ALWAYS_DYNAMIC);
		f.add(canvas);
		EventQueue.invokeLater(() -> {
			canvas.addSVGDocumentLoaderListener(new SVGDocumentLoaderAdapter() {

				/*
				 * (non-Javadoc)
				 *
				 * @see org.apache.batik.swing.svg.SVGDocumentLoaderAdapter#
				 * documentLoadingCompleted(org.apache.batik.swing.svg.
				 * SVGDocumentLoaderEvent)
				 */
				@Override
				public void documentLoadingCompleted(final SVGDocumentLoaderEvent e) {
					final SVGDocument doc = canvas.getSVGDocument();

					final NodeList pathNodes = doc.getElementsByTagName("path");
					for (int pathNodeIdx = 0; pathNodeIdx < pathNodes.getLength(); ++pathNodeIdx) {
						final SVGOMPathElement pathNode = (SVGOMPathElement) pathNodes.item(pathNodeIdx);
						// CSSStyleDeclaration style = pathNode.getStyle();
						// System.out.println(style);
						final NamedNodeMap pathNodeAttrs = pathNode.getAttributes();
						final Node styleAttrNode = pathNodeAttrs.getNamedItem("style");
						final String styleStr = styleAttrNode.getTextContent();
						// System.out.println(styleStr);
						styleAttrNode.setTextContent(styleStr + ";fill:purple");

						// final Node transformAttr =
						// pathNodeAttrs.getNamedItem("transform");
						// final String transformStr =
						// transformAttr.getTextContent();
						// final String scaledTransformStr = transformStr + "
						// scale(1.0)";
						// transformAttr.setTextContent(scaledTransformStr);
					}
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

			f.pack();
			f.setLocationByPlatform(true);
			f.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
			f.setVisible(true);
		});
	}

}
