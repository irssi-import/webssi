package org.irssi.webssi.client.model;

public class Model {
	private final WindowManager wm = new WindowManager();
	private final Group<Server> servers = new Group<Server>();
	private final Group<WindowItem> noServerItems = new Group<WindowItem>();

	public WindowManager getWm() {
		return wm;
	}
	
	public Group<Server> getServers() {
		return servers;
	}
	
	/**
	 * Returns the window items that don't have an associated server.
	 */
	public Group<WindowItem> getNoServerWindowItems() {
		return noServerItems;
	}
	
	
}
