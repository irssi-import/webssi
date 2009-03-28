package org.irssi.webssi.client.model;

/**
 * Model of a window item
 */
public class WindowItem implements Comparable<WindowItem> {
	private String visibleName;
	private final Server server;
	private Window win;
	private final String id;
	private final Activity activity;
	
	public WindowItem(String visibleName, Server server, Window win, String id) {
		this.visibleName = visibleName;
		this.server = server;
		this.win = win;
		this.id = id;
		this.activity = new Activity();
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
	
	public Activity getActivity() {
		return activity;
	}
	
	public int compareTo(WindowItem item) {
		if (item == this)
			return 0;
		int visibleNameComparison = visibleName.toLowerCase().compareTo(item.visibleName.toLowerCase());
		if (visibleNameComparison != 0)
			return visibleNameComparison;
		return this.hashCode() < item.hashCode() ? -1 : 1; // random consistent order
	}
	
	public String getId() {
		return id;
	}
}
