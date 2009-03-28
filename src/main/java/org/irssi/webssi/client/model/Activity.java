package org.irssi.webssi.client.model;

/**
 * Activity of a window or window item.
 * Consists of a {@link DataLevel} and hilight color.
 */
public class Activity {
	public static interface Listener {
		void activity();
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
		
		public void activity() {
			listener1.activity();
			listener2.activity();
		}
	}
	
	private DataLevel dataLevel;
	private String hilightColor;
	private Listener listener;
	
	public void addListener(Listener listener) {
		if (this.listener == null)
			this.listener = listener;
		else
			this.listener = new CompositeListener(this.listener, listener);
	}
	
	public DataLevel getDataLevel() {
		return dataLevel;
	}

	public String getHilightColor() {
		return hilightColor;
	}
	
	public void activity(DataLevel dataLevel, String hilightColor) {
		if (dataLevel != this.dataLevel || ! eq(hilightColor, this.hilightColor)) {
			this.dataLevel = dataLevel;
			this.hilightColor = hilightColor;
			if (listener != null) {
				listener.activity();
			}
		}
	}
	
	private static boolean eq(Object a, Object b) {
		return a == null ? b == null : a.equals(b);
	}
}
