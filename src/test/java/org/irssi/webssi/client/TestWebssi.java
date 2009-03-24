package org.irssi.webssi.client;

import org.irssi.webssi.client.control.Commander;

public class TestWebssi extends Webssi {
	private class TestJsonLink extends JsonLink {
		public void scheduleSyncFast() {
			// do nothing, we don't really want to contact irssi
		}
	}
	
	private TestJsonLink link;
	
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
