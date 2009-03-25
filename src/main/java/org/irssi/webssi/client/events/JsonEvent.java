package org.irssi.webssi.client.events;

import com.google.gwt.core.client.JavaScriptObject;

/**
 * Event sent by irssi.
 */
public class JsonEvent extends JavaScriptObject {
	protected JsonEvent() {}
	
	/**
	 * Name of the event. For example "window new".
	 */
	public final native String getType() /*-{ return this.type; }-*/;
	
	/**
	 * Id for this event. Event ids are ascending in the order that they are sent.
	 */
	public final native int getEventId() /*-{ return this.i; }-*/;
}
