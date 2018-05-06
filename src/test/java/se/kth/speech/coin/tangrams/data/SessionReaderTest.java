package se.kth.speech.coin.tangrams.data;

import org.hamcrest.CoreMatchers;
import org.hamcrest.collection.IsCollectionWithSize;
import org.hamcrest.collection.IsEmptyCollection;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class SessionReaderTest {

	private static Path sessionDataDir;

	@BeforeClass
	public static void readSessionData() throws IOException {
		final String archiveNameBase = "session-1";
		final Path tmpDir = Files.createTempDirectory(SessionReaderTest.class.getName());
		sessionDataDir = Files.createDirectory(tmpDir.resolve(archiveNameBase));
		try (InputStream is = SessionReaderTest.class.getResourceAsStream(archiveNameBase + ".zip")){
			extractArchive(is, sessionDataDir);
		}
	}

	@AfterClass
	public static void deleteUnpackedSessionData() throws IOException {
		final Iterable<Path> files = Files.list(sessionDataDir)::iterator;
		for (final Path file : files) {
			Files.deleteIfExists(file);
		}
		Files.deleteIfExists(sessionDataDir);
	}

	private static void extractArchive(final InputStream archiveStream, final Path outdir) throws IOException {
		final byte[] buffer = new byte[1024];
		try (ZipInputStream zis = new ZipInputStream(archiveStream)) {
			ZipEntry zipEntry = zis.getNextEntry();
			while (zipEntry != null) {
				final String fileName = zipEntry.getName();
				//final File newFile = new File(outdir, fileName);
				final Path newFile = outdir.resolve(fileName);
				try (OutputStream fos = Files.newOutputStream(newFile)) {
					int len;
					while ((len = zis.read(buffer)) > 0) {
						fos.write(buffer, 0, len);
					}
				}
				zipEntry = zis.getNextEntry();
			}
			zis.closeEntry();
		}
	}

	@Test
	public void testApply() throws IOException {
		// Just use the whole utterance text as "referring language" for testing purposes
		final Function<? super String[], String[]> referringTokenSeqFactory = Function.identity();
		final SessionReader reader = new SessionReader(referringTokenSeqFactory);
		final Session session = reader.apply(sessionDataDir.toFile());
		Assert.assertThat(session.rounds, IsCollectionWithSize.hasSize(37));
		final String[] expectedFirstUttTokens = "ah okay now I see a selected piece".split(" ");
		Assert.assertArrayEquals(expectedFirstUttTokens, session.rounds.get(0).utts.get(0).fullText);
		final String[] expectedLastUttTokens = "yeah".split(" ");

		final Optional<Round> optRound17 = session.rounds.stream().filter(round -> round.n == 17).findFirst();
		Assert.assertTrue(optRound17.isPresent());
		final Round round17 = optRound17.get();
		Assert.assertThat(round17.utts, IsCollectionWithSize.hasSize(6));
		final String[] expectedRound6FirstUttTokens = "all right so it's the s- uh Batman hat uh pink one".split(" ");
		Assert.assertArrayEquals(expectedRound6FirstUttTokens, round17.utts.get(0).fullText);

		final Round lastRound = session.rounds.get(session.rounds.size() - 1);
		Assert.assertThat(lastRound.utts, IsCollectionWithSize.hasSize(7));
		final Utterance lastUtt = lastRound.utts.get(lastRound.utts.size() - 1);
		Assert.assertArrayEquals(expectedLastUttTokens, lastUtt.fullText);

		for (final Round round : session.rounds) {
			Assert.assertThat(round.referents, CoreMatchers.not(IsEmptyCollection.empty()));
			final List<Utterance> utts = round.utts;
			Assert.assertThat(utts, CoreMatchers.not(IsEmptyCollection.empty()));
			Assert.assertTrue(utts.stream().noneMatch(utt -> utt.fullText.length < 1));
			for (Utterance utt : utts) {
				// In this test, the same exact tokens should be found for referring language
				Assert.assertArrayEquals(utt.fullText, utt.refText);
			}
			Assert.assertTrue(utts.stream().noneMatch(utt -> utt.getRound() < 1));
		}
	}
}
