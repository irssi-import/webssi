package org.irssi.webssi.client.model;

/**
 * Model of a window item
 */
public class WindowItem implements Comparable<WindowItem> {
	private String visibleName;
	private final Server server;
	private Window win;
	
	public WindowItem(String visibleName, Server server, Window win) {
		this.visibleName = visibleName;
		this.server = server;
		this.win = win;
	}
	
	public String getVisibleName() {
		return visibleName;
	}
	
	public Server getServer() {
		return server;
	}
	
	public Window getWin() {
		return win;
	}
	
	public void setWin(Window win) {
		this.win = win;
	}
	
	public int compareTo(WindowItem item) {
		if (item == this)
			return 0;
		int visibleNameComparison = visibleName.toLowerCase().compareTo(item.visibleName.toLowerCase());
		if (visibleNameComparison != 0)
			return visibleNameComparison;
		return this.hashCode() < item.hashCode() ? -1 : 1; // random consistent order
	}
}
