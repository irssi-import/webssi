package org.irssi.webssi.client.control;

import org.irssi.webssi.client.events.WindowEvent;
import org.irssi.webssi.client.model.Window;
import org.irssi.webssi.client.model.WindowManager;
import org.irssi.webssi.client.sync.ModelLocator;

import com.google.gwt.core.client.JavaScriptObject;

/**
 * Makes the specified window the active window.
 */
class ActivateWindowCommand extends SingleEchoCommand<Window, WindowEvent> {
	private final WindowManager wm;
	private final Window win;
	
	ActivateWindowCommand(WindowManager wm, Window win, ModelLocator<Window, WindowEvent> modelLocator) {
		super(js(win.getId()), "window changed", modelLocator);
		this.wm = wm;
		this.win = win;
	}
	
	@Override
	void execute() {
		wm.setActiveWindow(win);
	}
	
	@Override
	boolean echo(Window window, WindowEvent event) {
		return window == win;
	}

	private static native JavaScriptObject js(String winId) /*-{
		return { "type": "activateWindow", "win" : winId };
	}-*/;
}
