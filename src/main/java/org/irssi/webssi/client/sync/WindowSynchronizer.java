package org.irssi.webssi.client.sync;

import org.irssi.webssi.client.Link;
import org.irssi.webssi.client.events.EventHandler;
import org.irssi.webssi.client.events.TextEvent;
import org.irssi.webssi.client.events.WindowActivityEvent;
import org.irssi.webssi.client.events.WindowCreatedEvent;
import org.irssi.webssi.client.events.WindowEvent;
import org.irssi.webssi.client.events.WindowNameChangedEvent;
import org.irssi.webssi.client.events.WindowRefnumChangedEvent;
import org.irssi.webssi.client.model.DataLevel;
import org.irssi.webssi.client.model.Group;
import org.irssi.webssi.client.model.Model;
import org.irssi.webssi.client.model.Window;

class WindowSynchronizer extends Synchronizer<Window, WindowEvent, WindowCreatedEvent> {
	private final Model model;
	
	WindowSynchronizer(final Model model, Link link) {
		super("window", link);
		this.model = model;
		link.addEventHandler("window changed", new EventHandler<WindowEvent>() {
			public void handle(WindowEvent event) {
				model.getWm().setActiveWindow(getModelFrom(event));
			}
		});
		// TODO it's not id changing(?), but it is order changing
		link.addEventHandler("window refnum changed", wrapChangingEventHandler(new IdChangingEventHandler<WindowRefnumChangedEvent>() {
			public String handle(WindowRefnumChangedEvent event) {
				getModelFrom(event).setRefnum(event.getRefnum());
				return event.getWinId();
			}
		}));
		link.addEventHandler("window name changed", new EventHandler<WindowNameChangedEvent>() {
			public void handle(WindowNameChangedEvent event) {
				getModelFrom(event).setName(event.getName());
			}
		});
		link.addEventHandler("T", new EventHandler<TextEvent>() {
			public void handle(TextEvent event) {
				getModelFrom(event).printText(event.getText());
			}
		});
		link.addEventHandler("window activity", new EventHandler<WindowActivityEvent>() {
			public void handle(WindowActivityEvent event) {
				getItem(event).getActivity().activity(DataLevel.fromInt(event.getDataLevel()), event.getHilightColor());
			}
		});
	}
	
	@Override
	protected Window createNew(WindowCreatedEvent event) {
		Window result = new Window(event.getWinId(), event.getName(), event.getRefnum());
		result.getActivity().activity(DataLevel.fromInt(event.getDataLevel()), event.getHilightColor());
		return result;
	}

	@Override
	protected Group<Window> getGroup(WindowEvent event) {
		return model.getWm().getWindows();
	}

	@Override
	protected String getId(WindowEvent event) {
		return event.getWinId();
	}
}
