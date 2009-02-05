package org.irssi.webssi.client;

import org.irssi.webssi.client.model.Model;
import org.irssi.webssi.client.model.Window;
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
		model.getWm().setActiveWindow(window);
		// TODO also activate the window in irssi through link
	}
	
	/**
	 * The given command (or line to say) has been entered by the user in the given window.
	 */
	public void sendLine(Window win, String command) {
		link.sendLine(win.getId(), command);
	}
	
	public void debugMessage(String type, String message) {
		view.debug(type, message);
	}
	
}
