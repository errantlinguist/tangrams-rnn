package se.kth.speech.coin.tangrams.data;

import java.io.*;
import java.nio.file.Files;
import java.util.*;
import java.util.function.BiConsumer;

public class SessionSet {

	private static boolean isSessionDir(final File dir) {
		return new File(dir, "events.tsv").exists();
	}
	
	public List<Session> sessions = new ArrayList<>();
	
	public SessionSet(File dir, SessionReader sessionReader) throws IOException {
		if (dir.isDirectory()) {
			if (isSessionDir(dir)) {
				sessions.add(sessionReader.apply(dir));
			} else {
				for (File subdir : dir.listFiles()) {
					if (subdir.isDirectory() && isSessionDir(subdir)) {
						//System.out.println(subdir);
						Session session = sessionReader.apply(subdir);
						sessions.add(session);
					}
				} 
			}
		} else {
			final List<String> filePaths = Files.readAllLines(dir.toPath());
			for (String f :filePaths) {
				f = f.trim();
				if (f.length() > 0) {
					Session session = sessionReader.apply(new File(f));
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
