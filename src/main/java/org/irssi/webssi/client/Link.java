package org.irssi.webssi.client;

import org.irssi.webssi.client.events.EventHandler;

/**
 * Handles communication to irssi.
 */
public interface Link {
	public void sendLine(String win, String command);
	
	/**
	 * Register the given handler to be called every time we receive an event of the given type.
	 * It is the responsibility of the caller to assure that the handler's event class is appropriate for the given event type. 
	 */
	public void setEventHandler(String type, EventHandler<?> handler);
	
	/**
	 * Register another event handler for an event where another handler already exists.
	 * This is meant for tests.
	 */
	public void addSecondaryEventHandler(String type, EventHandler<?> handler);
}
