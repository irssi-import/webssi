package org.irssi.webssi.client;

import java.util.List;

import org.irssi.webssi.client.command.Command;
import org.irssi.webssi.client.events.EventHandler;

/**
 * Handles communication to irssi.
 */
public interface Link {
	/**
	 * Add the given command to the command queue, and schedule a sync.
	 */
	public void sendCommand(Command command);
	
	/**
	 * Register the given handler to be called every time we receive an event of the given type.
	 * It is the responsibility of the caller to assure that the handler's event class is appropriate for the given event type. 
	 */
	public void addEventHandler(String type, EventHandler<?> handler);
	
	/**
	 * List of commands that we sent but haven't received confirmation for
	 */
	public List<Command> getPendingCommands();
}
