package org.irssi.webssi.client.command;

import org.irssi.webssi.client.model.Window;

import com.google.gwt.core.client.JavaScriptObject;

/**
 * Command executed when the user entered a line.
 */
public class SendLineCommand extends Command {
	public SendLineCommand(Window win, String line) {
		super(js(win == null ? null : win.getId(), line));
	}
	
	@Override
	public void execute() {
		// do nothing, we can't predict the result
	}
	
	private static native JavaScriptObject js(String winId, String line) /*-{
		return { "type": "sendLine", "win" : winId, "line": line };
	}-*/;

}
