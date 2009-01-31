package org.irssi.webssi.client.view;

import org.irssi.webssi.client.Controller;
import org.irssi.webssi.client.model.Model;

import com.google.gwt.user.client.ui.KeyboardListener;
import com.google.gwt.user.client.ui.RootPanel;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.Widget;

public class View {
	private final RootPanel rootPanel = RootPanel.get();
	
	private final TextBox entryBox = new TextBox();
	private final DebugView debugView = new DebugView();
	private final TabWMView wmv;
	
	private Controller controller;
	
	public View(Model model) {
		wmv = new TabWMView(model.getWm());
		wmv.setWidth("100%");
		
		rootPanel.add(debugView);
		rootPanel.add(wmv);
		rootPanel.add(entryBox);
		entryBox.setStyleName("entryBox");
		entryBox.addKeyboardListener(new KeyboardListener() {
			public void onKeyDown(Widget sender, char keyCode, int modifiers) {
			}

			public void onKeyPress(Widget sender, char keyCode, int modifiers) {
			}

			public void onKeyUp(Widget sender, char keyCode, int modifiers) {
				if (keyCode == KeyboardListener.KEY_ENTER) {
					final String message = entryBox.getText();
					if (message.trim().length() > 0) {
						controller.sendLine(wmv.getActiveWinView().getWin(), message);
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
	}
}
