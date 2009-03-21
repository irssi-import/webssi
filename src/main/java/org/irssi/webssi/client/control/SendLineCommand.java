package org.irssi.webssi.client.control;

import org.irssi.webssi.client.model.Window;

import com.google.gwt.core.client.JavaScriptObject;

/**
 * Command executed when the user entered a line.
 */
class SendLineCommand extends Command {
	SendLineCommand(Window win, String line) {
		super(js(win == null ? null : win.getId(), line));
	}
	
	@Override
	void execute() {
		// do nothing, we can't predict the result
	}
	
	private static native JavaScriptObject js(String winId, String line) /*-{
		return { "type": "sendLine", "win" : winId, "line": line };
	}-*/;

}
