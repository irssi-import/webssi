package org.irssi.webssi.client.model;

/**
 * Model of a nicklist entry.
 */
public class Nick implements Comparable<Nick> {
	private String name;
	private Listener listener;
	
	public interface Listener {
		void nameChanged();
	}
	
	public Nick(String name) {
		this.name = name;
	}
	
	public void setListener(Listener listener) {
		this.listener = listener;
	}

	public String getName() {
		return name;
	}
	
	public void setName(String name) {
		this.name = name;
		if (listener != null) {
			listener.nameChanged();
		}
	}

	public int compareTo(Nick other) {
		return this.name.compareTo(other.name);
	}
}
