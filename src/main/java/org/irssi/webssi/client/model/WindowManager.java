package org.irssi.webssi.client.model;

/**
 * Model of the list of windows.
 */
public class WindowManager {
	
	public static interface Listener {
		void windowChanged(Window win);
	}
	
	private final Group<Window> windows = new Group<Window>();
	//private Window activeWindow;
	
	private Listener listener;
	
	public Group<Window> getWindows() {
		return windows;
	}
	
	public void setListener(Listener listener) {
//		assert this.listener == null;
		this.listener = listener;
	}

	Listener getListener() {
		return listener;
	}

	public void setActiveWindow(Window win) {
		//activeWindow = win;
		listener.windowChanged(win);
	}

	// window active according to irssi model, not always same as active window in browser...
//	public Window getActiveWindow() {
//		return activeWindow;
//	}
}
