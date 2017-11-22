package tangram.logistic;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import tangram.data.Round;
import tangram.data.RoundSet;
import tangram.data.SessionSet;
import tangram.data.Utterance;

public class TestDialog {

	public static void main(String[] args) throws Exception {
		final Path inpath = Paths.get(args[0]);
		final Path outpath = Paths.get(args[1]);
		System.err.println(String.format("Reading sessions from \"%s\"; Will write output to \"%s\".", inpath, outpath));
		SessionSet set = new SessionSet(inpath);
		set.crossValidate((training,testing) -> {
			try (PrintWriter pw = new PrintWriter(Files.newBufferedWriter(outpath.resolve(testing.name + ".html")))) {
				pw.println("<table>");
				LogisticModel model = new LogisticModel();
				model.train(training);
				for (Round round : new RoundSet(set).rounds) {
					for (Utterance utt : round.utts) {
						for (String word : utt.fullText) {
							
						}
					}
					//new File("turn-0").list;
					pw.println("<img src=\"" + testing.name + "/screenshots/\"></td></tr>");
				}
				pw.println("</table>");
			} catch (IOException e) {
				throw new UncheckedIOException(e);
			}
			
		});
	}
	
}
