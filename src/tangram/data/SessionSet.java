package tangram.data;

import java.io.*;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Predicate;

public class SessionSet {
	
	public List<Session> sessions = new ArrayList<>(); 
	
	public SessionSet(File dir) throws IOException {
		for (File subdir : dir.listFiles()) {
			if (subdir.isDirectory() && new File(subdir, "events.tsv").exists() && new File(subdir, "extracted-referring-tokens.tsv").exists()) {
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
