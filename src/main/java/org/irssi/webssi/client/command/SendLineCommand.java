package org.irssi.webssi.client.command;

import org.irssi.webssi.client.model.Window;

import com.google.gwt.core.client.JavaScriptObject;

/**
 * Command executed when the user entered a line.
 */
public class SendLineCommand extends Command {
	private final Window win;
	private final String line;

	public SendLineCommand(Window win, String line) {
		this.win = win;
		this.line = line;
	}

	@Override
	public JavaScriptObject createJS() {
		return js(win.getId(), line);
	}
	
	private static native JavaScriptObject js(String winId, String line) /*-{
		return { "type": "sendLine", "win" : winId, "line": line };
	}-*/;

}
