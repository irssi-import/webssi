package org.irssi.webssi.client.control;

import org.irssi.webssi.client.events.JsonEvent;
import org.irssi.webssi.client.events.WindowItemEvent;
import org.irssi.webssi.client.events.WindowItemMovedEvent;
import org.irssi.webssi.client.model.Window;
import org.irssi.webssi.client.model.WindowItem;
import org.irssi.webssi.client.sync.ModelLocator;

import com.google.gwt.core.client.JavaScriptObject;

/**
 * Makes the specified item the active item in its window.
 * This command does not make that window the active one.
 */
class ActivateWindowItemCommand extends Command {
	private class ActiveWindowItemPredictable extends Predictable {
		public ActiveWindowItemPredictable() {
			super("window item changed");
		}
		
		public boolean affectedBy(JsonEvent event) {
			if (! super.affectedBy(event))
				return false;
			WindowItemMovedEvent wime = event.cast();
			return winItemLocator.getModelFrom(wime).getWin() == window;
		}
		
		@Override
		public boolean equals(Object obj) {
			return super.equals(obj) && ((ActiveWindowItemPredictable)obj).getWindow() == getWindow();
		}
		
		private Window getWindow() {
			return window;
		}
	}
	
	private final Predictable activeItemPredictable = new ActiveWindowItemPredictable();
	private final WindowItem item;
	private final ModelLocator<WindowItem, WindowItemEvent> winItemLocator;
	private Window window;
	private WindowItem previousActiveItem;
	
	ActivateWindowItemCommand(WindowItem item, ModelLocator<WindowItem, WindowItemEvent> winItemLocator) {
		super(js(item.getId()));
		this.item = item;
		this.winItemLocator = winItemLocator;
	}
	
	@Override
	void execute() {
		previousActiveItem = item.getWin().getActiveItem();
		window = item.getWin();
		item.getWin().setActiveItem(item);
	}
	
	private static native JavaScriptObject js(String itemId) /*-{
		return { "type": "activateWindowItem", "item" : itemId };
	}-*/;
	
	@Override
	Predictable getPredictable() {
		return activeItemPredictable;
	}
	
	@Override
	boolean echoMatches(JsonEvent event) {
		WindowItemMovedEvent wime = event.cast();
		return winItemLocator.getModelFrom(wime) == item;
	}
	
	@Override
	void undo() {
		if (previousActiveItem == null || previousActiveItem.getWin() == window)
			window.setActiveItem(previousActiveItem);
	}
}
