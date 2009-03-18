package org.irssi.webssi.client.model;

/**
 * The text being typed by the user.
 */
public class Entry {
	private Entry.Listener listener;
	private String content = "";
	private int cursorPos;
	
	public interface Listener {
		void contentChanged(String content, int cursorPos);
	}
	
	private static class CompositeListener implements Listener {
		private final Listener listener1;
		private final Listener listener2;

		private CompositeListener(Listener listener1, Listener listener2) {
			this.listener1 = listener1;
			this.listener2 = listener2;
		}
		
		public void contentChanged(String content, int cursorPos) {
			listener1.contentChanged(content, cursorPos);
			listener2.contentChanged(content, cursorPos);
		}
	}
	
	public void addListener(Listener listener) {
		if (this.listener == null)
			this.listener = listener;
		else
			this.listener = new CompositeListener(this.listener, listener);
	}	
	
	/**
	 * Returns the text content of the entry.
	 */
	public String getContent() {
		return content;
	}

	/**
	 * Sets the content and cursor position.
	 */
	public void setContent(String content, int cursorPos) {
		if (this.content != content || this.cursorPos != cursorPos) {
			this.content = content;
			this.cursorPos = cursorPos;
			if (listener != null)
				listener.contentChanged(content, cursorPos);
		}
	}
	
	/**
	 * Returns the position of the cursor in the entry.
	 */
	public int getCursorPos() {
		return cursorPos;
	}
	
	/**
	 * Sets the position of the cursor.
	 */
	public void setCursorPos(int cursorPos) {
		setContent(content, cursorPos);
	}

	/**
	 * Returns the part of the content before the cursor
	 */
	public String getBeforeCursor() {
		return content.substring(0, cursorPos);
	}
	
	/**
	 * Replaces the text before the cursor with the given text.
	 */
	public void setBeforeCursor(String value) {
		setContent(value + getAfterCursor(), value.length());
	}

	/**
	 * Returns the part of the content after the cursor.
	 */
	public String getAfterCursor() {
		return content.substring(cursorPos);
	}

	/**
	 * Replaces the text after the cursor with the given text.
	 */
	public void setAfterCursor(String value) {
		setContent(getBeforeCursor() + value, cursorPos);
	}
}
