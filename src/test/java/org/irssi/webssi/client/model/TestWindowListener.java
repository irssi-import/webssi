package org.irssi.webssi.client.model;

import org.irssi.webssi.client.expect.ExpectSession;
import org.irssi.webssi.client.expect.Expectable;
import org.irssi.webssi.client.model.Window;
import org.irssi.webssi.client.model.WindowItem;

public class TestWindowListener extends TestListener implements Window.Listener {
	public static final Expectable<WindowItem> WINDOW_ITEM_CHANGED = declareExpectable("windowItemChanged");
	public static final Expectable<String> WINDOW_NAME_CHANGED = declareExpectable("windowNameChanged");
	
	public static void listen(ExpectSession session, Window win) {
		win.addListener(new TestWindowListener(session));
	}
	
	private TestWindowListener(ExpectSession session) {
		super(session);
	}
	
	public void textPrinted(String text) {
		// do nothing, we don't test for lines being printed
	}
	
	public void windowItemChanged(WindowItem item) {
		called(WINDOW_ITEM_CHANGED, item);
	}
	
	public void nameChanged(String name) {
		called(WINDOW_NAME_CHANGED, name);
	}
}