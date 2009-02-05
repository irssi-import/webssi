package org.irssi.webssi.client.model;

/**
 * Model of the list of windows.
 */
public class WindowManager {
	
	public static interface Listener {
		void windowChanged(Window win);
	}
	
	/**
	 * Listener delegating to two other listeners
	 */
	private static class CompositeListener implements Listener {
		private final Listener listener1;
		private final Listener listener2;

		private CompositeListener(Listener listener1, Listener listener2) {
			this.listener1 = listener1;
			this.listener2 = listener2;
		}
		
		public void windowChanged(Window win) {
			listener1.windowChanged(win);
			listener2.windowChanged(win);
		}
	}
	
	private final Group<Window> windows = new Group<Window>();
	private Window activeWindow;
	
	private Listener listener;
	
	public Group<Window> getWindows() {
		return windows;
	}
	
	public void addListener(Listener listener) {
		if (this.listener == null)
			this.listener = listener;
		else
			this.listener = new CompositeListener(this.listener, listener);
	}

	public void setActiveWindow(Window win) {
		if (activeWindow != win) {
			activeWindow = win;
			listener.windowChanged(win);
		}
	}

	/**
	 * Returns the active window.
	 * This is the window that's active here, which is not always the same as the one in irssi.
	 */
	public Window getActiveWindow() {
		return activeWindow;
	}
}
