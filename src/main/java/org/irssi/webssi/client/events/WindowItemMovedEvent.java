package org.irssi.webssi.client.events;

public class WindowItemMovedEvent extends WindowItemEvent {
	protected WindowItemMovedEvent() {}
	public final native WindowEvent getNewWindowEvent() /*-{ return this.new_window_event; }-*/;
}
