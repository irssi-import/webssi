package org.irssi.webssi.client.sync;

import org.irssi.webssi.client.events.JsonEvent;
import org.irssi.webssi.client.sync.Synchronizer;

/**
 * Finds the model object an event applies to.
 * This is the interface {@link Synchronizer}s expose to each other.
 *
 * @param <T> Type of the model object.
 * @param <E> Type of the event.
 */
interface ModelLocator<T, E extends JsonEvent> {
	T getModelFrom(E event);
}
