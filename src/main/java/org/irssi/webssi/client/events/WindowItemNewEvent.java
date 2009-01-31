package org.irssi.webssi.client.events;

/**
 * {@link JsonEvent} for a window item being created or moved to another window.
 */
public class WindowItemNewEvent extends WindowItemEvent {
	protected WindowItemNewEvent() {}
	public final native String getVisibleName() /*-{ return this.visible_name; }-*/;
	public final native boolean isActive() /*-{ return this.is_active != 0; }-*/;
	public final native String getItemType() /*-{ return this.item_type; }-*/;
}
