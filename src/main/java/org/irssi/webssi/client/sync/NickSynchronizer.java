package org.irssi.webssi.client.sync;

import org.irssi.webssi.client.Link;
import org.irssi.webssi.client.events.EventHandler;
import org.irssi.webssi.client.events.NicklistChangedEvent;
import org.irssi.webssi.client.events.NicklistEvent;
import org.irssi.webssi.client.events.NicklistNewEvent;
import org.irssi.webssi.client.events.WindowItemEvent;
import org.irssi.webssi.client.model.Channel;
import org.irssi.webssi.client.model.Group;
import org.irssi.webssi.client.model.Nick;
import org.irssi.webssi.client.model.WindowItem;

class NickSynchronizer extends Synchronizer<Nick, NicklistEvent, NicklistNewEvent> {
	private final ModelLocator<WindowItem, WindowItemEvent> windowItemLocator;
	
	NickSynchronizer(ModelLocator<WindowItem, WindowItemEvent> windowItemLocator, Link link) {
		super("nicklist", link);
		this.windowItemLocator = windowItemLocator;
		link.addEventHandler("nicklist changed", wrapChangingEventHandler(new IdChangingEventHandler<NicklistChangedEvent>() {
			public String handle(NicklistChangedEvent event) {
				Nick nick = getItem(event);
				nick.setName(event.getNewName());
				return event.getNewName();
			}
		}));
		link.addEventHandler("nicklist clear", new EventHandler<NicklistEvent>() {
			public void handle(NicklistEvent event) {
				getGroup(event).clear();
			}
		});
	}
	
	private Channel getChannel(WindowItemEvent event) {
		return (Channel)windowItemLocator.getModelFrom(event);
	}
	
	@Override
	protected Nick createNew(NicklistNewEvent event) {
		return new Nick(event.getName());
	}

	@Override
	protected Group<Nick> getGroup(NicklistEvent event) {
		return getChannel(event).getNicks();
	}

	@Override
	protected String getId(NicklistEvent event) {
		return event.getName();
	}
}