package org.irssi.webssi.client;

import org.irssi.webssi.client.control.Commander;

/**
 * Adapted Webssi module for unit tests.
 * It doesn't activate synchronization at startup
 * (because scheduling a DeferedCommand at that time doesn't seem to work well in a GWTTestCase).
 * Optionally, it totally deactivates the link; and it exposes more to ease testing.
 */
public class TestWebssi extends Webssi {
	private class TestJsonLink extends JsonLink {
		public void scheduleSyncFast() {
			if (allowLink)
				super.scheduleSyncFast();
		}
	}
	
	private TestJsonLink link;
	private boolean allowLink;
	
	public TestWebssi() {
		this(true);
	}
	
	public TestWebssi(boolean allowLink) {
		this.allowLink = allowLink;
	}
	
	@Override
	JsonLink createLink() {
		link = new TestJsonLink();
		return link;
	}
	
	public void fakeIncoming(String json)  {
		link.processEvents(json);
	}
	
	// make public
	@Override
	public Commander getCommander() {
		return super.getCommander();
	}
}
