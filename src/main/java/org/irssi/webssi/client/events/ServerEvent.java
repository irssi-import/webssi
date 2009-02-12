package org.irssi.webssi.client.events;

/**
 * {@link JsonEvent} related to a server.
 */
public class ServerEvent extends JsonEvent {
	protected ServerEvent() {}
	
	public final native String getTag() /*-{ return this.tag; }-*/;
}
