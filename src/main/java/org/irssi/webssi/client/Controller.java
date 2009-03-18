package org.irssi.webssi.client;

import java.util.List;

import org.irssi.webssi.client.command.ActivateWindowCommand;
import org.irssi.webssi.client.command.ActivateWindowItemCommand;
import org.irssi.webssi.client.command.Command;
import org.irssi.webssi.client.command.KeyCommand;
import org.irssi.webssi.client.command.SendLineCommand;
import org.irssi.webssi.client.events.JsonEvent;
import org.irssi.webssi.client.model.Model;
import org.irssi.webssi.client.model.Window;
import org.irssi.webssi.client.model.WindowItem;
import org.irssi.webssi.client.sync.Synchronizers;
import org.irssi.webssi.client.view.View;

/**
 * Controller. Used by the view to send commands.
 * 
 * @author Wouter Coekaerts <wouter@coekaerts.be>
 */
public class Controller {
	private final Model model;
	private final View view;
	private final Link link;
	private final Synchronizers synchronizers;

	public Controller(Model model, View view, Link link, Synchronizers synchronizers) {
		this.model = model;
		this.view = view;
		this.link = link;
		this.synchronizers = synchronizers;
		view.setController(this);
	}
	
	/**
	 * Activate the given window.
	 */
	public void activateWindow(Window window) {
		if (model.getWm().getActiveWindow() != window) {
			execute(new ActivateWindowCommand(model.getWm(), window, synchronizers.getWindowLocator()));
		}
	}

	/**
	 * Activate the item, and activate the window it is in.
	 */
	public void activateWindowItem(WindowItem item) {
		Window window = item.getWin();
		if (window.getActiveItem() != item) {
			execute(new ActivateWindowItemCommand(item, synchronizers.getWindowItemLocator()));
		}
		activateWindow(window);
	}
	
	/**
	 * The given command (or line to say) has been entered by the user in the given window.
	 */
	public void sendLine(Window win, String command) {
		link.sendCommand(new SendLineCommand(win, command));
	}
	
	public void keyPressed(char keyCode, char keyChar, int modifiers) {
		KeyCommand command = new KeyCommand(model.getEntry(), keyCode, keyChar, modifiers);
		command.execute();
		link.sendCommand(command);
	}
	
	public void debugMessage(String type, String message) {
		view.debug(type, message);
	}
	
	private void execute(Command command) {
		link.sendCommand(command);
		command.execute();
	}
	
	/**
	 * Called after events have been processed, to replay pending commands if necessary.
	 * @param events List of events that have just been processed, excluding events that were recognized as echo.
	 */
	public void eventsProcessed(List<JsonEvent> events) {
		for (Command pendingCommand : link.getPendingCommands()) {
			for (JsonEvent event : events) {
				if (pendingCommand.needReplayAfter(event)) {
					pendingCommand.execute();
					break;
				}
			}
		}
	}
	
}
