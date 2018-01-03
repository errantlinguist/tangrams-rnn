/*
 *  This file is part of client.
 *
 *  se.kth.speech.coin.tangrams.client is free software: you can redistribute it and/or modify
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

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.batik.anim.dom.SAXSVGDocumentFactory;
import org.apache.batik.bridge.UserAgent;
import org.apache.batik.transcoder.SVGAbstractTranscoder;
import org.apache.batik.transcoder.TranscoderException;
import org.apache.batik.transcoder.TranscoderInput;
import org.apache.batik.transcoder.TranscoderOutput;
import org.apache.batik.transcoder.image.PNGTranscoder;
import org.apache.batik.util.XMLResourceDescriptor;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * @author <a href="mailto:tcshore@kth.se>Todd Shore</a>
 * @since 7 Mar 2017
 *
 */
public final class SVGIconImagePNGTranscoder {

	private static final Pattern LENGTH_MEASUREMENT_PATTERN = Pattern.compile("(\\d+(?:\\.\\d+)?)(\\S*)");

	/**
	 * @see <a href= "http://stackoverflow.com/q/32721467/1391325">StackOverflow</a>
	 * @param doc
	 * @throws TranscoderException
	 * @throws IOException
	 */
	public static void convertSVGToPNG(final String inputUri, final Path outpath)
			throws TranscoderException, IOException {
		final Document doc = createSVGDocument(inputUri);
		final ByteArrayOutputStream resultByteStream = new ByteArrayOutputStream();
		final TranscoderInput transcoderInput = new TranscoderInput(doc);
		final TranscoderOutput transcoderOutput = new TranscoderOutput(resultByteStream);

		final PNGTranscoder pngTranscoder = new PNGTranscoder();
		final UserAgent userAgent = pngTranscoder.getUserAgent();

		final float[] maxDimensions = findMaxDimensions(doc, userAgent);
		pngTranscoder.addTranscodingHint(SVGAbstractTranscoder.KEY_WIDTH, Float.valueOf(maxDimensions[0]));
		pngTranscoder.addTranscodingHint(SVGAbstractTranscoder.KEY_HEIGHT, Float.valueOf(maxDimensions[1]));
		pngTranscoder.transcode(transcoderInput, transcoderOutput);

		try (OutputStream os = new BufferedOutputStream(
				Files.newOutputStream(outpath, StandardOpenOption.CREATE, StandardOpenOption.APPEND))) {
			resultByteStream.writeTo(os);
			// writer.flush();
		}

	}

	public static void main(final String[] args) throws TranscoderException, IOException, URISyntaxException {
		if (args.length < 1) {
			System.err
					.println(String.format("Usage: %s <infile> <outfile>", SVGIconImagePNGTranscoder.class.getName()));
			System.exit(64);
		} else {
			final Path infilePath = Paths.get(args[0]);
			final Path outfilePath = Paths.get(args[1]);
			System.out.print(infilePath + " > ");
			convertSVGToPNG(infilePath.toString(), outfilePath);
			System.out.println(outfilePath);
		}

	}

	/**
	 *
	 * @param lengthValMatcher
	 *            The {@link Matcher} object matching the length to normalize
	 * @param userAgent
	 *            The {@link UserAgent} instance to use for converting lengths to
	 *            pixel equivalents.
	 * @return A floating-point value representing the matched length in pixels.
	 */
	private static float createPixelLength(final Matcher lengthValMatcher, final UserAgent userAgent) {
		float result = Float.parseFloat(lengthValMatcher.group(1));
		if (lengthValMatcher.groupCount() > 1) {
			final String measurement = lengthValMatcher.group(2);
			if (measurement.equalsIgnoreCase("mm")) {
				result = result / userAgent.getPixelUnitToMillimeter();
			}
		}
		return result;
	}

	/**
	 * Uses a {@link SAXSVGDocumentFactory} to parse the given URI into a DOM.
	 *
	 * @param uri
	 *            A URI identifying the SVG file to read.
	 * @return A {@link Document} instance that represents the SVG file.
	 * @throws IOException
	 *             if an error occured while reading the document.
	 */
	private static Document createSVGDocument(final String uri) throws IOException {
		final String parser = XMLResourceDescriptor.getXMLParserClassName();
		final SAXSVGDocumentFactory factory = new SAXSVGDocumentFactory(parser);
		return factory.createDocument(uri);
	}

	/**
	 * Finds the width of the widest element and the height of the tallest element
	 * in a given {@link Document} in pixels.
	 *
	 * @param doc
	 *            The {@code Document} to find the maximum dimensions for.
	 * @param userAgent
	 *            The {@link UserAgent} instance to use for converting lengths to
	 *            pixel equivalents.
	 * @return A two-element array of <code>{width, height}</code>.
	 */
	private static float[] findMaxDimensions(final Document doc, final UserAgent userAgent) {
		final Matcher lengthValMatcher = LENGTH_MEASUREMENT_PATTERN.matcher("");
		final NodeList svgNodes = doc.getElementsByTagName("svg");
		float maxWidth = -1;
		float maxHeight = -1;
		for (int i = 0; i < svgNodes.getLength(); ++i) {
			final Node svgNode = svgNodes.item(i);
			final NamedNodeMap svgNodeAttrs = svgNode.getAttributes();
			{
				final Node svgWidthAttrNode = svgNodeAttrs.getNamedItem("width");
				lengthValMatcher.reset(svgWidthAttrNode.getTextContent());
				if (lengthValMatcher.matches()) {
					final float width = createPixelLength(lengthValMatcher, userAgent);
					maxWidth = Math.max(maxWidth, width);
				}
			}
			{
				final Node svgHeightAttrNode = svgNodeAttrs.getNamedItem("height");
				lengthValMatcher.reset(svgHeightAttrNode.getTextContent());
				if (lengthValMatcher.matches()) {
					final float height = createPixelLength(lengthValMatcher, userAgent);
					maxHeight = Math.max(maxHeight, height);
				}
			}

		}
		return new float[] { maxWidth, maxHeight };
	}

}
