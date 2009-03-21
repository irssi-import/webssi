package org.irssi.webssi.client.control;

import org.irssi.webssi.client.events.WindowItemEvent;
import org.irssi.webssi.client.model.WindowItem;
import org.irssi.webssi.client.sync.ModelLocator;

import com.google.gwt.core.client.JavaScriptObject;

/**
 * Makes the speficied item the active item in its window.
 * This command does not make that window the active one.
 */
class ActivateWindowItemCommand extends SingleEchoCommand<WindowItem, WindowItemEvent> {
	private final WindowItem item;
	
	ActivateWindowItemCommand(WindowItem item, ModelLocator<WindowItem, WindowItemEvent> modelLocator) {
		super(js(item.getId()), "window item changed", modelLocator);
		this.item = item;
	}
	
	@Override
	void execute() {
		item.getWin().setActiveItem(item);
	}
	
	@Override
	boolean echo(WindowItem item, WindowItemEvent event) {
		return this.item == item;
	}

	private static native JavaScriptObject js(String itemId) /*-{
		return { "type": "activateWindowItem", "item" : itemId };
	}-*/;

}
