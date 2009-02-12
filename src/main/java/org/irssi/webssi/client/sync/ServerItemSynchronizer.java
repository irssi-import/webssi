package org.irssi.webssi.client.sync;

import org.irssi.webssi.client.Link;
import org.irssi.webssi.client.events.ServerEvent;
import org.irssi.webssi.client.events.WindowItemEvent;
import org.irssi.webssi.client.events.WindowItemNewEvent;
import org.irssi.webssi.client.model.Group;
import org.irssi.webssi.client.model.Server;
import org.irssi.webssi.client.model.WindowItem;

/**
 * Synchronized the model of {@link WindowItem}s per {@link Server}.
 */
public class ServerItemSynchronizer extends Synchronizer<WindowItem, WindowItemEvent, WindowItemNewEvent> {
	private final ModelLocator<WindowItem, WindowItemEvent> winItemLocator;
	private final ModelLocator<Server, ServerEvent> serverLocator;
	private final Group<WindowItem> noServerItems;
	
	public ServerItemSynchronizer(Group<WindowItem> noServerItems, ModelLocator<Server, ServerEvent> serverLocator,
			ModelLocator<WindowItem, WindowItemEvent> winItemLocator, Link link) {
		super("window item", link);
		this.noServerItems = noServerItems;
		this.winItemLocator = winItemLocator;
		this.serverLocator = serverLocator;
	}

	@Override
	protected WindowItem createNew(WindowItemNewEvent event) {
		return winItemLocator.getModelFrom(event);
	}

	@Override
	protected Group<WindowItem> getGroup(WindowItemEvent event) {
		if (event.getTag() == null) {
			return noServerItems;
		} else {
			Server server = serverLocator.getModelFrom(event.<ServerEvent>cast());
			return server.getItems();
		}
	}

	@Override
	protected String getId(WindowItemEvent event) {
		return event.getItemId();
	}
}
