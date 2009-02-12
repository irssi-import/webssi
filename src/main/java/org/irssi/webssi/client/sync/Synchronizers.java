package org.irssi.webssi.client.sync;

import org.irssi.webssi.client.Link;
import org.irssi.webssi.client.model.Model;

/**
 * Sets up model synchronization.  
 */
public class Synchronizers {
	public static void init(Model model, Link link) {
		WindowSynchronizer winSync = new WindowSynchronizer(model, link);
		WindowItemSynchronizer wiSync = new WindowItemSynchronizer(winSync, link);
		new NickSynchronizer(wiSync, link);
		ServerSynchronizer serverSync = new ServerSynchronizer(model, link);
		new ServerItemSynchronizer(model.getNoServerWindowItems(), serverSync, wiSync, link);
	}
}
