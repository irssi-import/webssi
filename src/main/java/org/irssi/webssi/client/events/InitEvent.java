package org.irssi.webssi.client.events;

/**
 * Event send by irssi when we just started a new session.
 */
public class InitEvent extends JsonEvent {
	protected InitEvent() {}
	public final native String getSessionId() /*-{ return this.sessionid; }-*/;
}
