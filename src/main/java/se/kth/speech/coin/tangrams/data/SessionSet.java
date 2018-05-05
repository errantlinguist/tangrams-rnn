package se.kth.speech.coin.tangrams.data;

import java.io.*;
import java.nio.file.Files;
import java.util.*;
import java.util.function.BiConsumer;

public class SessionSet {
	
	public List<Session> sessions = new ArrayList<>(); 
	
	public SessionSet(File dir) throws IOException {
		if (dir.isDirectory()) {
			if (new File(dir, "events.tsv").exists()) {
				sessions.add(new Session(dir));
			} else {
				for (File subdir : dir.listFiles()) {
					if (subdir.isDirectory() && new File(subdir, "events.tsv").exists() && new File(subdir, "extracted-referring-tokens.tsv").exists()) {
						//System.out.println(subdir);
						Session session = new Session(subdir);
						sessions.add(session);
					}
				} 
			}
		} else {
			for (String f : Files.readAllLines(dir.toPath())) {
				f = f.trim();
				if (f.length() > 0) {
				Session session = new Session(new File(dir.getParentFile(), f));
				sessions.add(session);
				}
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


	public int size() {
		return sessions.size();
	}

	
	
}
