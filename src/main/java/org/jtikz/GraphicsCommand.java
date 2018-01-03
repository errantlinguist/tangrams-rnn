package org.jtikz;

import java.awt.Shape;

final class GraphicsCommand {

	private static Shape findClip(AbstractGraphicsInterface creator) {
		Shape result = creator.getClip();
		while (result == null && creator.parent != null) {
			creator = creator.parent;
			result = creator.getClip();
		}
		return result;
	}

	private final Shape clip;

	private final Object command;

	GraphicsCommand(final Object command, final AbstractGraphicsInterface creator) {
		this.command = command;
		clip = findClip(creator);
	}

	public Shape getClip() {
		return clip;
	}

	public Object getCommand() {
		return command;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		final StringBuilder builder = new StringBuilder(128);
		builder.append("GraphicsCommand [clip=");
		builder.append(clip);
		builder.append(", command=");
		builder.append(command);
		builder.append("]");
		return builder.toString();
	}
}
