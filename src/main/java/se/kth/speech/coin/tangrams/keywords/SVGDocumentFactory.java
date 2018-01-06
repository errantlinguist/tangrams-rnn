/*
 * 	Copyright 2018 Todd Shore
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
package se.kth.speech.coin.tangrams.keywords;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Function;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.svg.SVGDocument;

import se.kth.speech.svg.SVGDocuments;

/**
 * @author <a href="mailto:tcshore@kth.se">Todd Shore</a>
 * @since 5 Jan 2018
 *
 */
final class SVGDocumentFactory implements Function<VisualizableReferent, SVGDocument> {

	private static final Logger LOGGER = LoggerFactory.getLogger(SVGDocumentFactory.class);

	private static String createColorHexCode(final int r, final int g, final int b) {
		// https://stackoverflow.com/a/3607942/1391325
		return String.format("#%02x%02x%02x", r, g, b);
	}

	private static String createColorHexCode(final VisualizableReferent ref) {
		return createColorHexCode(ref.getRed(), ref.getGreen(), ref.getBlue());
	}

	private final Path imgResDir;

	private final List<? extends BiConsumer<? super VisualizableReferent, ? super SVGDocument>> postProcessors;

	public SVGDocumentFactory(final Path imgResDir,
			final List<? extends BiConsumer<? super VisualizableReferent, ? super SVGDocument>> postProcessors) {
		this.imgResDir = imgResDir;
		this.postProcessors = postProcessors;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.util.function.Function#apply(java.lang.Object)
	 */
	@Override
	public SVGDocument apply(final VisualizableReferent ref) {
		final Path imgFilePath = imgResDir.resolve(ref.getShape() + ".svg");
		LOGGER.debug("Loading image from \"{}\".", imgFilePath);
		final SVGDocument result;
		try {
			result = SVGDocuments.read(imgFilePath);
		} catch (final IOException e) {
			throw new UncheckedIOException(e);
		}
		SVGDocuments.setPathStyles(result, "fill", createColorHexCode(ref));
		postProcessors.forEach(postProcessor -> postProcessor.accept(ref, result));
		return result;
	}

}
