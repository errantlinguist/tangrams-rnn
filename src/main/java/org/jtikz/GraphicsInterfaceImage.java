package org.jtikz;

import java.awt.Graphics;
import java.awt.Image;
import java.awt.image.ImageObserver;
import java.awt.image.ImageProducer;

final class GraphicsInterfaceImage extends Image {

	private final AbstractGraphicsInterface g;

	GraphicsInterfaceImage(final AbstractGraphicsInterface g) {
		this.g = g;
	}

	@Override
	public void flush() {
		g.flush();
	}

	@Override
	public Graphics getGraphics() {
		return g.create();
	}

	@Override
	public int getHeight(final ImageObserver io) {
		return Integer.MAX_VALUE;
	}

	@Override
	public Object getProperty(final String name, final ImageObserver observer) {
		return null;
	}

	@Override
	public ImageProducer getSource() {
		return null;
	}

	@Override
	public int getWidth(final ImageObserver io) {
		return Integer.MAX_VALUE;
	}
}