package org.irssi.webssi.client.sync;

import org.irssi.webssi.client.Link;
import org.irssi.webssi.client.events.EntryChangedEvent;
import org.irssi.webssi.client.events.EventHandler;
import org.irssi.webssi.client.model.Entry;

class EntrySynchronizer {
	
	public EntrySynchronizer(final Entry entry, Link link) {
		link.addEventHandler("entry changed", new EventHandler<EntryChangedEvent>() {
			public void handle(EntryChangedEvent event) {
				entry.setContent(event.getContent(), event.getCursorPos());
			};
		});
	}
}
