package se.kth.speech.coin.tangrams.data;

import java.util.ArrayList;
import java.util.List;

public class Session {

	public final List<Round> rounds = new ArrayList<>();

	public final  String name;

	public Session(final String name) {
		this.name = name;
	}
	
	public Round getRound(int n) {
		for (Round r : rounds) {
			if (r.n == n)
				return r;
		}
		return null;
	}
	
}
