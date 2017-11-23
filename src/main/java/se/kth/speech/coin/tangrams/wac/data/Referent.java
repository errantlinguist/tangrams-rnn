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

import java.util.HashSet;
import java.util.Set;

public final class Referent {

	/**
	 * A {@link Set} of all {@link Referent#shape shape} values assigned to all
	 * {@link Referent} instances.
	 */
	private static final Set<String> SHAPES = new HashSet<>(18, 1.0f);

	/**
	 * @return A {@link Set} of all {@link Referent#shape shape} values assigned
	 *         to all {@link Referent} instances.
	 */
	public static Set<String> getShapes() {
		return SHAPES;
	}

	private float blue = 0f;

	private float green = 0f;

	private float hue = 0f;

	private float midX = 0f;

	private float midY = 0f;

	private float positionX = 0f;

	private float positionY = 0f;

	private float red = 0f;

	private int round = 0;

	private String shape = "wedge";

	private float size = 0f;

	private boolean target = false;

	public Referent() {
	}

	public Referent(final String[] cols) {
		round = Integer.parseInt(cols[1]);
		target = Boolean.parseBoolean(cols[7]);
		shape = cols[9];
		SHAPES.add(shape);
		size = Float.parseFloat(cols[11]);
		red = Float.parseFloat(cols[12]) / 255f;
		green = Float.parseFloat(cols[13]) / 255f;
		blue = Float.parseFloat(cols[14]) / 255f;
		hue = Float.parseFloat(cols[16]);
		setPosition(Float.parseFloat(cols[19]), Float.parseFloat(cols[20]));
	}

	/**
	 * @return the blue
	 */
	public float getBlue() {
		return blue;
	}

	/**
	 * @return the green
	 */
	public float getGreen() {
		return green;
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
	public float getMidX() {
		return midX;
	}

	/**
	 * @return the midY
	 */
	public float getMidY() {
		return midY;
	}

	/**
	 * @return the positionX
	 */
	public float getPositionX() {
		return positionX;
	}

	/**
	 * @return the positionY
	 */
	public float getPositionY() {
		return positionY;
	}

	/**
	 * @return the red
	 */
	public float getRed() {
		return red;
	}

	/**
	 * @return the round
	 */
	public int getRound() {
		return round;
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
	public float getSize() {
		return size;
	}

	/**
	 * @return the target
	 */
	public boolean isTarget() {
		return target;
	}

	/**
	 * @param blue
	 *            the blue to set
	 */
	public void setBlue(final float blue) {
		this.blue = blue;
	}

	/**
	 * @param green
	 *            the green to set
	 */
	public void setGreen(final float green) {
		this.green = green;
	}

	/**
	 * @param hue
	 *            the hue to set
	 */
	public void setHue(final float hue) {
		this.hue = hue;
	}

	public void setPosition(final float x, final float y) {
		positionX = x;
		positionY = y;
		midX = 1f - Math.abs(0.5f - positionX) * 2f;
		midY = 1f - Math.abs(0.5f - positionY) * 2f;
	}

	/**
	 * @param red
	 *            the red to set
	 */
	public void setRed(final float red) {
		this.red = red;
	}

	/**
	 * @param round
	 *            the round to set
	 */
	public void setRound(final int round) {
		this.round = round;
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
	public void setSize(final float size) {
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
		builder.append("Referent [round=");
		builder.append(round);
		builder.append(", target=");
		builder.append(target);
		builder.append(", shape=");
		builder.append(shape);
		builder.append(", size=");
		builder.append(size);
		builder.append(", red=");
		builder.append(red);
		builder.append(", green=");
		builder.append(green);
		builder.append(", blue=");
		builder.append(blue);
		builder.append(", hue=");
		builder.append(hue);
		builder.append(", positionX=");
		builder.append(positionX);
		builder.append(", positionY=");
		builder.append(positionY);
		builder.append(", midX=");
		builder.append(midX);
		builder.append(", midY=");
		builder.append(midY);
		builder.append("]");
		return builder.toString();
	}

}
