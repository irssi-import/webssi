package org.irssi.webssi.client.sync;

import org.irssi.webssi.client.Link;
import org.irssi.webssi.client.events.ServerEvent;
import org.irssi.webssi.client.events.ServerNewEvent;
import org.irssi.webssi.client.model.Group;
import org.irssi.webssi.client.model.Model;
import org.irssi.webssi.client.model.Server;

class ServerSynchronizer extends Synchronizer<Server, ServerEvent, ServerNewEvent> {
	private final Model model;
	
	public ServerSynchronizer(Model model, Link link) {
		super("server", link);
		this.model = model;
	}

	@Override
	protected Server createNew(ServerNewEvent event) {
		return new Server(event.getTag());
	}

	@Override
	protected Group<Server> getGroup(ServerEvent event) {
		return model.getServers();
	}

	@Override
	protected String getId(ServerEvent event) {
		return event.getTag();
	}

}
