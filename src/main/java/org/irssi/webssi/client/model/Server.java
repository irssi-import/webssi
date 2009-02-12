package org.irssi.webssi.client.model;

/**
 * Model of a server.
 */
public class Server implements Comparable<Server> {
	private final String tag;
	private final Group<WindowItem> items;
	
	public Server(String tag) {
		super();
		this.tag = tag;
		this.items = new Group<WindowItem>();
	}

	public int compareTo(Server o) {
		return tag.compareTo(o.tag);
	}
	
	public String getTag() {
		return tag;
	}
	
	public Group<WindowItem> getItems() {
		return items;
	}
}
