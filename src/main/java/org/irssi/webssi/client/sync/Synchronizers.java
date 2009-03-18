package org.irssi.webssi.client.sync;

import org.irssi.webssi.client.Link;
import org.irssi.webssi.client.events.WindowEvent;
import org.irssi.webssi.client.events.WindowItemEvent;
import org.irssi.webssi.client.model.Model;
import org.irssi.webssi.client.model.Window;
import org.irssi.webssi.client.model.WindowItem;

/**
 * Sets up model synchronization.  
 */
public class Synchronizers {
	private final WindowSynchronizer windowSynchronizer;
	private final WindowItemSynchronizer wiSync;
	
	public Synchronizers(Model model, Link link) {
		windowSynchronizer = new WindowSynchronizer(model, link);
		ServerSynchronizer serverSync = new ServerSynchronizer(model, link);
		wiSync = new WindowItemSynchronizer(windowSynchronizer, serverSync, link);
		new NickSynchronizer(wiSync, link);
		new ServerItemSynchronizer(model.getNoServerWindowItems(), serverSync, wiSync, link);
		new EntrySynchronizer(model.getEntry(), link);
	}
	
	public ModelLocator<Window, WindowEvent> getWindowLocator() {
		return windowSynchronizer;
	}
	
	public ModelLocator<WindowItem, WindowItemEvent> getWindowItemLocator() {
		return wiSync;
	}
}
