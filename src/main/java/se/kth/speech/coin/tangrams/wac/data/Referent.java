/*
 * 	Copyright 2017 Todd Shore
 *
 *	Licensed under the Apache License, Version 2.0 (the "License");
 *	you may not use this file except in compliance with the License.
 *	You may obtain a copy of the License at
 *
 *		http://www.apache.org/licenses/LICENSE-2.0
 *
 *	Unless required by applicable law or agreed to in writing, software
 *	distributed under the License is distributed on an "AS IS" BASIS,
 *	WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *	See the License for the specific language governing permissions and
 *	limitations under the License.
 */
package se.kth.speech.coin.tangrams.wac.data;

import java.util.NavigableSet;
import java.util.TreeSet;

public final class Referent {

	/**
	 * A {@link NavigableSet} of all {@link Referent#edgeCount edge count}
	 * values assigned to all {@link Referent} instances.
	 */
	private static final NavigableSet<Integer> EDGE_COUNTS = new TreeSet<>();

	/**
	 * A {@link NavigableSet} of all {@link Referent#shape shape} values
	 * assigned to all {@link Referent} instances.
	 */
	private static final NavigableSet<String> SHAPES = new TreeSet<>();

	/**
	 * @return A {@link NavigableSet} of all {@link Referent#edgeCount edge
	 *         count} values assigned to all {@link Referent} instances.
	 */
	public static NavigableSet<Integer> getEdgeCounts() {
		return EDGE_COUNTS;
	}

	/**
	 * @return A {@link NavigableSet} of all {@link Referent#shape shape} values
	 *         assigned to all {@link Referent} instances.
	 */
	public static NavigableSet<String> getShapes() {
		return SHAPES;
	}

	private static int delinearizeColorByteValue(final float value) {
		return Math.round(value * 255f);
	}

	/**
	 * Calculates the distance from the extrema for a value in the range of
	 * <code>0.0</code> to <code>1.0</code>.
	 *
	 * @param value
	 *            A value between <code>0.0</code> and <code>1.0</code>
	 *            (inclusive).
	 * @return The distance from the extrema (i.e.&nbsp;<code>0.5</code>).
	 */
	private static double distanceFromExtrema(final double value) {
		assert value >= 0.0;
		assert value <= 1.0;
		final double result = 1.0 - Math.abs(0.5 - value) * 2.0;
		assert result >= 0.0;
		assert result <= 1.0;
		return result;
	}

	private static float linearizeColorByteValue(final int value) {
		return value / 255f;
	}

	private int blueInt = 0;

	private float blueLinear = 0f;

	private int edgeCount = 0;

	private int greenInt = 0;

	private float greenLinear = 0f;

	private float hue = 0f;

	private double midX = 0.0;

	private double midY = 0.0;

	private double positionX = 0.0;

	private double positionY = 0.0;

	private int redInt = 0;

	private float redLinear = 0f;

	private String shape = "wedge";

	private double size = 0.0;

	private boolean target = false;

	public Referent() {
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
		if (!(obj instanceof Referent)) {
			return false;
		}
		final Referent other = (Referent) obj;
		if (Float.floatToIntBits(blueLinear) != Float.floatToIntBits(other.blueLinear)) {
			return false;
		}
		if (edgeCount != other.edgeCount) {
			return false;
		}
		if (Float.floatToIntBits(greenLinear) != Float.floatToIntBits(other.greenLinear)) {
			return false;
		}
		if (Float.floatToIntBits(hue) != Float.floatToIntBits(other.hue)) {
			return false;
		}
		if (Double.doubleToLongBits(midX) != Double.doubleToLongBits(other.midX)) {
			return false;
		}
		if (Double.doubleToLongBits(midY) != Double.doubleToLongBits(other.midY)) {
			return false;
		}
		if (Double.doubleToLongBits(positionX) != Double.doubleToLongBits(other.positionX)) {
			return false;
		}
		if (Double.doubleToLongBits(positionY) != Double.doubleToLongBits(other.positionY)) {
			return false;
		}
		if (Float.floatToIntBits(redLinear) != Float.floatToIntBits(other.redLinear)) {
			return false;
		}
		if (shape == null) {
			if (other.shape != null) {
				return false;
			}
		} else if (!shape.equals(other.shape)) {
			return false;
		}
		if (Double.doubleToLongBits(size) != Double.doubleToLongBits(other.size)) {
			return false;
		}
		if (target != other.target) {
			return false;
		}
		return true;
	}

	/**
	 * @return the blueInt
	 */
	public int getBlueInt() {
		return blueInt;
	}

	/**
	 * @return the blueLinear
	 */
	public float getBlueLinear() {
		return blueLinear;
	}

	/**
	 * @return the edgeCount
	 */
	public int getEdgeCount() {
		return edgeCount;
	}

	/**
	 * @return the greenInt
	 */
	public int getGreenInt() {
		return greenInt;
	}

	/**
	 * @return the greenLinear
	 */
	public float getGreenLinear() {
		return greenLinear;
	}

	/**
	 * @return the hue
	 */
	public float getHue() {
		return hue;
	}

	/**
	 * @return the midX
	 */
	public double getMidX() {
		return midX;
	}

	/**
	 * @return the midY
	 */
	public double getMidY() {
		return midY;
	}

	/**
	 * @return the positionX
	 */
	public double getPositionX() {
		return positionX;
	}

	/**
	 * @return the positionY
	 */
	public double getPositionY() {
		return positionY;
	}

	/**
	 * @return the redInt
	 */
	public int getRedInt() {
		return redInt;
	}

	/**
	 * @return the redLinear
	 */
	public float getRedLinear() {
		return redLinear;
	}

	/**
	 * @return the shape
	 */
	public String getShape() {
		return shape;
	}

	/**
	 * @return the size
	 */
	public double getSize() {
		return size;
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
		result = prime * result + Float.floatToIntBits(blueLinear);
		result = prime * result + edgeCount;
		result = prime * result + Float.floatToIntBits(greenLinear);
		result = prime * result + Float.floatToIntBits(hue);
		long temp;
		temp = Double.doubleToLongBits(midX);
		result = prime * result + (int) (temp ^ temp >>> 32);
		temp = Double.doubleToLongBits(midY);
		result = prime * result + (int) (temp ^ temp >>> 32);
		temp = Double.doubleToLongBits(positionX);
		result = prime * result + (int) (temp ^ temp >>> 32);
		temp = Double.doubleToLongBits(positionY);
		result = prime * result + (int) (temp ^ temp >>> 32);
		result = prime * result + Float.floatToIntBits(redLinear);
		result = prime * result + (shape == null ? 0 : shape.hashCode());
		temp = Double.doubleToLongBits(size);
		result = prime * result + (int) (temp ^ temp >>> 32);
		result = prime * result + (target ? 1231 : 1237);
		return result;
	}

	/**
	 * @return the target
	 */
	public boolean isTarget() {
		return target;
	}

	/**
	 * @param blueLinear
	 *            the blueLinear to set
	 */
	public void setBlue(final float blue) {
		blueLinear = blue;
		blueInt = delinearizeColorByteValue(blue);
	}

	/**
	 * @param blueLinear
	 *            the blueLinear to set
	 */
	public void setBlue(final int blue) {
		blueInt = blue;
		blueLinear = linearizeColorByteValue(blue);
	}

	/**
	 * @param edgeCount
	 *            the edgeCount to set
	 */
	public void setEdgeCount(final int edgeCount) {
		this.edgeCount = edgeCount;
		EDGE_COUNTS.add(edgeCount);
	}

	/**
	 * @param greenLinear
	 *            the greenLinear to set
	 */
	public void setGreen(final float green) {
		greenLinear = green;
		greenInt = delinearizeColorByteValue(green);
	}

	/**
	 * @param greenLinear
	 *            the greenLinear to set
	 */
	public void setGreen(final int green) {
		greenInt = green;
		greenLinear = linearizeColorByteValue(green);
	}

	/**
	 * @param hue
	 *            the hue to set
	 */
	public void setHue(final float hue) {
		this.hue = hue;
	}

	public void setPosition(final double x, final double y) {
		positionX = x;
		positionY = y;
		midX = distanceFromExtrema(positionX);
		midY = distanceFromExtrema(positionY);
	}

	/**
	 * @param redLinear
	 *            the redLinear to set
	 */
	public void setRed(final float red) {
		redLinear = red;
		redInt = delinearizeColorByteValue(red);
	}

	/**
	 * @param redLinear
	 *            the redLinear to set
	 */
	public void setRed(final int red) {
		redInt = red;
		redLinear = linearizeColorByteValue(red);
	}

	/**
	 * @param shape
	 *            the shape to set
	 */
	public void setShape(final String shape) {
		this.shape = shape;
		SHAPES.add(shape);
	}

	/**
	 * @param size
	 *            the size to set
	 */
	public void setSize(final double size) {
		this.size = size;
	}

	/**
	 * @param target
	 *            the target to set
	 */
	public void setTarget(final boolean target) {
		this.target = target;
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
		builder.append(edgeCount);
		builder.append(", blueLinear=");
		builder.append(blueLinear);
		builder.append(", greenLinear=");
		builder.append(greenLinear);
		builder.append(", hue=");
		builder.append(hue);
		builder.append(", midX=");
		builder.append(midX);
		builder.append(", midY=");
		builder.append(midY);
		builder.append(", positionX=");
		builder.append(positionX);
		builder.append(", positionY=");
		builder.append(positionY);
		builder.append(", redLinear=");
		builder.append(redLinear);
		builder.append(", shape=");
		builder.append(shape);
		builder.append(", size=");
		builder.append(size);
		builder.append(", target=");
		builder.append(target);
		builder.append("]");
		return builder.toString();
	}

}
