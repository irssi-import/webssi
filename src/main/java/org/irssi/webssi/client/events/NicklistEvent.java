package org.irssi.webssi.client.events;

/**
 * {@link JsonEvent} related to a nicklist entry.
 */
public class NicklistEvent extends WindowItemEvent {
	protected NicklistEvent() {}
	public final native String getName() /*-{ return this.name; }-*/;
}
