package org.irssi.webssi.client.model;

import org.irssi.webssi.client.expect.ExpectSession;
import org.irssi.webssi.client.expect.Expectable;
import org.irssi.webssi.client.model.Window;
import org.irssi.webssi.client.model.WindowManager;

public class TestWindowManagerListener extends TestListener implements WindowManager.Listener {
	public static final Expectable<Window> WINDOW_CHANGED = declareExpectable("windowChanged");
	
	private final WindowManager.Listener delegate;
	private int listenCount = 1;
	
	public static void listen(ExpectSession session, WindowManager wm) {
		if (wm.getListener() instanceof TestWindowManagerListener) {
			((TestWindowManagerListener)wm.getListener()).listenCount++;
		} else {
			wm.setListener(new TestWindowManagerListener(session, wm.getListener()));
		}
	}
	
	private TestWindowManagerListener(ExpectSession session, WindowManager.Listener delegate) {
		super(session);
		this.delegate = delegate;
	}
	
	public void windowChanged(Window win) {
		delegate.windowChanged(win);
		called(WINDOW_CHANGED, win);
	}
}