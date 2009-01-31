package org.irssi.webssi.client.events;

/**
 * {@link JsonEvent} for the name of a window changing.
 */
public class WindowNameChangedEvent extends WindowEvent {
	protected WindowNameChangedEvent() {}
	
	/**
	 * The new name for the window.
	 */
	public final native String getName() /*-{ return this.name; }-*/;
	
}
