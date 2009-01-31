package org.irssi.webssi.client.events;

/**
 * {@link JsonEvent} related to a window.
 */
public class WindowEvent extends JsonEvent {
	protected WindowEvent() {}
	public final native String getWinId() /*-{ return this.window; }-*/;
}
