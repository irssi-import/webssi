package org.irssi.webssi.client.view;

import java.util.HashMap;

import org.irssi.webssi.client.model.Group;
import org.irssi.webssi.client.model.Window;
import org.irssi.webssi.client.model.WindowManager;

import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.TabPanel;

class TabWMView extends Composite implements WindowManager.Listener, Group.Listener<Window> {
	private final TabPanel tabPanel;
	private HashMap<String, WinView> windowViews = new HashMap<String, WinView>();
	
	public TabWMView(WindowManager wm) {
		this.tabPanel = new TabPanel();
		initWidget(tabPanel);
		wm.setListener(this);
		wm.getWindows().setListener(this);
		this.setWidth("100%");
		setStyleName("TabWC");
	}
	
	public WinView getActiveWinView() {
		int selectedTab = tabPanel.getTabBar().getSelectedTab();
		return selectedTab == -1 ? null : (WinView) tabPanel.getWidget(selectedTab);
	}
	
	public void itemAdded(Window win, int index) {
		WinView wv = new WinView(this, win);
		windowViews.put(win.getId(), wv);
		String title = win.getTitle();
		tabPanel.insert(wv, title, index);
	}
	
	private WinView getWindowView(Window window) {
		return windowViews.get(window.getId());
	}
	
	public void itemRemoved(Window win, int index) {
		WinView wv = windowViews.remove(win.getId());
		tabPanel.remove(wv);
	}
	
	void winTitleChanged(WinView winView, String title) {
		tabPanel.getTabBar().setTabText(tabPanel.getWidgetIndex(winView), title);
	}
	
	public void windowChanged(Window win) {
		tabPanel.selectTab(tabPanel.getWidgetIndex(getWindowView(win)));
	}
	
	public void itemMoved(Window win, int oldIndex, int newIndex) {
		WinView wv = getWindowView(win);
		boolean active = getActiveWinView() == wv;
		tabPanel.insert(wv, win.getTitle(), newIndex);
		if (active) {
			tabPanel.selectTab(tabPanel.getWidgetIndex(wv));
		}
	}
}
