package org.irssi.webssi.client.command;

import org.irssi.webssi.client.events.JsonEvent;

import com.google.gwt.core.client.JavaScriptObject;

/**
 * Commmand sent from client to irssi.
 */
public abstract class Command {
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
	public JavaScriptObject getJS() {
		return js;
	}
	
	/**
	 * Applies this command in the model.
	 */
	public abstract void execute();
	
	/**
	 * Called when an event is sent while irssi processes this command.
	 * Returns true if the event is an echo; if it the normal event handlers should not be called. 
	 */
	public boolean echo(JsonEvent event) {
		return false;
	}
	
	/**
	 * Checked after processing of all events, when the given event is received, and was not predicted as echo,
	 * and this command is still pending.
	 * If this returns true, this command will be executed again.
	 */
	public boolean needReplayAfter(JsonEvent event) {
		return false;
	}
	
	/**
	 * Checked after processing of all events, when the given command has been processed but produced no echo,
	 * and this command is still pending.
	 * If this returns true, this command will be executed again.
	 */
	public boolean needReplayAfterMissingEcho(Command missingEchoCommand) {
		return false;
	}
}
