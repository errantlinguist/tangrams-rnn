package se.kth.speech.coin.tangrams.data;

import java.util.*;

public class Referent {

	public static Set<String> shapes = new HashSet<>((int) Math.ceil(DatasetConstants.EXPECTED_UNIQUE_SHAPE_COUNT / 0.75f));
	public int round = 0;
	public String shape = "wedge";
	public float size = 0f;
	public float red = 0f;
	public float green = 0f;
	public float blue = 0f;
	public float hue = 0f;
	public float posx = 0f;
	public float posy = 0f;
	public float midx = 0f;
	public float midy = 0f;
	int id = 0;
	private boolean target = false;
	private int mentioned = 0;

	public Referent() {
	}

	public Referent(String[] cols) {
		this.id = Integer.parseInt(cols[6]);
		this.round = Integer.parseInt(cols[1]);
		this.target = Boolean.parseBoolean(cols[7]);
		this.shape = cols[9];
		shapes.add(shape);
		this.size = Float.parseFloat(cols[11]);
		this.red = Float.parseFloat(cols[12]) / 255f;
		this.green = Float.parseFloat(cols[13]) / 255f;
		this.blue = Float.parseFloat(cols[14]) / 255f;
		this.hue = Float.parseFloat(cols[16]);
		setPos(Float.parseFloat(cols[19]), Float.parseFloat(cols[20]));
	}

	public boolean isTarget() {
		return target;
	}

	public void setTarget(final boolean target) {
		this.target = target;
	}

	public int getMentioned() {
		return mentioned;
	}

	public void setMentioned(int mentioned) {
		this.mentioned = mentioned;
	}

	public void setPos(float x, float y) {
		this.posx = x;
		this.posy = y;
		this.midx = 1f - (Math.abs(0.5f - posx) * 2f);
		this.midy = 1f - (Math.abs(0.5f - posy) * 2f);
	}

	@Override
	public String toString() {
		return String.format("%s R:%.2f G:%.2f B:%.2f size:%.2f", shape, red, green, blue, size) + " " + (target ? "*" : "");
	}

}
