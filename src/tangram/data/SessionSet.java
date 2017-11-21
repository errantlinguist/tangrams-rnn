package tangram.data;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.function.BiConsumer;


public class SessionSet {
	
	public List<Session> sessions = new ArrayList<>(); 
	
	public SessionSet(Path dir) throws IOException {
		for (final Iterator<Path> subdirIter = Files.list(dir).filter(Files::isDirectory).iterator(); subdirIter.hasNext();) {
			final Path subdir = subdirIter.next();
			if (Files.isRegularFile(subdir.resolve("events.tsv")) && Files.isRegularFile(subdir.resolve("extracted-referring-tokens.tsv"))) {
				//System.out.println(subdir);
				Session session = new Session(subdir);
				sessions.add(session);
			}
		}
	}
	
	public SessionSet(SessionSet toCopy) {
		this.sessions = new ArrayList<>(toCopy.sessions);
	}
	
	public SessionSet(Session session) {
		sessions.add(session);
	}


	public void crossValidate(BiConsumer<SessionSet,Session> consumer) {
		for (int i = 0; i < sessions.size(); i++) {
			SessionSet training = new SessionSet(this);
			Session testing = training.sessions.remove(i);
			//System.out.println("Testing on " + testing.sessions.get(0).name);
			consumer.accept(training,testing);
		}
	}

	public void printNegatives() {
		for (Session sess : sessions) {
			for (Round round : sess.rounds) {
				if (round.isNegative())
					System.out.println(round.prettyDialog());
			}
		}
	}

	public int size() {
		return sessions.size();
	}

	
	
}
