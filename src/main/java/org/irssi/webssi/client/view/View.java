package org.irssi.webssi.client.view;

import org.irssi.webssi.client.Controller;
import org.irssi.webssi.client.model.Model;

import com.google.gwt.user.client.ui.DockPanel;
import com.google.gwt.user.client.ui.KeyboardListener;
import com.google.gwt.user.client.ui.RootPanel;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.Widget;

public class View {
	private final RootPanel rootPanel = RootPanel.get();
	
	private final TextBox entryBox = new TextBox();
	private final DebugView debugView = new DebugView();
	private final WinDeck wmv;
	private final ItemTreeView itemTreeView;
	private final WinTabBar winTabBar;
	
	private Controller controller;
	
	public View(final Model model) {
		wmv = new WinDeck(model.getWm());
		wmv.setStylePrimaryName("winDeck");
		
		winTabBar = new WinTabBar(model.getWm());
		
		itemTreeView = new ItemTreeView(model.getServers(), model.getWm());
		
		DockPanel dock = new DockPanel();
		dock.setStylePrimaryName("rootDock");
		
		dock.add(itemTreeView, DockPanel.WEST);
		dock.add(winTabBar, DockPanel.NORTH);
		dock.add(wmv, DockPanel.CENTER);
		dock.add(entryBox, DockPanel.SOUTH);

		dock.setCellWidth(itemTreeView, "100px");
		dock.setCellHeight(winTabBar, "20px");
		dock.setCellHeight(entryBox, "30px");
		
		rootPanel.add(dock);
		rootPanel.setStylePrimaryName("root");
		
		entryBox.setStylePrimaryName("entryBox");
		entryBox.addKeyboardListener(new KeyboardListener() {
			public void onKeyDown(Widget sender, char keyCode, int modifiers) {
			}

			public void onKeyPress(Widget sender, char keyCode, int modifiers) {
			}

			public void onKeyUp(Widget sender, char keyCode, int modifiers) {
				if (keyCode == KeyboardListener.KEY_ENTER) {
					final String message = entryBox.getText();
					if (message.trim().length() > 0) {
						controller.sendLine(model.getWm().getActiveWindow(), message);
					}
					entryBox.setText("");
				}
			}
		});
	}
	
	public void debug(String sender, String message) {
		debugView.debug(sender, message);
	}

	public void setController(Controller controller) {
		this.controller = controller;
		itemTreeView.setController(controller);
		winTabBar.setController(controller);
	}
}
