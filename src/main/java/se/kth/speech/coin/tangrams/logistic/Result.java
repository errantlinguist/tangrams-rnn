package se.kth.speech.coin.tangrams.logistic;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.apache.commons.math3.stat.descriptive.moment.Mean;

import se.kth.speech.coin.tangrams.data.Referent;

public class Result {

	private Mean rank = new Mean();
	private Mean mrr = new Mean();
	private Mean acc = new Mean();

	public Result() {
	}
	
	public void increment(Map<Referent, Double> scores) {
		List<Referent> ranking = new ArrayList<>(scores.keySet());
		ranking.sort(new Comparator<Referent>() {
			@Override
			public int compare(Referent o1, Referent o2) {
				return scores.get(o2).compareTo(scores.get(o1));
			}
		});
		int rank = 0;
		for (Referent ref : ranking) {
			rank++;
			if (ref.isTarget())
				break;
		}
		increment(rank, rank == 1 ? 1d : 0d, 1d/rank);
	}

	@Override
	public String toString() {
		return String.format(Locale.US, "%.3f\t%.3f\t%.3f", rank.getResult(), mrr.getResult(), acc.getResult());
	}

	public void increment(double rank, double acc, double mrr) {
		this.rank.increment(rank);
		this.acc.increment(acc);
		this.mrr.increment(mrr);
	}
	
	public void increment(Result result) {
		this.rank.increment(result.rank.getResult());
		this.acc.increment(result.acc.getResult());
		this.mrr.increment(result.mrr.getResult());
	}

}
