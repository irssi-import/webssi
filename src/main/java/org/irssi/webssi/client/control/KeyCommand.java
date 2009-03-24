package org.irssi.webssi.client.control;

import org.irssi.webssi.client.events.EntryChangedEvent;
import org.irssi.webssi.client.events.JsonEvent;
import org.irssi.webssi.client.model.Entry;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.user.client.ui.KeyboardListener;

/**
 * Command executed when the user presses a key.
 */
class KeyCommand extends Command {
	private static Predictable ENTRY_PREDICTABLE = new Predictable("entry changed");
	
	private final Entry entry;
	private final char keyCode;
	private final char keyChar;
	private final int modifiers;
	
	private String origEntryContent;
	private int origEntryPos;
	
	/**
	 * Predicted entry after the command has been executed.
	 * This might not be right, but we'll correct it when we receive the response from irssi.
	 */
	private String predictedEntryContent;
	private int predictedEntryPos;
	
	private JavaScriptObject js;
	
	KeyCommand(Entry entry, char keyCode, char keyChar, int modifiers) {
		super();
		this.entry = entry;
		this.keyCode = keyCode;
		this.keyChar = keyChar;
		this.modifiers = modifiers;
	}
	
	@Override
	void execute() {
		origEntryContent = entry.getContent();
		origEntryPos = entry.getCursorPos();
		
		IntArray keys = IntArray.create();
		
		if ((modifiers & KeyboardListener.MODIFIER_ALT) != 0)
			keys.push(27);
		
		if ((modifiers & KeyboardListener.MODIFIER_CTRL) != 0 && keyCode >= 'A' && keyCode <= 'Z') {
			keys.push(keyCode - 'A' + 1);
		} else if (keyCode == KeyboardListener.KEY_ENTER) {
			keys.push(10);
			entry.setContent("", 0);
		} else if (keyCode == KeyboardListener.KEY_BACKSPACE) {
			keys.push(127);
			if (entry.getCursorPos() != 0) {
				String beforeCursor = entry.getBeforeCursor();
				entry.setBeforeCursor(beforeCursor.substring(0, beforeCursor.length() - 1));
			}
		} else if (keyCode == KeyboardListener.KEY_DELETE) {
			keys.push(27, 91, 51, 126);
			if (entry.getCursorPos() != entry.getContent().length()) {
				entry.setAfterCursor(entry.getAfterCursor().substring(1));
			}
		} else if (keyCode == KeyboardListener.KEY_LEFT) {
			keys.push(27, 91, 68);
			if (entry.getCursorPos() != 0)
				entry.setCursorPos(entry.getCursorPos() - 1);
		} else if (keyCode == KeyboardListener.KEY_RIGHT) {
			keys.push(27, 91, 67);
			if (entry.getCursorPos() != entry.getContent().length())
				entry.setCursorPos(entry.getCursorPos() + 1);
		} else if (keyCode == KeyboardListener.KEY_UP) {
			keys.push(27, 91, 65);
		} else if (keyCode == KeyboardListener.KEY_DOWN) {
			keys.push(27, 91, 66);
		} else if (keyCode == KeyboardListener.KEY_HOME) {
			keys.push(27, 91, 72);
			entry.setCursorPos(0);
		} else if (keyCode == KeyboardListener.KEY_END) {
			keys.push(27, 91, 70);
			entry.setCursorPos(entry.getContent().length());
		} else if (keyCode == KeyboardListener.KEY_PAGEUP) {
			keys.push(27, 91, 53, 126);
		} else if (keyCode == KeyboardListener.KEY_PAGEDOWN) {
			keys.push(27, 91, 54, 126);
		} else if (keyCode == KeyboardListener.KEY_ESCAPE) {
			keys.push(27);
		} else if (keyCode == KeyboardListener.KEY_TAB) {
			keys.push(9);
		} else if (keyChar != 0) {
			keys.push(keyChar);
			entry.setBeforeCursor(entry.getBeforeCursor() + keyChar);
		}
		
		predictedEntryContent = entry.getContent();
		predictedEntryPos = entry.getCursorPos();
		
		js = js(keys);
	}
	
	@Override
	Predictable getPredictable() {
		return ENTRY_PREDICTABLE;
	}
	
	@Override
	boolean echoMatches(JsonEvent event) {
		EntryChangedEvent entryEvent = event.<EntryChangedEvent>cast();
		
		// if the entry is as expected
		if (entryEvent.getContent().equals(predictedEntryContent)
			&& entryEvent.getCursorPos() == predictedEntryPos) {
			return true; // ignore the event
		} else {
			return false;
		}
	}
	
	@Override
	void undo() {
		entry.setContent(origEntryContent, origEntryPos);
	}
	
	@Override
	JavaScriptObject getJS() {
		if (js == null)
			throw new IllegalStateException("Can't get JS before executing");
		return js;
	}
	
	private static class IntArray extends JavaScriptObject {
		protected IntArray() {}
		
		private static native IntArray create() /*-{
			return [];
		}-*/;
		
		private native void push(int i) /*-{
			this[this.length] = i;
		}-*/;
		
		private void push(int ...ints) {
			for (int i : ints)
				push(i);
		}
	}
	
	private static native JavaScriptObject js(IntArray keys) /*-{
		return { "type": "key", "keys" : keys };
	}-*/;
}
