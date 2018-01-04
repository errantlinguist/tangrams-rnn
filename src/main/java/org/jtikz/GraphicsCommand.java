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

	public GraphicsCommand(final Object command, final AbstractGraphicsInterface creator) {
		this.command = command;
		clip = findClip(creator);
	}

	public Shape getClip() {
		return clip;
	}

	public Object getCommand() {
		return command;
	}
}
