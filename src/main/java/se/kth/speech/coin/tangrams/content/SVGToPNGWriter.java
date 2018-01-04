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

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

import org.apache.batik.transcoder.TranscoderException;
import org.apache.batik.transcoder.TranscoderInput;
import org.apache.batik.transcoder.TranscoderOutput;
import org.apache.batik.transcoder.image.PNGTranscoder;
import org.w3c.dom.Document;

/**
 * @author <a href="mailto:errantlinguist+github@gmail.com>Todd Shore</a>
 * @since 7 Mar 2017
 *
 */
public final class SVGToPNGWriter {

//	private static final Pattern LENGTH_MEASUREMENT_PATTERN = Pattern.compile("(\\d+(?:\\.\\d+)?)(\\S*)");

	public static void main(final String[] args) throws TranscoderException, IOException, URISyntaxException {
		if (args.length != 2) {
			System.err.println(String.format("Usage: %s <infile> <outfile>", SVGToPNGWriter.class.getName()));
			System.exit(64);
		} else {
			final Path infilePath = Paths.get(args[0]);
			final Path outfilePath = Paths.get(args[1]);
			System.out.print(infilePath + " > ");
			write(infilePath.toUri(), outfilePath);
			System.out.println(outfilePath);
		}

	}

	/**
	 * @see <a href=
	 *      "http://stackoverflow.com/q/32721467/1391325">StackOverflow</a>
	 * @param doc
	 * @param outpath
	 * @throws TranscoderException
	 * @throws IOException
	 */
	public static void write(final Document doc, final Path outpath) throws TranscoderException, IOException {
		final ByteArrayOutputStream resultByteStream = new ByteArrayOutputStream();
		final TranscoderInput transcoderInput = new TranscoderInput(doc);
		final TranscoderOutput transcoderOutput = new TranscoderOutput(resultByteStream);

		final PNGTranscoder pngTranscoder = new PNGTranscoder();
//		final UserAgent userAgent = pngTranscoder.getUserAgent();
//		final float[] maxDimensions = findMaxDimensions(doc, userAgent);
//		pngTranscoder.addTranscodingHint(SVGAbstractTranscoder.KEY_WIDTH, Float.valueOf(maxDimensions[0]));
//		pngTranscoder.addTranscodingHint(SVGAbstractTranscoder.KEY_HEIGHT, Float.valueOf(maxDimensions[1]));
		pngTranscoder.transcode(transcoderInput, transcoderOutput);

		try (OutputStream os = new BufferedOutputStream(
				Files.newOutputStream(outpath, StandardOpenOption.CREATE, StandardOpenOption.APPEND))) {
			resultByteStream.writeTo(os);
			// writer.flush();
		}

	}

	/**
	 * @see <a href=
	 *      "http://stackoverflow.com/q/32721467/1391325">StackOverflow</a>
	 * @param inputUri
	 * @param outpath
	 * @throws TranscoderException
	 * @throws IOException
	 */
	public static void write(final URI inputUri, final Path outpath) throws TranscoderException, IOException {
		write(SVGDocuments.read(inputUri), outpath);
	}

//	/**
//	 *
//	 * @param lengthValMatcher
//	 *            The {@link Matcher} object matching the length to normalize
//	 * @param userAgent
//	 *            The {@link UserAgent} instance to use for converting lengths
//	 *            to pixel equivalents.
//	 * @return A floating-point value representing the matched length in pixels.
//	 */
//	private static float createPixelLength(final Matcher lengthValMatcher, final UserAgent userAgent) {
//		float result = Float.parseFloat(lengthValMatcher.group(1));
//		if (lengthValMatcher.groupCount() > 1) {
//			final String measurement = lengthValMatcher.group(2);
//			if (measurement.equalsIgnoreCase("mm")) {
//				result = result / userAgent.getPixelUnitToMillimeter();
//			}
//		}
//		return result;
//	}
//
//	/**
//	 * Finds the width of the widest <code>svg</code> element and the height of
//	 * the tallest <code>svg</code> element in a given {@link Document} in
//	 * pixels.
//	 *
//	 * @param doc
//	 *            The {@code Document} to find the maximum dimensions for.
//	 * @param userAgent
//	 *            The {@link UserAgent} instance to use for converting lengths
//	 *            to pixel equivalents.
//	 * @return A two-element array of <code>{width, height}</code>.
//	 */
//	private static float[] findMaxDimensions(final Document doc, final UserAgent userAgent) {
//		final Matcher lengthValMatcher = LENGTH_MEASUREMENT_PATTERN.matcher("");
//		final NodeList svgNodes = doc.getElementsByTagName("svg");
//		float maxWidth = -1;
//		float maxHeight = -1;
//		for (int i = 0; i < svgNodes.getLength(); ++i) {
//			final Node svgNode = svgNodes.item(i);
//			final NamedNodeMap svgNodeAttrs = svgNode.getAttributes();
//			{
//				final Node svgWidthAttrNode = svgNodeAttrs.getNamedItem("width");
//				lengthValMatcher.reset(svgWidthAttrNode.getTextContent());
//				if (lengthValMatcher.matches()) {
//					final float width = createPixelLength(lengthValMatcher, userAgent);
//					maxWidth = Math.max(maxWidth, width);
//				}
//			}
//			{
//				final Node svgHeightAttrNode = svgNodeAttrs.getNamedItem("height");
//				lengthValMatcher.reset(svgHeightAttrNode.getTextContent());
//				if (lengthValMatcher.matches()) {
//					final float height = createPixelLength(lengthValMatcher, userAgent);
//					maxHeight = Math.max(maxHeight, height);
//				}
//			}
//
//		}
//		return new float[] { maxWidth, maxHeight };
//	}

}
