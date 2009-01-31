package org.irssi.webssi.client.expect;

public interface ExpectListener {
	<T> void called(Expectable<T> expectable, T param);
	void log(String msg);
	void done();
}
