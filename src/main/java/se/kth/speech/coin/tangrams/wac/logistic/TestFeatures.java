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
package se.kth.speech.coin.tangrams.wac.logistic;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import se.kth.speech.coin.tangrams.wac.data.SessionSet;
import se.kth.speech.coin.tangrams.wac.data.SessionSetReader;

/**
 * @author <a href="mailto:tcshore@kth.se">Todd Shore</a>
 * @since Nov 24, 2017
 *
 */
public final class TestFeatures {

	private static final Logger LOGGER = LoggerFactory.getLogger(TestFeatures.class);

	public static void main(final String[] args) throws IOException, ClassificationException {
		if (args.length != 2) {
			throw new IllegalArgumentException(String.format("Usage: %s INPATH OUTPATH", TestFeatures.class.getName()));
		} else {
			final Path inpath = Paths.get(args[0]);
			final Path outpath = Paths.get(args[1]);
			LOGGER.info("Will read sessions from \"{}\"; Will write output to \"{}\".", inpath, outpath);
			run(inpath, outpath);
		}
	}

	public static void write(final LogisticModel model, final Path outpath)
			throws IOException, ClassificationException {
		TestColor.write(model, outpath);
		TestSize.write(model, outpath);
		TestSpace.write(model, outpath);
	}

	private static void run(final Path inpath, final Path outpath) throws IOException, ClassificationException {
		final SessionSet set = new SessionSetReader().apply(inpath);
		LOGGER.info("Read {} session(s).", set.size());
		final LogisticModel model = new LogisticModel();
		model.train(set);
		write(model, outpath);
	}

}
