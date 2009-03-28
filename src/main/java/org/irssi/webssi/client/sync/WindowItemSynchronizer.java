package org.irssi.webssi.client.sync;

import org.irssi.webssi.client.Link;
import org.irssi.webssi.client.events.EventHandler;
import org.irssi.webssi.client.events.ServerEvent;
import org.irssi.webssi.client.events.WindowEvent;
import org.irssi.webssi.client.events.WindowItemActivityEvent;
import org.irssi.webssi.client.events.WindowItemEvent;
import org.irssi.webssi.client.events.WindowItemMovedEvent;
import org.irssi.webssi.client.events.WindowItemNewEvent;
import org.irssi.webssi.client.model.Channel;
import org.irssi.webssi.client.model.DataLevel;
import org.irssi.webssi.client.model.Group;
import org.irssi.webssi.client.model.Server;
import org.irssi.webssi.client.model.Window;
import org.irssi.webssi.client.model.WindowItem;

class WindowItemSynchronizer extends Synchronizer<WindowItem, WindowItemEvent, WindowItemNewEvent> {
	private final ModelLocator<Window, WindowEvent> winLocator;
	private final ModelLocator<Server, ServerEvent> serverLocator;
	
	WindowItemSynchronizer(final ModelLocator<Window, WindowEvent> winLocator, ModelLocator<Server, ServerEvent> serverLocator, Link link) {
		super("window item", link);
		this.winLocator = winLocator;
		this.serverLocator = serverLocator;
		link.addEventHandler("window item moved", new EventHandler<WindowItemMovedEvent>() {
			public void handle(WindowItemMovedEvent event) {
				String id = getId(event);
				WindowItem item = getModelFrom(event);
				Window oldWindow = winLocator.getModelFrom(event);
				assert oldWindow == item.getWin();
				Window newWindow = winLocator.getModelFrom(event.getNewWindowEvent());
				oldWindow.getItems().removeItem(id, item);
				removed(event, item);
				item.setWin(newWindow);
				newWindow.getItems().addItem(id, item);
				newWindow.setActiveItem(item);
			}
		});
		link.addEventHandler("window item changed", new EventHandler<WindowItemEvent>() {
			public void handle(WindowItemEvent event) {
				winLocator.getModelFrom(event).setActiveItem(getItem(event));
			}
		});
		link.addEventHandler("window item activity", new EventHandler<WindowItemActivityEvent>() {
			public void handle(WindowItemActivityEvent event) {
				getItem(event).getActivity().activity(DataLevel.fromInt(event.getDataLevel()), event.getHilightColor());
			}
		});
	}
	
	@Override
	protected WindowItem createNew(WindowItemNewEvent event) {
		Server server = serverLocator.getModelFrom(event.<ServerEvent>cast());
		Window win = winLocator.getModelFrom(event);
		WindowItem result;
		if ("channel".equals(event.getItemType())) {
			result = new Channel(event.getVisibleName(), server, win, getId(event));
		} else {
			result = new WindowItem(event.getVisibleName(), server, win, getId(event));
		}
		result.getActivity().activity(DataLevel.fromInt(event.getDataLevel()), event.getHilightColor());
		return result;
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
	}
}
