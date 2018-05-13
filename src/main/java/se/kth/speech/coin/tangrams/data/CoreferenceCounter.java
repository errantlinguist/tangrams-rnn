/**
 * 
 */
package se.kth.speech.coin.tangrams.data;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author tshore
 *
 */
public final class CoreferenceCounter {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(CoreferenceCounter.class);

	public static void main(final String[] args) throws IOException {
		if (args.length != 2) {
			throw new IllegalArgumentException(String.format("Usage: %s <dataDir> <outfile>",
					CoreferenceCounter.class.getName()));
		}

		final SessionReader sessionReader = new SessionReader(Function.identity());
		final File dataDir = new File(args[0]);
		LOGGER.info("Reading session data underneath \"{}\".", dataDir);
		final Path outfile = Paths.get(args[1]);
		LOGGER.info("Will write results to \"{}\".", outfile);
		if (Files.exists(outfile)) {
			throw new IllegalArgumentException(String.format("Output file \"%s\" already exists.", outfile));
		} else {
			SessionSet sessions = new SessionSet(dataDir, sessionReader);
			Files.createDirectories(outfile.getParent());
			try(CSVPrinter printer = CSVFormat.TDF.withHeader("SESSION", "ROUND", "REF_ID", "COREF_COUNT").print(outfile, StandardCharsets.UTF_8)) {
				for (Session session : sessions.sessions) {
					final List<Round> rounds = session.rounds;
					final Map<Integer, Integer> refCounts = new HashMap<>();
					for (Round round : rounds) {
						final Integer refId = round.target.id;
						final Integer corefCount = refCounts.getOrDefault(refId, 0) + 1;
						printer.printRecord(session.name, round.n, refId, corefCount);
						refCounts.put(refId, corefCount);
					}
				}	
			}
		}
	}

}
