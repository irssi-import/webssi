package org.irssi.webssi.client.command;

import org.irssi.webssi.client.model.WindowItem;

import com.google.gwt.core.client.JavaScriptObject;

/**
 * Makes the speficied item the active item in its window.
 * This command does not make that window the active one.
 */
public class ActivateWindowItemCommand extends Command {

	private final WindowItem item;
	
	public ActivateWindowItemCommand(WindowItem item) {
		this.item = item;
	}

	@Override
	public JavaScriptObject createJS() {
		return js(item.getId());
	}

	private static native JavaScriptObject js(String itemId) /*-{
		return { "type": "activateWindowItem", "item" : itemId };
	}-*/;

}
