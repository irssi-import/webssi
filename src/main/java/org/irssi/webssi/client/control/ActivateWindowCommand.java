package org.irssi.webssi.client.control;

import org.irssi.webssi.client.events.JsonEvent;
import org.irssi.webssi.client.events.WindowEvent;
import org.irssi.webssi.client.model.Window;
import org.irssi.webssi.client.model.WindowManager;
import org.irssi.webssi.client.sync.ModelLocator;

import com.google.gwt.core.client.JavaScriptObject;

/**
 * Makes the specified window the active window.
 */
class ActivateWindowCommand extends Command {
	private static Predictable ACTIVE_WINDOW_PREDICTABLE = new Predictable("window changed");
	
	private final WindowManager wm;
	private final Window win;
	private final ModelLocator<Window, WindowEvent> winLocator;
	private Window previousActiveWindow;
	
	ActivateWindowCommand(WindowManager wm, Window win, ModelLocator<Window, WindowEvent> winLocator) {
		super(js(win.getId()));
		this.wm = wm;
		this.win = win;
		this.winLocator = winLocator;
	}
	
	@Override
	void execute() {
		previousActiveWindow = wm.getActiveWindow();
		wm.setActiveWindow(win);
	}
	
	private static native JavaScriptObject js(String winId) /*-{
		return { "type": "activateWindow", "win" : winId };
	}-*/;
	
	@Override
	Predictable getPredictable() {
		return ACTIVE_WINDOW_PREDICTABLE;
	}
	
	@Override
	boolean echoMatches(JsonEvent event) {
		return winLocator.getModelFrom(event.<WindowEvent>cast()) == win;
	}
	
	@Override
	void undo() {
		wm.setActiveWindow(previousActiveWindow);
	}
}
