package org.irssi.webssi.client.sync;

import org.irssi.webssi.client.Link;
import org.irssi.webssi.client.model.Model;

/**
 * Sets up model synchronization.  
 */
public class Synchronizers {
	public static void init(Model model, Link link) {
		WindowSynchronizer winSync = new WindowSynchronizer(model, link);
		ServerSynchronizer serverSync = new ServerSynchronizer(model, link);
		WindowItemSynchronizer wiSync = new WindowItemSynchronizer(winSync, serverSync, link);
		new NickSynchronizer(wiSync, link);
		new ServerItemSynchronizer(model.getNoServerWindowItems(), serverSync, wiSync, link);
	}
}
