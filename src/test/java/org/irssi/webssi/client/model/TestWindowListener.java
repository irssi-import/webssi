package org.irssi.webssi.client.model;

import org.irssi.webssi.client.expect.ExpectSession;
import org.irssi.webssi.client.expect.Expectable;
import org.irssi.webssi.client.model.Window;
import org.irssi.webssi.client.model.WindowItem;

public class TestWindowListener extends TestListener implements Window.Listener {
	public static final Expectable<WindowItem> WINDOW_ITEM_CHANGED = declareExpectable("windowItemChanged");
	public static final Expectable<String> WINDOW_NAME_CHANGED = declareExpectable("windowNameChanged");
	
	private final Window.Listener delegate;
	private int listenCount = 1;
	
	public static void listen(ExpectSession session, Window win) {
		if (win.getListener() instanceof TestWindowManagerListener) {
			((TestWindowListener)win.getListener()).listenCount++;
		} else {
			win.setListener(new TestWindowListener(session, win.getListener()));
		}
	}
	
//	public static void unListen(ExpectSession session, Window win) {
//		((TestWindowListener)win.getListener()).listenCount--;
//	}
	
	private TestWindowListener(ExpectSession session, Window.Listener delegate) {
		super(session);
		this.delegate = delegate;
	}
	
	public void textPrinted(String text) {
		delegate.textPrinted(text);
	}
	
	public void windowItemChanged(WindowItem item) {
		delegate.windowItemChanged(item);
		called(WINDOW_ITEM_CHANGED, item);
	}
	
	public void nameChanged(String name) {
		delegate.nameChanged(name);
		called(WINDOW_NAME_CHANGED, name);
	}
}