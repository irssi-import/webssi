package org.irssi.webssi.client.model;

import org.irssi.webssi.client.expect.ExpectSession;
import org.irssi.webssi.client.expect.Expectable;

class TestListener {
	static <T> Expectable<T> declareExpectable(String name) {
		return Expectable.declareExpectable(name);
	}
	
	private final ExpectSession session;
	
	<T> void called(Expectable<T> expectable, T param) {
		session.called(expectable, param);
	}
	
	TestListener(ExpectSession session) {
		this.session = session;
	}
}
