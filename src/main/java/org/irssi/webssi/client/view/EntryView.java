package org.irssi.webssi.client.view;

import org.irssi.webssi.client.control.Controller;
import org.irssi.webssi.client.model.Entry;
import org.irssi.webssi.client.model.Model;

import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.DeferredCommand;
import com.google.gwt.user.client.Element;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.KeyboardListener;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.Widget;

public class EntryView extends Composite implements Entry.Listener {
	private final TextBox textBox;
	private Controller controller;
	private final Entry entry;
	
	/**
	 * If the last event received was onKeyPress. 
	 */
	private boolean lastKeyEventWasKeyPress;
	
	/**
	 * Key code of the last received onKeyDown.
	 * This is remembered because otherwise in onKeyPress we can't distinguish between some keys
	 */
	private char downKeyCode;
	
	public EntryView(final Model model) {
		this.entry = model.getEntry();
		entry.addListener(this);
		textBox = new TextBox();
		initWidget(textBox);
		
		blockDefaultTab(textBox.getElement());
		
		textBox.addKeyboardListener(new KeyboardListener() {
			public void onKeyDown(Widget sender, char keyCode, int modifiers) {
				logKey("down", keyCode, modifiers);
				downKeyCode = keyCode;
				lastKeyEventWasKeyPress = false;
			}

			public void onKeyPress(Widget sender, char keyCode, int modifiers) {
				logKey("press", keyCode, modifiers);
				controller.keyPressed(downKeyCode, keyCode, modifiers);
				lastKeyEventWasKeyPress = true;
//				refresh();
			}

			public void onKeyUp(Widget sender, char keyCode, int modifiers) {
				logKey("up", keyCode, modifiers);
				if (! lastKeyEventWasKeyPress) {
					controller.keyPressed(keyCode, '\0', modifiers);
				}
				lastKeyEventWasKeyPress = false;
//				refresh();
			}
		});
		
	}
	
	private static void logKey(String type, char keyCode, int modifiers) {
		System.err.println("key " + type + ": " + ((int)keyCode) + " (" + keyCode + ") mod:" + modifiers);
	}
	
	public void setController(Controller controller) {
		this.controller = controller;
	}
	
	public void contentChanged(String content, int cursorPos) {
		// refresh the content later, to avoid flickering
		DeferredCommand.addCommand(new Command() {
			public void execute() {
				refresh();
			}
		});
	}
	
	/**
	 * Sets the textBox state to the state from the model.
	 */
	private void refresh() {
		textBox.setText(entry.getContent());
		textBox.setCursorPos(entry.getCursorPos());
	}
	
	/**
	 * Blocks default behaviour of keys.
	 * Based on http://stackoverflow.com/questions/3362/capturing-tab-key-in-text-box
	 * @param myInput the textBox element.
	 */
	private static native void blockDefaultTab(Element myInput) /*-{
		var keyHandler = function(e) {
			if(e.preventDefault) {
				e.preventDefault();
			}
			return false;
		};
		
		if (myInput.addEventListener) {
			myInput.addEventListener('keydown', keyHandler, false);
		} else if(el.attachEvent ) {
			myInput.attachEvent('onkeydown', keyHandler); // IE hack
		}
	}-*/;
	
}
