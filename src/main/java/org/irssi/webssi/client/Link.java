package org.irssi.webssi.client;

import org.irssi.webssi.client.events.EventHandler;

/**
 * Handles communication to irssi.
 */
public interface Link {
	/**
	 * Schedule a sync really soon, but not immediately
	 */
	public void scheduleSyncFast();
	
	/**
	 * Register the given handler to be called every time we receive an event of the given type.
	 * It is the responsibility of the caller to assure that the handler's event class is appropriate for the given event type. 
	 */
	public void addEventHandler(String type, EventHandler<?> handler);
}
