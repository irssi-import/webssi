package org.irssi.webssi.client.command;

import org.irssi.webssi.client.model.Window;

import com.google.gwt.core.client.JavaScriptObject;

/**
 * Makes the specified window the active window.
 */
public class ActivateWindowCommand extends Command {
	private final Window win;
	
	public ActivateWindowCommand(Window win) {
		this.win = win;
	}

	@Override
	public JavaScriptObject createJS() {
		return js(win.getId());
	}

	private static native JavaScriptObject js(String winId) /*-{
		return { "type": "activateWindow", "win" : winId };
	}-*/;
}
