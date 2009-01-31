package org.irssi.webssi.client.events;

/**
 * {@link JsonEvent} for text being printed into a window.
 */
public class TextEvent extends WindowEvent {
	protected TextEvent() {
	}

	/**
	 * Text being printed, as html.
	 */
	public final native String getText() /*-{ return this.text; }-*/;
}
