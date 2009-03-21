package org.irssi.webssi.client.control;

import org.irssi.webssi.client.events.JsonEvent;

import com.google.gwt.core.client.JavaScriptObject;

/**
 * Commmand sent from client to irssi.
 */
abstract class Command {
	private final JavaScriptObject js;
	
	Command() {
		this(null);
	}
	
	Command(JavaScriptObject js) {
		this.js = js;
	}
	
	/**
	 * Returns the JavaScriptObject to send to irssi.
	 */
	JavaScriptObject getJS() {
		return js;
	}
	
	/**
	 * Applies this command in the model.
	 */
	abstract void execute();
	
	/**
	 * Called when an event is sent while irssi processes this command.
	 * Returns true if the event is an echo; if it the normal event handlers should not be called. 
	 */
	boolean echo(JsonEvent event) {
		return false;
	}
	
	/**
	 * Checked after processing of all events, when the given event is received, and was not predicted as echo,
	 * and this command is still pending.
	 * If this returns true, this command will be executed again.
	 */
	boolean needReplayAfter(JsonEvent event) {
		return false;
	}
	
	/**
	 * Checked after processing of all events, when the given command has been processed but produced no echo,
	 * and this command is still pending.
	 * If this returns true, this command will be executed again.
	 */
	boolean needReplayAfterMissingEcho(Command missingEchoCommand) {
		return false;
	}
}
