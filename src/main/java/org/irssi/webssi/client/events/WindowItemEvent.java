package org.irssi.webssi.client.events;

/**
 * {@link JsonEvent} related to a window item.
 */
public class WindowItemEvent extends WindowEvent {
	protected WindowItemEvent() {}
	public final native String getTag() /*-{ return this.tag; }-*/;
	public final native String getItemId() /*-{ return this.item; }-*/;
}
