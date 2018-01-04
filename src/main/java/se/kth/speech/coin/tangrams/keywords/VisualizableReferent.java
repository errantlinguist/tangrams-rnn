package se.kth.speech.coin.tangrams.keywords;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import se.kth.speech.coin.tangrams.wac.data.Referent;

final class VisualizableReferent {

	private static final Map<Referent, VisualizableReferent> INSTANCES = new ConcurrentHashMap<>();

	static VisualizableReferent fetch(final Referent ref) {
		return INSTANCES.computeIfAbsent(ref, VisualizableReferent::new);
	}

	private final int blue;

	private final int green;

	private final float hue;

	private final int red;

	private final String shape;

	private VisualizableReferent(final Referent ref) {
		blue = ref.getBlueInt();
		green = ref.getGreenInt();
		hue = ref.getHue();
		red = ref.getRedInt();
		shape = ref.getShape();
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(final Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (!(obj instanceof VisualizableReferent)) {
			return false;
		}
		final VisualizableReferent other = (VisualizableReferent) obj;
		if (Float.floatToIntBits(blue) != Float.floatToIntBits(other.blue)) {
			return false;
		}
		if (Float.floatToIntBits(green) != Float.floatToIntBits(other.green)) {
			return false;
		}
		if (Float.floatToIntBits(hue) != Float.floatToIntBits(other.hue)) {
			return false;
		}
		if (Float.floatToIntBits(red) != Float.floatToIntBits(other.red)) {
			return false;
		}
		if (shape == null) {
			if (other.shape != null) {
				return false;
			}
		} else if (!shape.equals(other.shape)) {
			return false;
		}
		return true;
	}

	/**
	 * @return the blue
	 */
	public int getBlue() {
		return blue;
	}

	/**
	 * @return the green
	 */
	public int getGreen() {
		return green;
	}

	/**
	 * @return the red
	 */
	public int getRed() {
		return red;
	}

	/**
	 * @return the shape
	 */
	public String getShape() {
		return shape;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + Float.floatToIntBits(blue);
		result = prime * result + Float.floatToIntBits(green);
		result = prime * result + Float.floatToIntBits(hue);
		result = prime * result + Float.floatToIntBits(red);
		result = prime * result + (shape == null ? 0 : shape.hashCode());
		return result;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		final StringBuilder builder = new StringBuilder(256);
		builder.append("Referent [edgeCount=");
		builder.append(", blue=");
		builder.append(blue);
		builder.append(", green=");
		builder.append(green);
		builder.append(", hue=");
		builder.append(hue);
		builder.append(", red=");
		builder.append(red);
		builder.append(", shape=");
		builder.append(shape);
		builder.append("]");
		return builder.toString();
	}

}