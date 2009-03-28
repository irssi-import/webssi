package org.irssi.webssi.client.events;

/**
 * {@link JsonEvent} for activity in a window.
 */
public class WindowActivityEvent extends WindowEvent {
	protected WindowActivityEvent() {}
	public final native int getDataLevel() /*-{ return this.data_level; }-*/;
	public final native String getHilightColor() /*-{ return this.hilight_color; }-*/;
}
