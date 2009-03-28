package org.irssi.webssi.client.view;

import org.irssi.webssi.client.control.Controller;
import org.irssi.webssi.client.model.Activity;
import org.irssi.webssi.client.model.Group;
import org.irssi.webssi.client.model.Window;
import org.irssi.webssi.client.model.WindowItem;
import org.irssi.webssi.client.model.WindowManager;

import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.SourcesTabEvents;
import com.google.gwt.user.client.ui.TabBar;
import com.google.gwt.user.client.ui.TabListener;
import com.google.gwt.user.client.ui.Widget;

/**
 * TabBar to switch between windows.
 * This only displays the tabs, the content is shown by {@link WinDeck}.
 */
public class WinTabBar extends Composite implements WindowManager.Listener, Group.Listener<Window>, TabListener {
	
	/**
	 * The content (title) of one tab
	 */
	private static class TabWidget extends Label implements Window.Listener, Activity.Listener {
		private final Window window;

		public TabWidget(Window window) {
			super();
			this.window = window;
			setWordWrap(false);
			refreshTitle();
			refreshStyle();
			window.addListener(this);
			window.getActivity().addListener(this);
		}
		
		private void refreshTitle() {
			setText(window.getTitle());
		}
		
		private void refreshStyle() {
			this.setStyleName(ColorUtil.styleForActivity(window.getActivity()));
		}
		
		public void nameChanged(String name) {
			refreshTitle();
		}

		public void windowItemChanged(WindowItem item) {
			refreshTitle();
		}
		
		public void textPrinted(String text) {
			// do nothing
		}
		
		public void activity() {
			refreshStyle();
		}
	}
	
	/**
	 * TabBar that supports moving of tabs.
	 */
	private static class MoveableTabBar extends TabBar {
		/**
		 * Move tab from fromIndex to toIndex.
		 */
		private void moveTab(int fromIndex, int toIndex) {
			HorizontalPanel panel = (HorizontalPanel) getWidget();
			Widget widget = panel.getWidget(fromIndex + 1);
			panel.remove(fromIndex + 1);
			panel.insert(widget, toIndex + 1);
		}
	}
	
	private final MoveableTabBar tabBar;
	private final WindowManager wm;
	private Controller controller;
	
	public WinTabBar(WindowManager wm) {
		this.wm = wm;
		tabBar = new MoveableTabBar();
		initWidget(tabBar);
		wm.addListener(this);
		wm.getWindows().addListener(this);
		tabBar.addTabListener(this);
	}

	public void windowChanged(Window win) {
		tabBar.selectTab(wm.getWindows().indexFor(win));
	}

	public void itemAdded(Window win, int index) {
		tabBar.insertTab(new TabWidget(win), index);
	}

	public void itemMoved(Window win, int oldIndex, int newIndex) {
		tabBar.moveTab(oldIndex, newIndex);
	}

	public void itemRemoved(Window item, int index) {
		tabBar.removeTab(index);
	}

	public boolean onBeforeTabSelected(SourcesTabEvents sender, int tabIndex) {
		// do nothing
		return true;
	}

	public void onTabSelected(SourcesTabEvents sender, int tabIndex) {
		controller.activateWindow(getWindowForTab(tabIndex));
	}
	
	private Window getWindowForTab(int tabIndex) {
		assert tabIndex >= 0 && tabIndex < wm.getWindows().getSortedItems().size();
		int i = 0;
		for (Window window : wm.getWindows().getSortedItems()) {
			if (i++ == tabIndex)
				return window;
		}
		throw new AssertionError("No window for tab " + tabIndex);
	}
	
	public void setController(Controller controller) {
		this.controller = controller;
	}
}
