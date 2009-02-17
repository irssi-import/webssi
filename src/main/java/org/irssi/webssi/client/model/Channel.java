package org.irssi.webssi.client.model;

/**
 * Model of a channel.
 */
public class Channel extends WindowItem {
	private Group<Nick> nicks = new Group<Nick>();
	
	public Channel(String visibleName, Server server, Window win, String id) {
		super(visibleName, server, win, id);
	}
	
	public Group<Nick> getNicks() {
		return nicks;
	}
}
