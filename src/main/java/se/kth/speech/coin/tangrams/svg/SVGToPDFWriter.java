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
package se.kth.speech.coin.tangrams.svg;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Supplier;

import org.apache.batik.transcoder.SVGAbstractTranscoder;
import org.apache.batik.transcoder.TranscoderException;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.MissingOptionException;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.fop.svg.PDFTranscoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import se.kth.speech.function.ThrowingSupplier;

/**
 * @author <a href="mailto:errantlinguist+github@gmail.com>Todd Shore</a>
 * @since 7 Mar 2017
 *
 */
public final class SVGToPDFWriter {

	private enum Parameter implements Supplier<Option> {
		HEIGHT("h") {
			@Override
			public Option get() {
				return Option.builder(optName).longOpt("height").desc("The output image height.").hasArg()
						.type(Number.class).argName("px").build();
			}
		},
		HELP("?") {
			@Override
			public Option get() {
				return Option.builder(optName).longOpt("help").desc("Prints this message.").build();
			}
		},

		OUTPATH("o") {
			@Override
			public Option get() {
				return Option.builder(optName).longOpt("output").desc(
						"If a single input file is specified; This is the path of the file to write the output to. If multiple files and/or directories are specified, this is the directory to which all output files will be written.")
						.hasArg().type(File.class).argName("path").build();
			}
		},
		WIDTH("w") {
			@Override
			public Option get() {
				return Option.builder(optName).longOpt("width").desc("The output image width.").hasArg()
						.type(Number.class).argName("px").build();
			}
		};

		private static final Options OPTIONS = createOptions();

		private static Options createOptions() {
			final Options result = new Options();
			Arrays.stream(Parameter.values()).map(Parameter::get).forEach(result::addOption);
			return result;
		}

		private static Consumer<SVGAbstractTranscoder> createTranscoderConfigurator(final CommandLine cl)
				throws ParseException {
			final Consumer<SVGAbstractTranscoder> result;
			final Optional<Consumer<SVGAbstractTranscoder>> optWidthConfigurator = parseTranscoderWidthConfigurator(cl);
			final Optional<Consumer<SVGAbstractTranscoder>> optHeightConfigurator = parseTranscoderHeightConfigurator(
					cl);
			if (optWidthConfigurator.isPresent()) {
				final Consumer<SVGAbstractTranscoder> widthConfigurator = optWidthConfigurator.get();
				result = optHeightConfigurator.map(heightConfigurator -> widthConfigurator.andThen(heightConfigurator))
						.orElse(widthConfigurator);
			} else if (optHeightConfigurator.isPresent()) {
				result = optHeightConfigurator.get();
			} else {
				result = transcoder -> {
					// Do nothing
				};
			}
			return result;
		}

		private static ThrowingSupplier<OutputStream, IOException> parseOutpath(final File outfile) throws IOException {
			final ThrowingSupplier<OutputStream, IOException> result;
			if (outfile == null) {
				LOGGER.info("No output file path specified; Will write to standard output stream.");
				result = () -> System.out;
			} else {
				LOGGER.info("Output file path is \"{}\".", outfile);
				result = createNewFileSupplier(outfile.toPath());
			}
			return result;
		}

		private static Optional<Consumer<SVGAbstractTranscoder>> parseTranscoderHeightConfigurator(final CommandLine cl)
				throws ParseException {
			final Number optValue = (Number) cl.getParsedOptionValue(Parameter.HEIGHT.optName);
			Optional<Consumer<SVGAbstractTranscoder>> result;
			if (optValue == null) {
				result = Optional.empty();
			} else {
				final Float value = Float.valueOf(optValue.floatValue());
				LOGGER.info("Will set image height to {}.", value);
				result = Optional
						.of(transcoder -> transcoder.addTranscodingHint(SVGAbstractTranscoder.KEY_HEIGHT, value));
			}
			return result;
		}

		private static Optional<Consumer<SVGAbstractTranscoder>> parseTranscoderWidthConfigurator(final CommandLine cl)
				throws ParseException {
			final Number optValue = (Number) cl.getParsedOptionValue(Parameter.WIDTH.optName);
			Optional<Consumer<SVGAbstractTranscoder>> result;
			if (optValue == null) {
				result = Optional.empty();
			} else {
				final Float value = Float.valueOf(optValue.floatValue());
				LOGGER.info("Will set image width to {}.", value);
				result = Optional
						.of(transcoder -> transcoder.addTranscodingHint(SVGAbstractTranscoder.KEY_WIDTH, value));
			}
			return result;
		}

		private static void printHelp() {
			final HelpFormatter formatter = new HelpFormatter();
			formatter.printHelp(SVGToPDFWriter.class.getName() + " INPATHS...", OPTIONS);
		}

		protected final String optName;

		private Parameter(final String optName) {
			this.optName = optName;
		}

	}

	private static final Logger LOGGER = LoggerFactory.getLogger(SVGToPDFWriter.class);

	public static void main(final CommandLine cl)
			throws TranscoderException, IOException, URISyntaxException, ParseException {
		if (cl.hasOption(Parameter.HELP.optName)) {
			Parameter.printHelp();
		} else {
			final Path[] inpaths = cl.getArgList().stream().map(Paths::get).toArray(Path[]::new);
			final File outpath = (File) cl.getParsedOptionValue(Parameter.OUTPATH.optName);
			final Consumer<SVGAbstractTranscoder> transcoderConfigurator = Parameter.createTranscoderConfigurator(cl);
			final Supplier<PDFTranscoder> transcoderFactory = () -> {
				final PDFTranscoder transcoder = new PDFTranscoder();
				transcoderConfigurator.accept(transcoder);
				return transcoder;
			};
			final TranscodingWriter writer = new TranscodingWriter(transcoderFactory, "pdf");

			switch (inpaths.length) {
			case 0: {
				throw new MissingOptionException("No input path(s) specified.");
			}
			case 1: {
				final Path inpath = inpaths[0];
				if (Files.isDirectory(inpath)) {
					if (outpath == null) {
						writer.write(inpath);
					} else {
						writer.write(inpath, outpath.toPath());
					}

				} else {
					final ThrowingSupplier<OutputStream, IOException> singleFileOsSupplier = Parameter
							.parseOutpath(outpath);
					writer.write(inpath.toUri(), singleFileOsSupplier);
				}
				break;
			}
			default: {
				writer.write(inpaths);
				break;
			}
			}
		}
	}

	public static void main(final String[] args) throws TranscoderException, IOException, URISyntaxException {
		final CommandLineParser parser = new DefaultParser();
		try {
			final CommandLine cl = parser.parse(Parameter.OPTIONS, args);
			main(cl);
		} catch (final ParseException e) {
			System.out.println(String.format("An error occured while parsing the command-line arguments: %s", e));
			Parameter.printHelp();
		}
	}

	private static OutputStream createNewFile(final Path outfile) throws IOException {
		return Files.newOutputStream(outfile, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
	}

	private static ThrowingSupplier<OutputStream, IOException> createNewFileSupplier(final Path outfile) {
		return () -> createNewFile(outfile);
	}

	// /**
	// *
	// * @param lengthValMatcher
	// * The {@link Matcher} object matching the length to normalize
	// * @param userAgent
	// * The {@link UserAgent} instance to use for converting lengths
	// * to pixel equivalents.
	// * @return A floating-point value representing the matched length in
	// pixels.
	// */
	// private static float createPixelLength(final Matcher lengthValMatcher,
	// final UserAgent userAgent) {
	// float result = Float.parseFloat(lengthValMatcher.group(1));
	// if (lengthValMatcher.groupCount() > 1) {
	// final String measurement = lengthValMatcher.group(2);
	// if (measurement.equalsIgnoreCase("mm")) {
	// result = result / userAgent.getPixelUnitToMillimeter();
	// }
	// }
	// return result;
	// }
	//
	// /**
	// * Finds the width of the widest <code>svg</code> element and the height
	// of
	// * the tallest <code>svg</code> element in a given {@link Document} in
	// * pixels.
	// *
	// * @param doc
	// * The {@code Document} to find the maximum dimensions for.
	// * @param userAgent
	// * The {@link UserAgent} instance to use for converting lengths
	// * to pixel equivalents.
	// * @return A two-element array of <code>{width, height}</code>.
	// */
	// private static float[] findMaxDimensions(final Document doc, final
	// UserAgent userAgent) {
	// final Matcher lengthValMatcher = LENGTH_MEASUREMENT_PATTERN.matcher("");
	// final NodeList svgNodes = doc.getElementsByTagName("svg");
	// float maxWidth = -1;
	// float maxHeight = -1;
	// for (int i = 0; i < svgNodes.getLength(); ++i) {
	// final Node svgNode = svgNodes.item(i);
	// final NamedNodeMap svgNodeAttrs = svgNode.getAttributes();
	// {
	// final Node svgWidthAttrNode = svgNodeAttrs.getNamedItem("width");
	// lengthValMatcher.reset(svgWidthAttrNode.getTextContent());
	// if (lengthValMatcher.matches()) {
	// final float width = createPixelLength(lengthValMatcher, userAgent);
	// maxWidth = Math.max(maxWidth, width);
	// }
	// }
	// {
	// final Node svgHeightAttrNode = svgNodeAttrs.getNamedItem("height");
	// lengthValMatcher.reset(svgHeightAttrNode.getTextContent());
	// if (lengthValMatcher.matches()) {
	// final float height = createPixelLength(lengthValMatcher, userAgent);
	// maxHeight = Math.max(maxHeight, height);
	// }
	// }
	//
	// }
	// return new float[] { maxWidth, maxHeight };
	// }

}
