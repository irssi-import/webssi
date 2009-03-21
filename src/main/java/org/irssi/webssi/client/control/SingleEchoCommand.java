package org.irssi.webssi.client.control;

import org.irssi.webssi.client.events.JsonEvent;
import org.irssi.webssi.client.sync.ModelLocator;

import com.google.gwt.core.client.JavaScriptObject;

/**
 * Command that recognizes echo for one type of event, and uses a ModelLocator
 * when determining if an event is echo or not.
 * By default, any unexpected event of the same type triggers a replay.
 * 
 * @param <T> Type of model the echo would apply to
 * @param <E> Type of event that would be echo
 */
abstract class SingleEchoCommand<T, E extends JsonEvent> extends Command {
	private final String echoEventType;
	private final ModelLocator<T,E> locator;
	
	SingleEchoCommand(JavaScriptObject js, String echoEventType, ModelLocator<T, E> locator) {
		super(js);
		this.echoEventType = echoEventType;
		this.locator = locator;
	}
	
	@Override
	final boolean echo(JsonEvent event) {
		if (! echoEventType.equals(event.getType()))
			return false;
		E castedEvent = event.<E>cast();
		T item = locator.getModelFrom(castedEvent);
		return SingleEchoCommand.this.echo(item, castedEvent);
	}
	
	abstract boolean echo(T item, E event);
	
	@Override
	boolean needReplayAfter(JsonEvent event) {
		return (event.getType().equals(echoEventType));
	}
	
	@Override
	boolean needReplayAfterMissingEcho(Command missingEchoCommand) {
		return missingEchoCommand.getClass() == this.getClass();
	}
}
