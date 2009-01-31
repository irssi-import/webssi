package org.irssi.webssi.client;

import org.irssi.webssi.client.model.Window;
import org.irssi.webssi.client.view.View;

/**
 * Controller. Used by the view to send commands.
 * 
 * @author Wouter Coekaerts <wouter@coekaerts.be>
 */
public class Controller {
	private final View view;
	private final Link link;

	public Controller(View view, Link link) {
		this.view = view;
		this.link = link;
		view.setController(this);
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
