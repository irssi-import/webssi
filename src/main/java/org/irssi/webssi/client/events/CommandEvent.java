package org.irssi.webssi.client.events;

/**
 * Event signaling irssi starts or ends processing a command we sent it.
 */
public class CommandEvent extends JsonEvent {
	protected CommandEvent() {}
	
	/**
	 * ID of the command being processed, or -1 if no command is being processed.
	 */
	public final native int getCommandId() /*-{ return this.id ? this.id : -1 }-*/;
	
}
