package org.irssi.webssi.client.model;

/**
 * Model of a window
 */
public class Window implements Comparable<Window> {
	public static interface Listener {
		void textPrinted(String text);
		void nameChanged(String name);
		void windowItemChanged(WindowItem item);
	}
	
	/**
	 * Listener delegating to two other listeners
	 */
	private static class CompositeListener implements Listener {
		private final Listener listener1;
		private final Listener listener2;

		private CompositeListener(Listener listener1, Listener listener2) {
			this.listener1 = listener1;
			this.listener2 = listener2;
		}

		public void nameChanged(String name) {
			listener1.nameChanged(name);
			listener2.nameChanged(name);
		}

		public void textPrinted(String text) {
			listener1.textPrinted(text);
			listener2.textPrinted(text);
		}

		public void windowItemChanged(WindowItem item) {
			listener1.windowItemChanged(item);
			listener2.windowItemChanged(item);
		}
	}
	
	private final String id;
	private String name;
	private int refnum;
	private final Group<WindowItem> items = new Group<WindowItem>();
	private WindowItem activeItem;
	private Window.Listener listener;
	private final Activity activity;
	
	public Window(String id, String name, int refnum) {
		this.id = id;
		this.name = name;
		this.refnum = refnum;
		this.activity = new Activity();
	}

	public void printText(String text) {
		if (listener != null)
			listener.textPrinted(text);
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
		listener.nameChanged(name);
	}

	public String getId() {
		return id;
	}

	public void addListener(Window.Listener listener) {
		if (this.listener == null)
			this.listener = listener;
		else
			this.listener = new CompositeListener(this.listener, listener);
	}
	
	public Group<WindowItem> getItems() {
		return items;
	}

	public WindowItem getActiveItem() {
		return activeItem;
	}
	
	public void setActiveItem(WindowItem item) {
//		assert item == null || items.contains(item);
		if (item != activeItem) {
			activeItem = item;
			listener.windowItemChanged(item);
		}
	}
	
	public String getTitle() {
		if (name != null && name.length() != 0) {
			return name;
		} else if (activeItem != null) {
			return activeItem.getVisibleName();
		} else {
			return "";
		}
	}
	
	public int getRefnum() {
		return refnum;
	}
	
	public void setRefnum(int refnum) {
		this.refnum = refnum;
	}
	
	public Activity getActivity() {
		return activity;
	}
	
	public int compareTo(Window o) {
		if (this == o)
			return 0;
		else if (this.refnum != o.refnum)
			return this.refnum < o.refnum ? -1 : 1;
		else // refnums of two different windows can temporarily be the same when windows are moving
			return this.hashCode() < o.hashCode() ? -1 : 1; // it doesn't matter which comes first, as long as it is consistent
	}
}
