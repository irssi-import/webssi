package org.irssi.webssi.client.events;

/**
 * Handler for {@link JsonEvent}s.
 *
 * @param <T> Type of the events it handles.
 */
public interface EventHandler<T extends JsonEvent> {
	void handle(T event);
}
