package org.irssi.webssi.client.events;

/**
 * {@link JsonEvent} for creating a new window.
 */
public class WindowCreatedEvent extends WindowEvent {
	protected WindowCreatedEvent(){}
	public final native int getRefnum() /*-{ return this.refnum; }-*/;
	public final native String getName() /*-{ return this.name; }-*/;
	public final native int getDataLevel() /*-{ return this.data_level; }-*/;
	public final native String getHilightColor() /*-{ return this.hilight_color; }-*/;
}
