package org.irssi.webssi.client.events;

/**
 * {@link JsonEvent} for activity in a window item.
 */
public class WindowItemActivityEvent extends WindowItemEvent {
	protected WindowItemActivityEvent() {}
	public final native int getDataLevel() /*-{ return this.data_level; }-*/;
	public final native String getHilightColor() /*-{ return this.hilight_color; }-*/;
}
