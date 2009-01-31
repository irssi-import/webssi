package org.irssi.webssi.client.sync;

import org.irssi.webssi.client.Link;
import org.irssi.webssi.client.events.EventHandler;
import org.irssi.webssi.client.events.JsonEvent;
import org.irssi.webssi.client.model.Group;

/**
 * Synchronizes model on the client side with the irssi model.
 * 
 * @param <T> Type of items in the group
 * @param <N> Type of event that creates new items
 * @param <E> Type of event referring to an existing items
 */
abstract class Synchronizer<T extends Comparable<T>, E extends JsonEvent, N extends E> implements ModelLocator<T, E>{

	/**
	 * Creates a synchronizer, registers event handlers for <tt>new</tt> and <tt>remove</tt> events on the link. 
	 * @param eventPrefix Start of the name of <tt>new</tt> and <tt>remove</tt> events
	 * @param link The link to add event handlers on.
	 */
	Synchronizer(String eventPrefix, Link link) {
		link.setEventHandler(eventPrefix + " new", new EventHandler<N>() {
			public void handle(N event) {
				T item = createNew(event.<N>cast());
				Group<T> group = getGroup(event);
				group.addItem(getId(event), item);
				added(event, item);
			};
		});
		
		link.setEventHandler(eventPrefix + " remove", new EventHandler<E>() {
			public void handle(E event) {
				T item = getItem(event);
				Group<T> group = getGroup(event);
				group.removeItem(getId(event), item);
				removed(event, item);
			};
		});
	}
	
	/**
	 * Wrap the event handler so that changing the id and the place in the group of the affected item is
	 * dealt with.
	 * Note: while the handler is being called, the Group is in a slightly inconsistent state:
	 *  getSortedItems() will not contain the item being moved.
	 *  Therefore, it is not allowed to add or remove items in the same group inside the handler
	 *  (because that would lead to listeners being called with a wrong index).
	 *  
	 * @param <X> Type of the {@link JsonEvent}
	 * @param handler Handler to wrap
	 * @return The given handler, but wrapped.
	 */
	final <X extends E> EventHandler<X> wrapChangingEventHandler(IdChangingEventHandler<X> handler) {
		return new ChangingEventHandlerWrapper<X>(handler);
	}
	
	private class ChangingEventHandlerWrapper<X extends E> implements EventHandler<X> {
		private final IdChangingEventHandler<X> handler;
		public ChangingEventHandlerWrapper(IdChangingEventHandler<X> handler) {
			this.handler = handler;
		}
		
		public void handle(X event) {
			T item = getItem(event);
			Group<T> group = getGroup(event);
			int oldIndex = group.indexFor(item);
			group.beforeMove(item);
			
			String oldId = getId(event);
			
			String newId = handler.handle(event);
			
			if (! oldId.equals(newId)) {
				group.idChanged(item, oldId, newId);
			}
			
			group.afterMove(item, oldIndex);
		}
	}
	
	interface IdChangingEventHandler<T extends JsonEvent> {
		/**
		 * Handles the event, and returns the new id.
		 */
		String handle(T event);
	}
	
	protected abstract T createNew(N event);
	
	final T getItem(E event) {
		return getGroup(event).getFromId(getId(event));
	}
	
	final public T getModelFrom(E event) {
		return getGroup(event).getFromId(getId(event));
	}	
	
	protected abstract String getId(E event);
	
	protected abstract Group<T> getGroup(E event);
	
	/**
	 * Called right after an item has been added to the group
	 */
	protected void added(N event, T item) {
	}
	
	/**
	 * Called right after an item has been removed from the group
	 */
	protected void removed(E event, T item) {
	}
}