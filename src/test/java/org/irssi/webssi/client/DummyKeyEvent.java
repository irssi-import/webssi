package org.irssi.webssi.client;

import com.google.gwt.event.dom.client.KeyEvent;
import com.google.gwt.event.shared.EventHandler;

public class DummyKeyEvent extends KeyEvent<EventHandler> {
	@Override
	public Type<EventHandler> getAssociatedType() {
		throw new RuntimeException("not implemented");
	}

	@Override
	protected void dispatch(EventHandler handler) {
		throw new RuntimeException("not implemented");
	}
	
	@Override
	public boolean isAltKeyDown() {
		return false;
	}
	
	@Override
	public boolean isControlKeyDown() {
		return false;
	}
	
	@Override
	public boolean isShiftKeyDown() {
		return false;
	}
}
