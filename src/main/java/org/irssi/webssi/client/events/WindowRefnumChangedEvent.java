package org.irssi.webssi.client.events;

/**
 * {@link JsonEvent} for the refnum of a window changing.
 */
public class WindowRefnumChangedEvent extends WindowEvent {
	protected WindowRefnumChangedEvent() {}
	
	/**
	 * The new refnum.
	 */
	public final native int getRefnum() /*-{ return this.refnum; }-*/;
}
