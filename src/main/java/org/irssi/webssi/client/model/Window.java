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
	
	private final String id;
	private String name;
	private int refnum;
	private final Group<WindowItem> items = new Group<WindowItem>();
	private WindowItem activeItem;
	private Window.Listener listener;
	
	public Window(String id, String name, int refnum) {
		this.id = id;
		this.name = name;
		this.refnum = refnum;
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

	public void setListener(Window.Listener listener) {
		this.listener = listener;
	}
	
	Window.Listener getListener() {
		return listener;
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
	
	public int compareTo(Window o) {
		return this == o ? 0 : this.refnum < o.refnum ? -1 : 1;
	}
}
