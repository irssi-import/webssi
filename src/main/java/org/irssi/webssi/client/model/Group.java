package org.irssi.webssi.client.model;

import java.util.Collections;
import java.util.HashMap;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * Sorted collection of items, identifiable by a {@link String} Id.
 * A listener can be added for changes in the list of items.
 */
public class Group<T extends Comparable<T>> {
	private final HashMap<String, T> idToItem = new HashMap<String, T>(); 
	private final TreeSet<T> sortedItems = new TreeSet<T>();
	private Listener<T> listener;
	
	/**
	 * Is an event that changes the ordering of items being processed?
	 */
	private boolean moving = false;
	
	/**
	 * Listener for changes in a {@link Group}
	 */
	public interface Listener<T> {
		void itemAdded(T item, int index);
		void itemRemoved(T item, int index);
		void itemMoved(T item, int oldIndex, int newIndex);
	}
	
	public void setListener(Listener<T> listener) {
		this.listener = listener;
	}
	
	/**
	 * Get the listener. For JUnit tests only.
	 */
	Listener<T> getListener() {
		return listener;
	}
	
	/**
	 * Get a {@link SortedSet} of the items in this {@link Group}.
	 */
	public SortedSet<T> getSortedItems() {
		assert !moving;
		return Collections.unmodifiableSortedSet(sortedItems);
	}
	
	public void addItem(String id, T item) {
		idToItem.put(id, item);
		sortedItems.add(item);
		if (listener != null) {
			int index = indexFor(item);
			listener.itemAdded(item, index);
		}
	}
	
	public void removeItem(String id, T item) {
		if (listener != null) {
			int index = indexFor(item);
			removeItemWithoutEvents(id, item);
			listener.itemRemoved(item, index);
		} else {
			removeItemWithoutEvents(id, item);
		}
	}
	
	private void removeItemWithoutEvents(String id, T item) {
		idToItem.remove(id);
		sortedItems.remove(item);
	}
	
	/**
	 * Called when the id of an item has changed.
	 */
	public void idChanged(T item, String oldId, String newId) {
		assert idToItem.containsKey(oldId);
		assert ! idToItem.containsKey(newId);
		idToItem.remove(oldId);
		idToItem.put(newId, item);
	}
	
	/**
	 * Called before properties of the item that might affect its ordering are changed.
	 */
	public void beforeMove(T item) {
		assert ! moving;
		boolean removeSuccess = sortedItems.remove(item);
		assert removeSuccess;
		moving = true;
	}
	
	/**
	 * Called after properties of the item that might affect its ordering are changed.
	 */
	public void afterMove(T item, int oldIndex) {
		assert moving;
		assert ! sortedItems.contains(item);
		sortedItems.add(item);
		if (listener != null) {
			int newIndex = indexFor(item);
			listener.itemMoved(item, oldIndex, newIndex);
		}
		this.moving = false;
	}
	
	/**
	 * If the item is in the group, returns the index it is at.
	 * Otherwise, returns the index it would get if it got added.
	 */
	public int indexFor(T item) {
		return sortedItems.headSet(item).size();
	}
	
	/**
	 * Returns the item that was added with the given <tt>id</tt>.
	 */
	public T getFromId(String id) {
		return idToItem.get(id);
	}

	/**
	 * For JUnit tests
	 */
	public void clear() {
		idToItem.clear();
		sortedItems.clear();
		listener = null;
	}
}
