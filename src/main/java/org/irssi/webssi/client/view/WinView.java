package org.irssi.webssi.client.view;

import org.irssi.webssi.client.model.Window;
import org.irssi.webssi.client.model.WindowItem;

import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.HorizontalPanel;

//TODO use HorizontalSplitPanel to split buffer and nicklist
class WinView extends Composite implements Window.Listener {
	private final TabWMView parent;
	private final Window win;
//	private final HorizontalSplitPanel panel;
	private final HorizontalPanel panel;
	private final BufferView bufferView;
	private final NicklistView nicklistView;
	
	WinView(TabWMView parent, Window win) {
		super();
		this.parent = parent;
		this.win = win;
		win.setListener(this);
//		this.panel = new HorizontalSplitPanel();
		this.panel = new HorizontalPanel();
		initWidget(panel);
		this.setSize("100%", "100%");
		
		this.bufferView = new BufferView();
//		panel.setLeftWidget(bufferView);
		panel.add(bufferView);
//		bufferView.setHeight("100%");
		bufferView.setSize("100%", "100%");
		
		this.nicklistView = new NicklistView();
//		panel.setRightWidget(nicklistView);
		panel.add(nicklistView);
		nicklistView.setSize("100px", "100%");
		
		panel.setCellWidth(bufferView, "90%");
		panel.setCellHeight(bufferView, "100%");
		panel.setCellWidth(nicklistView, "10%");
		panel.setCellHeight(nicklistView, "100%");
	}

	public void textPrinted(String text) {
		bufferView.textPrinted(text);
	}
	
	public Window getWin() {
		return win;
	}

	public void windowItemChanged(WindowItem item) {
		parent.winTitleChanged(this, win.getTitle());
		nicklistView.setItem(item);
	}
	
	public void nameChanged(String name) {
		parent.winTitleChanged(this, win.getTitle());
	}
}
