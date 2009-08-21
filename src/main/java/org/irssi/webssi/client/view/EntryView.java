package org.irssi.webssi.client.view;

import org.irssi.webssi.client.control.Controller;
import org.irssi.webssi.client.model.Entry;
import org.irssi.webssi.client.model.Model;

import com.google.gwt.event.dom.client.KeyDownEvent;
import com.google.gwt.event.dom.client.KeyDownHandler;
import com.google.gwt.event.dom.client.KeyEvent;
import com.google.gwt.event.dom.client.KeyPressEvent;
import com.google.gwt.event.dom.client.KeyPressHandler;
import com.google.gwt.event.dom.client.KeyUpEvent;
import com.google.gwt.event.dom.client.KeyUpHandler;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.DeferredCommand;
import com.google.gwt.user.client.Element;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.TextBox;

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
	private int downKeyCode;
	
	public EntryView(final Model model) {
		this.entry = model.getEntry();
		entry.addListener(this);
		textBox = new TextBox();
		initWidget(textBox);
		
		blockDefaultTab(textBox.getElement());
		
		textBox.addKeyDownHandler(new KeyDownHandler() {
			public void onKeyDown(KeyDownEvent event) {
				logKey(event, event.getNativeKeyCode());
				downKeyCode = event.getNativeKeyCode();
				lastKeyEventWasKeyPress = false;
			}
		});
		
		textBox.addKeyPressHandler(new KeyPressHandler() {
			public void onKeyPress(KeyPressEvent event) {
				logKey(event, event.getCharCode());
				controller.keyPressed(downKeyCode, event.getCharCode(), event);
				lastKeyEventWasKeyPress = true;
			}
		});
		
		textBox.addKeyUpHandler(new KeyUpHandler() {
			public void onKeyUp(KeyUpEvent event) {
				logKey(event, event.getNativeKeyCode());
				if (! lastKeyEventWasKeyPress) {
					controller.keyPressed(event.getNativeKeyCode(), '\0', event);
				}
				lastKeyEventWasKeyPress = false;
			}
		});
	}
	
	private static void logKey(KeyEvent<?> event, int code) {
		System.err.println("key " + event.getAssociatedType().getName() + ": " + code);
	}
	
//	private static void logKey(String type, int keyCode/*, int modifiers*/) {
//		System.err.println("key " + type + ": " + keyCode + " (" + keyCode + ") " /*mod:" + modifiers*/);
//	}
	
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
