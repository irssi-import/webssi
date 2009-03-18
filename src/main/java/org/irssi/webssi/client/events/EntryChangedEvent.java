package org.irssi.webssi.client.events;

/**
 * {@link JsonEvent} sent when the content of the entry, or the position of the cursor changes.
 */
public class EntryChangedEvent extends JsonEvent {
	protected EntryChangedEvent() {}
	public final native String getContent() /*-{ return this.content; }-*/;
	public final native int getCursorPos() /*-{ return this.cursorPos; }-*/;
}
