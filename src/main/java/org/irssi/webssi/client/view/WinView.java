package org.irssi.webssi.client.view;

import org.irssi.webssi.client.model.Window;
import org.irssi.webssi.client.model.WindowItem;

import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.ScrollPanel;

//TODO use HorizontalSplitPanel to split buffer and nicklist
class WinView extends Composite implements Window.Listener {
	private final Window win;
//	private final HorizontalSplitPanel panel;
	private final HorizontalPanel panel;
	private final ScrollPanel bufferViewScroller;
	private final BufferView bufferView;
	private final NicklistView nicklistView;
	
	WinView(Window win) {
		super();
		this.win = win;
		win.addListener(this);
//		this.panel = new HorizontalSplitPanel();
		this.panel = new HorizontalPanel();
		initWidget(panel);
		
		this.bufferView = new BufferView();
		bufferView.setStylePrimaryName("buffer");
		bufferViewScroller = new ScrollPanel(bufferView);
		bufferViewScroller.setStylePrimaryName("bufferScroller");
		
		DOM.setStyleAttribute(bufferViewScroller.getElement(), "position", "absolute");
		
//		panel.setLeftWidget(bufferViewScroller);
		panel.add(bufferViewScroller);
		
		this.nicklistView = new NicklistView();
		nicklistView.setStylePrimaryName("nicklist");
		ScrollPanel nicklistViewScroller = new ScrollPanel(nicklistView);
		nicklistViewScroller.setStylePrimaryName("nicklistScroller");
		
		DOM.setStyleAttribute(nicklistViewScroller.getElement(), "position", "absolute");
		
//		panel.setRightWidget(nicklistViewScroller);
		panel.add(nicklistViewScroller);
		
		panel.setCellWidth(bufferViewScroller, "90%");
		panel.setCellHeight(bufferViewScroller, "100%");
		panel.setCellWidth(nicklistViewScroller, "10%");
		panel.setCellHeight(nicklistViewScroller, "100%");
	}

	public Window getWin() {
		return win;
	}
	
	public void textPrinted(String text) {
		int bufferHeight = bufferView.getOffsetHeight();
		int scrollerHeight = bufferViewScroller.getOffsetHeight();
		int position = bufferViewScroller.getScrollPosition();
		int bottom = bufferHeight - position;
		boolean atBottom = (bottom <= scrollerHeight);
			
		bufferView.textPrinted(text);
		
		if (atBottom)
			bufferViewScroller.scrollToBottom();
	}

	public void windowItemChanged(WindowItem item) {
		nicklistView.setItem(item);
	}
	
	public void nameChanged(String name) {
		// do nothing
	}

	/**
	 * Called when this view is activated in the deck.
	 */
	void activated() {
		// browsers seem to forget scroll position of invisible ScrollPanels,
		// so scroll all the way down when we get selected
		bufferViewScroller.scrollToBottom();
	}
}
