package org.irssi.webssi.client.control;

import org.irssi.webssi.client.model.Model;
import org.irssi.webssi.client.model.Window;
import org.irssi.webssi.client.model.WindowItem;
import org.irssi.webssi.client.sync.Synchronizers;
import org.irssi.webssi.client.view.View;

import com.google.gwt.event.dom.client.KeyEvent;

/**
 * Controller. Used by the view to send commands.
 */
public class Controller {
	private final Model model;
	private final View view;
	private final Synchronizers synchronizers;
	private final Commander commander;

	public Controller(Model model, View view, Commander commander, Synchronizers synchronizers) {
		this.model = model;
		this.view = view;
		this.commander = commander;
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
		execute(new SendLineCommand(win, command));
	}
	
	public void keyPressed(int keyCode, char charCode, KeyEvent<?> event) {
		KeyCommand command = new KeyCommand(model.getEntry(), keyCode, charCode, event);
		execute(command);
	}
	
	public void debugMessage(String type, String message) {
		view.debug(type, message);
	}
	
	private void execute(Command command) {
		commander.execute(command);
	}
}
