package org.irssi.webssi.client.model;

import org.irssi.webssi.client.expect.ExpectSession;
import org.irssi.webssi.client.expect.Expectable;

public class TestWindowManagerListener extends TestListener implements WindowManager.Listener {
	public static final Expectable<Window> WINDOW_CHANGED = declareExpectable("windowChanged");
	
	public static void listen(ExpectSession session, WindowManager wm) {
		wm.addListener(new TestWindowManagerListener(session));
	}
	
	private TestWindowManagerListener(ExpectSession session) {
		super(session);
	}
	
	public void windowChanged(Window win) {
		called(WINDOW_CHANGED, win);
	}
}