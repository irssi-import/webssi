package org.irssi.webssi.client.model;

import org.irssi.webssi.client.expect.ExpectSession;
import org.irssi.webssi.client.expect.Expectable;

/**
 * Test listener for {@link Group}.
 * This one is a bit special because it has a generic parameter.
 * 
 * For every type parameter effectively used, there's a {@link ElementType} instance,
 * with the list of {@link Expectable}s for that type.
 * 
 * @param <T> Type of the items of the group.
 */
public class TestGroupListener<T extends Comparable<T>> extends TestListener implements Group.Listener<T> {
	
	/**
	 * Represents a type that's used as item in a {@link Group}.
	 * This contains just the {@link Expectable}s for the events for a {@link Group.Listener} for that type.
	 */
	public static class ElementType<T> {
		public final Expectable<T> ITEM_ADDED;
		public final Expectable<T> ITEM_REMOVED;
		public final Expectable<T> ITEM_MOVED;
		
		private ElementType(String name) {
			ITEM_ADDED = declareExpectable("itemAdded[" + name + "]");
			ITEM_REMOVED = declareExpectable("itemRemoved[" + name + "]");
			ITEM_MOVED = declareExpectable("itemMoved[" + name + "]");
		}
	}
	
	/**
	 * Container of {@link Expectable}s for a {@link Group}&lt;{@link Window}&gt;.
	 */
	public static final ElementType<Window> WIN = new ElementType<Window>("win"); 
	
	private final ElementType<T> type;
	
	/**
	 * Add a test listener on the given {@link Group} if there isn't already one on it.
	 */
	public static <T extends Comparable<T>> void listen(ExpectSession session, ElementType<T> type, Group<T> group) {
		group.addListener(new TestGroupListener<T>(session, type));
	}
	
	private TestGroupListener(ExpectSession session, ElementType<T> type) {
		super(session);
		this.type = type;
	}
	
	public void itemAdded(T item, int index) {
		called(type.ITEM_ADDED, item);
	}

	public void itemRemoved(T item, int index) {
		called(type.ITEM_REMOVED, item);
	}
	
	public void itemMoved(T item, int oldIndex, int newIndex) {
		called(type.ITEM_MOVED, item);
	}
}
