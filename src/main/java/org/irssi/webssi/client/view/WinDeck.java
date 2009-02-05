package org.irssi.webssi.client.view;

import java.util.HashMap;

import org.irssi.webssi.client.model.Group;
import org.irssi.webssi.client.model.Window;
import org.irssi.webssi.client.model.WindowManager;

import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.DeckPanel;

/**
 * {@link DeckPanel} that contains the windows.
 */
class WinDeck extends Composite implements WindowManager.Listener, Group.Listener<Window> {
	private final HashMap<String, WinView> windowViews = new HashMap<String, WinView>();
	private final DeckPanel deck = new DeckPanel();
	private final WindowManager wm;
	
	WinDeck(WindowManager wm) {
		this.wm = wm;
		initWidget(deck);
		wm.addListener(this);
		wm.getWindows().addListener(this);
	}

	public void windowChanged(Window win) {
		deck.showWidget(wm.getWindows().indexFor(win));
	}

	public void itemAdded(Window win, int index) {
		WinView wv = new WinView(win);
		wv.setStyleName("win");
		windowViews.put(win.getId(), wv);
		deck.insert(wv, index);
	}
	
	private WinView getWindowView(Window window) {
		return windowViews.get(window.getId());
	}

	public void itemMoved(Window win, int oldIndex, int newIndex) {
		WinView wv = getWindowView(win);
		boolean active = wm.getActiveWindow() == win;
		deck.remove(oldIndex);
		deck.insert(wv, newIndex);
		if (active) {
			deck.showWidget(newIndex);
		}
	}

	public void itemRemoved(Window win, int index) {
		WinView wv = windowViews.remove(win.getId());
		deck.remove(wv);
	}
}
