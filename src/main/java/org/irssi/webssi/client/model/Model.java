package org.irssi.webssi.client.model;

public class Model {
	@Deprecated
	public final static Model INSTANCE = new Model();
	
	private final WindowManager wm = new WindowManager();

	public WindowManager getWm() {
		return wm;
	}
}
