package org.irssi.webssi.client;

import org.irssi.webssi.client.command.ActivateWindowCommand;
import org.irssi.webssi.client.command.ActivateWindowItemCommand;
import org.irssi.webssi.client.command.SendLineCommand;
import org.irssi.webssi.client.model.Model;
import org.irssi.webssi.client.model.Window;
import org.irssi.webssi.client.model.WindowItem;
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

	public Controller(Model model, View view, Link link) {
		this.model = model;
		this.view = view;
		this.link = link;
		view.setController(this);
	}
	
	/**
	 * Activate the given window.
	 * For now this only activates it locally, not in irssi.
	 */
	public void activateWindow(Window window) {
		if (model.getWm().getActiveWindow() != window) {
			model.getWm().setActiveWindow(window);
			link.sendCommand(new ActivateWindowCommand(window));
		}
	}
	
	public void activateWindowItem(WindowItem item) {
		Window window = item.getWin();
		if (window.getActiveItem() != item) {
			window.setActiveItem(item);
			link.sendCommand(new ActivateWindowItemCommand(item));
		}
		activateWindow(window);
	}
	
	/**
	 * The given command (or line to say) has been entered by the user in the given window.
	 */
	public void sendLine(Window win, String command) {
		link.sendCommand(new SendLineCommand(win, command));
	}
	
	public void debugMessage(String type, String message) {
		view.debug(type, message);
	}
	
}
