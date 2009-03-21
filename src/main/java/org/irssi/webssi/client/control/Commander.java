package org.irssi.webssi.client.control;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.irssi.webssi.client.Link;
import org.irssi.webssi.client.events.CommandEvent;
import org.irssi.webssi.client.events.EventHandler;
import org.irssi.webssi.client.events.JsonEvent;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.json.client.JSONArray;
import com.google.gwt.json.client.JSONObject;

/**
 * Manages the commands.
 * Manages the list of pending commands, and deals with echo and replaying.
 */
public class Commander {
	private final Link link;
	
	/**
	 * List of commands that we sent but haven't received confirmation for
	 */
	private final List<Command> pendingCommands = new LinkedList<Command>();
	
	/**
	 * Events that have been preprocessed but not postprocessed,
	 * excluding events that were recognized as echo.
	 */
	private final List<JsonEvent> processingEvents = new ArrayList<JsonEvent>();

	/**
	 * Queue of outgoing commands to be sent with the next sync
	 */
	private JSONArray commandQueue = new JSONArray();
	
	/**
	 * The command who's response is being processed.
	 */
	private Command processingCommand;
	
	public Commander(Link link) {
		this.link = link;
		link.addEventHandler("command", new EventHandler<CommandEvent>(){
			public void handle(CommandEvent event) {
				Integer commandId = event.getCommandId();
				if (commandId == -1) {
					processingCommand = null;
				} else {
					processingCommand = pendingCommands.remove(0);
					assert event.getCommandId() == getCommandId(processingCommand);
				}
			}
		});
	}

	/**
	 * Execute the given command, add it to the queue to send to irssi, and schedule a sync really soon 
	 */
	void execute(Command command) {
		command.execute();
		JavaScriptObject js = command.getJS();
		pendingCommands.add(command);
		addCommandId(js, getCommandId(command));
		commandQueue.set(commandQueue.size(), new JSONObject(js));
		link.scheduleSyncFast();
	}
	
	/**
	 * We don't really need an id for commands yes, just used to double-check
	 * if we receive command replies in the same order we sent them.
	 */
	private int getCommandId(Command command) {
		int hashCode = command.hashCode();
		return hashCode == -1 ? 1 : hashCode; // avoid -1 as id, it means no command
	}
	
	private static native void addCommandId(JavaScriptObject command, int id) /*-{
		command['id'] = id;
	}-*/;
	
	/**
	 * Returns the queue of commands converted in a String containing JSON to be sent to irssi,
	 * and empties the queue.
	 */
	public String popCommands() {
		String result = commandQueue.toString();
		commandQueue = new JSONArray();
		return result;
	}
	
	/**
	 * Called for every incoming event right before it is processed.
	 * If this returns true, the event will not be handled by its normal listeners.
	 */
	public boolean preProcessEvent(JsonEvent event) {
		if (processingCommand != null && processingCommand.echo(event)) {
			return true;
		} else {
			processingEvents.add(event);
			return false;
		}
	}
	
	/**
	 * Called after events have been processed, to replay pending commands if necessary.
	 */
	public void postProcessEvents() {
		for (Command pendingCommand : pendingCommands) {
			for (JsonEvent event : processingEvents) {
				if (pendingCommand.needReplayAfter(event)) {
					pendingCommand.execute();
					break;
				}
			}
		}
		processingEvents.clear();
	}
}
