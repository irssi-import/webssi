package org.irssi.webssi.client.events;

/**
 * {@link NicklistEvent} sent when someone changes his nick.
 */
public class NicklistChangedEvent extends NicklistEvent {
	protected NicklistChangedEvent() {}
	public final native String getNewName() /*-{ return this.new_name; }-*/;
}
