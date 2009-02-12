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
		if (item == this)
			return 0;
		int visibleNameComparison = visibleName.toLowerCase().compareTo(item.visibleName.toLowerCase());
		if (visibleNameComparison != 0)
			return visibleNameComparison;
		return this.hashCode() < item.hashCode() ? -1 : 1; // random consistent order
	}
}
