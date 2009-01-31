package org.irssi.webssi.client.sync;

import org.irssi.webssi.client.Link;
import org.irssi.webssi.client.events.EventHandler;
import org.irssi.webssi.client.events.WindowEvent;
import org.irssi.webssi.client.events.WindowItemEvent;
import org.irssi.webssi.client.events.WindowItemNewEvent;
import org.irssi.webssi.client.model.Channel;
import org.irssi.webssi.client.model.Group;
import org.irssi.webssi.client.model.Window;
import org.irssi.webssi.client.model.WindowItem;

class WindowItemSynchronizer extends Synchronizer<WindowItem, WindowItemEvent, WindowItemNewEvent> {
	private final ModelLocator<Window, WindowEvent> winLocator;
	
	/**
	 * Last item that was removed. Keeping a reference to this because it might actually be moved
	 */
	private WindowItem lastRemoved;
	
	/**
	 * Id of the last item that was removed
	 */
	private String lastRemovedId;

	WindowItemSynchronizer(final ModelLocator<Window, WindowEvent> winLocator, Link link) {
		super("window item", link);
		this.winLocator = winLocator;
		link.setEventHandler("window item changed", new EventHandler<WindowItemEvent>() {
			public void handle(WindowItemEvent event) {
				winLocator.getModelFrom(event).setActiveItem(getItem(event));
			}
		});
	}
	
	@Override
	protected WindowItem createNew(WindowItemNewEvent event) {
		if (event.getItemId().equals(lastRemovedId)) {
			// it's not really a new item, it got moved here from another window
			return lastRemoved;
		} else {
			if ("channel".equals(event.getItemType())) {
				return new Channel(event.getVisibleName());
			} else {
				return new WindowItem(event.getVisibleName());
			}

		}
	}

	@Override
	protected Group<WindowItem> getGroup(WindowItemEvent event) {
		return winLocator.getModelFrom(event).getItems();
	}

	@Override
	protected String getId(WindowItemEvent event) {
		return event.getItemId();
	}
	
	@Override
	protected void added(WindowItemNewEvent event, WindowItem item) {
		if (event.isActive())
			winLocator.getModelFrom(event).setActiveItem(item);
	}
	
	@Override
	protected void removed(WindowItemEvent event, WindowItem item) {
		Window win = winLocator.getModelFrom(event);
		if (item == win.getActiveItem())
			win.setActiveItem(null);
		lastRemoved = item;
		lastRemovedId = event.getItemId();
	}
}
