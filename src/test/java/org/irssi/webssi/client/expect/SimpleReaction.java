package org.irssi.webssi.client.expect;

/**
 * Reaction that doesn't care about the previous expected call.
 */
public abstract class SimpleReaction extends Reaction<Object> {
	@Override
	final public void run(Object param) {
		run();
	}
	
	
	abstract public void run();
}
