package org.irssi.webssi.client.view;

import org.irssi.webssi.client.control.Controller;
import org.irssi.webssi.client.model.Model;

import com.google.gwt.user.client.ui.DockPanel;
import com.google.gwt.user.client.ui.RootPanel;

public class View {
	private final RootPanel rootPanel = RootPanel.get();
	
	private final EntryView entryView;
	private final DebugView debugView = new DebugView();
	private final WinDeck wmv;
	private final ItemTreeView itemTreeView;
	private final WinTabBar winTabBar;
	
//	private Controller controller;
	
	public View(final Model model) {
		wmv = new WinDeck(model.getWm());
		wmv.setStylePrimaryName("winDeck");
		
		winTabBar = new WinTabBar(model.getWm());
		itemTreeView = new ItemTreeView(model.getServers(), model.getWm());
		entryView = new EntryView(model);
		entryView.setStylePrimaryName("entryBox");
		
		DockPanel dock = new DockPanel();
		dock.setStylePrimaryName("rootDock");
		
		dock.add(winTabBar, DockPanel.NORTH);
		dock.add(entryView, DockPanel.SOUTH);
		dock.add(wmv, DockPanel.CENTER);

		dock.setCellHeight(winTabBar, "20px");
		dock.setCellHeight(entryView, "30px");
		dock.setCellHeight(wmv, "100%");
		
		DockPanel outerDock = new DockPanel();
		outerDock.setStylePrimaryName("rootDock");
		outerDock.add(itemTreeView, DockPanel.WEST);
		outerDock.add(dock, DockPanel.CENTER);
		outerDock.setCellWidth(itemTreeView, "100px");
		
		rootPanel.add(outerDock);
		rootPanel.setStylePrimaryName("root");
	}
	
	public void debug(String sender, String message) {
		debugView.debug(sender, message);
	}

	public void setController(Controller controller) {
//		this.controller = controller;
		itemTreeView.setController(controller);
		winTabBar.setController(controller);
		entryView.setController(controller);
	}
}
