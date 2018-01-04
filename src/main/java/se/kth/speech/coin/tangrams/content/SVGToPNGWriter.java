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

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.batik.transcoder.SVGAbstractTranscoder;
import org.apache.batik.transcoder.TranscoderException;
import org.apache.batik.transcoder.TranscoderInput;
import org.apache.batik.transcoder.TranscoderOutput;
import org.apache.batik.transcoder.image.PNGTranscoder;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.MissingOptionException;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;

import se.kth.speech.function.ThrowingSupplier;

/**
 * @author <a href="mailto:errantlinguist+github@gmail.com>Todd Shore</a>
 * @since 7 Mar 2017
 *
 */
public final class SVGToPNGWriter {

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
				return Option.builder(optName).longOpt("output")
						.desc("If a single input file is specified; This is the path of the file to write the output to. If multiple files and/or directories are specified, this is the directory to which all output files will be written.")
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
			formatter.printHelp(SVGToPNGWriter.class.getName() + " INPATHS...", OPTIONS);
		}

		protected final String optName;

		private Parameter(final String optName) {
			this.optName = optName;
		}

	}

	private static final Logger LOGGER = LoggerFactory.getLogger(SVGToPNGWriter.class);

	private static final Pattern SVG_EXT_PATTERN = Pattern.compile("^.+\\.(svgz?)$", Pattern.CASE_INSENSITIVE);

	public static void main(final String[] args) throws TranscoderException, IOException, URISyntaxException {
		final CommandLineParser parser = new DefaultParser();
		try {
			final CommandLine cl = parser.parse(Parameter.OPTIONS, args);
			if (cl.hasOption(Parameter.HELP.optName)) {
				Parameter.printHelp();
			} else {
				final Path[] inpaths = cl.getArgList().stream().map(Paths::get).toArray(Path[]::new);
				final File outpath = (File) cl.getParsedOptionValue(Parameter.OUTPATH.optName);
				final Consumer<SVGAbstractTranscoder> transcoderConfigurator = Parameter
						.createTranscoderConfigurator(cl);
				final SVGToPNGWriter writer = new SVGToPNGWriter(transcoderConfigurator);

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
		} catch (final ParseException e) {
			System.out.println(String.format("An error occured while parsing the command-line arguments: %s", e));
			Parameter.printHelp();
		}
	}

	public static void writeStatic(final Document doc,
			final ThrowingSupplier<OutputStream, IOException> outputStreamSupplier)
			throws TranscoderException, IOException {
		final ByteArrayOutputStream resultByteStream = new ByteArrayOutputStream();
		final TranscoderInput transcoderInput = new TranscoderInput(doc);
		final TranscoderOutput transcoderOutput = new TranscoderOutput(resultByteStream);

		final PNGTranscoder transcoder = new PNGTranscoder();
		transcoder.transcode(transcoderInput, transcoderOutput);

		try (OutputStream os = outputStreamSupplier.get()) {
			resultByteStream.writeTo(os);
			// writer.flush();
		}

	}

	public static void writeStatic(final URI inputUri,
			final ThrowingSupplier<OutputStream, IOException> outputStreamSupplier)
			throws TranscoderException, IOException {
		writeStatic(SVGDocuments.read(inputUri), outputStreamSupplier);
	}

	private static OutputStream createNewFile(final Path outfile) throws IOException {
		return Files.newOutputStream(outfile, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
	}

	private static ThrowingSupplier<OutputStream, IOException> createNewFileSupplier(final Path outfile) {
		return () -> createNewFile(outfile);
	}

	private static String createOutputFileName(final Path infile) {
		final Path infileName = infile.getFileName();
		final String infileNameStr = infileName == null ? "" : infileName.toString();
		return replaceGroup(SVG_EXT_PATTERN, infileNameStr, 1, "png");
	}

	private static Path createOutputFilePath(final Path infile) {
		final String outpathStr = replaceGroup(SVG_EXT_PATTERN, infile.toString(), 1, "png");
		return Paths.get(outpathStr);
	}

	private static Path createOutputFilePath(final Path infile, final Path outdir) {
		final String outfileNameStr = createOutputFileName(infile);
		return outdir.resolve(outfileNameStr);
	}

	/**
	 * Checks if a given {@link Path} definitely does not point to a file
	 * containing SVG content.
	 *
	 * @param path
	 *            The {@code Path} to check.
	 * @return <code>true</code> if the path does not point to a file containing
	 *         SVG content; <code>false</code> means the path <em>might</em>
	 *         point to SVG content, but this is not guaranteed.
	 */
	private static boolean isNotSvgFilePath(final Path path) {
		boolean result = false;
		try {
			result = !isSvgFilePath(path);
		} catch (final IOException e) {
			LOGGER.error(String.format("An IO exception occurred while probing the content type of \"%s\".", path), e);
		}
		return result;
	}

	/**
	 * Checks if a given {@link Path} most definitely points to a file
	 * containing SVG content.
	 *
	 * @param path
	 *            The {@code Path} to check.
	 * @return <code>true</code> if the path most definitely points to a file
	 *         containing SVG content; <code>false</code> means the path most
	 *         likely does not point to SVG content, but this is not guaranteed.
	 * @throws IOException
	 *             If an error occurs while probing the file content.
	 */
	private static boolean isSvgFilePath(final Path path) throws IOException {
		final boolean result;
		// result = SVG_EXT_PATTERN.matcher(path.toString()).matches();
		final String contentType = Files.probeContentType(path);
		if (contentType == null) {
			result = SVG_EXT_PATTERN.matcher(path.toString()).matches();
		} else {
			result = "image/svg+xml".equals(contentType);
		}
		return result;
	}

	private static String replaceGroup(final Pattern pattern, final String source, final int groupToReplace,
			final String replacement) {
		// https://stackoverflow.com/a/20999446/1391325
		final Matcher m = pattern.matcher(source);
		final String result;
		if (m.matches()) {
			result = new StringBuilder(source).replace(m.start(groupToReplace), m.end(groupToReplace), replacement)
					.toString();
		} else {
			result = source;
		}
		return result;
	}

	private static Iterator<Path> walkSvgFiles(final Path inpath) throws IOException {
		// Filtering first by content type and then by file vs. directory is
		// deliberate so that cases where e.g. file access is restricted will
		// result in an exception being thrown rather than simply being ignored
		return Files.walk(inpath).filter(path -> !isNotSvgFilePath(path)).filter(Files::isRegularFile).iterator();
	}

	private final Consumer<? super PNGTranscoder> transcoderConfigurator;

	public SVGToPNGWriter(final Consumer<? super PNGTranscoder> transcoderConfigurator) {
		this.transcoderConfigurator = transcoderConfigurator;
	}

	/**
	 * @see <a href=
	 *      "http://stackoverflow.com/q/32721467/1391325">StackOverflow</a>
	 * @param doc
	 * @param outfile
	 * @throws TranscoderException
	 * @throws IOException
	 */
	public void write(final Document doc, final Path outfile) throws TranscoderException, IOException {
		final ThrowingSupplier<OutputStream, IOException> osSupplier = createNewFileSupplier(outfile);
		write(doc, osSupplier);
	}

	/**
	 * @see <a href=
	 *      "http://stackoverflow.com/q/32721467/1391325">StackOverflow</a>
	 * @param doc
	 * @param outputStreamSupplier
	 * @throws TranscoderException
	 * @throws IOException
	 */
	public void write(final Document doc, final ThrowingSupplier<OutputStream, IOException> outputStreamSupplier)
			throws TranscoderException, IOException {
		final ByteArrayOutputStream resultByteStream = new ByteArrayOutputStream();
		final TranscoderInput transcoderInput = new TranscoderInput(doc);
		final TranscoderOutput transcoderOutput = new TranscoderOutput(resultByteStream);

		final PNGTranscoder transcoder = new PNGTranscoder();
		transcoderConfigurator.accept(transcoder);
		transcoder.transcode(transcoderInput, transcoderOutput);

		try (OutputStream os = outputStreamSupplier.get()) {
			resultByteStream.writeTo(os);
		}

	}

	public void write(final Path inpath) throws IOException, TranscoderException {
		final Iterator<Path> infileIter = walkSvgFiles(inpath);
		while (infileIter.hasNext()) {
			final Path infile = infileIter.next();
			final Path outfile = createOutputFilePath(infile);
			writeSingleFile(infile, outfile);
		}
	}

	public void write(final Path inpath, Path outdir) throws IOException, TranscoderException {
		final Iterator<Path> infileIter = walkSvgFiles(inpath);
		if (infileIter.hasNext()) {
			outdir = Files.createDirectories(outdir);
			do {
				final Path infile = infileIter.next();
				final Path outfile = createOutputFilePath(infile, outdir);
				writeSingleFile(infile, outfile);
			} while (infileIter.hasNext());
		}
	}

	public void write(final Path[] inpaths) throws IOException, TranscoderException {
		for (final Path inpath : inpaths) {
			write(inpath);
		}
	}

	/**
	 * @see <a href=
	 *      "http://stackoverflow.com/q/32721467/1391325">StackOverflow</a>
	 * @param inputUri
	 * @param outfile
	 * @throws TranscoderException
	 * @throws IOException
	 */
	public void write(final URI inputUri, final Path outfile) throws TranscoderException, IOException {
		final ThrowingSupplier<OutputStream, IOException> osSupplier = createNewFileSupplier(outfile);
		write(inputUri, osSupplier);
	}

	/**
	 * @see <a href=
	 *      "http://stackoverflow.com/q/32721467/1391325">StackOverflow</a>
	 * @param inputUri
	 * @param outputStreamSupplier
	 * @throws TranscoderException
	 * @throws IOException
	 */
	public void write(final URI inputUri, final ThrowingSupplier<OutputStream, IOException> outputStreamSupplier)
			throws TranscoderException, IOException {
		write(SVGDocuments.read(inputUri), outputStreamSupplier);
	}

	private void writeSingleFile(final Path infile, final Path outfile) throws TranscoderException, IOException {
		final ThrowingSupplier<OutputStream, IOException> osSupplier = createNewFileSupplier(outfile);
		LOGGER.info("Will read from \"{}\" and write output to \"{}\".", infile, outfile);
		write(infile.toUri(), osSupplier);
	}

}
