package org.irssi.webssi.client.model;

/**
 * Model of a window item
 */
public class WindowItem implements Comparable<WindowItem> {
	private String visibleName;
	
	public WindowItem(String visibleName) {
		this.visibleName = visibleName;
	}
	
	public String getVisibleName() {
		return visibleName;
	}
	
	public int compareTo(WindowItem item) {
		return visibleName.compareTo(item.visibleName);
	}
}
