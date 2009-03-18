package org.irssi.webssi.client.model;

import org.irssi.webssi.client.expect.ExpectSession;
import org.irssi.webssi.client.expect.Expectable;

public class TestEntryListener extends TestListener implements Entry.Listener {
	public static final Expectable<String> CONTENT_CHANGED = declareExpectable("contentChanged");
	
	public static void listen(ExpectSession session, Entry entry) {
		entry.addListener(new TestEntryListener(session));
	}
	
	public void contentChanged(String content, int cursorPos) {
		called(CONTENT_CHANGED, content);
	}

	private TestEntryListener(ExpectSession session) {
		super(session);
	}
}
