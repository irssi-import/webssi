package org.irssi.webssi.client.view;

import org.irssi.webssi.client.control.Controller;
import org.irssi.webssi.client.model.Model;

import com.google.gwt.core.client.GWT;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiFactory;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.RootLayoutPanel;
import com.google.gwt.user.client.ui.Widget;

public class View {
	interface MyUiBinder extends UiBinder<Widget, View> {}
	private static MyUiBinder uiBinder = GWT.create(MyUiBinder.class);
	
	private final RootLayoutPanel rootPanel = RootLayoutPanel.get();
	
	@UiField EntryView entryView;
	@UiField WinDeck wmv;
	@UiField ItemTreeView itemTreeView;
	@UiField WinTabBar winTabBar;
	private final Model model;
	
//	private Controller controller;
	
	public View(final Model model) {
		this.model = model;
		
		rootPanel.add(uiBinder.createAndBindUi(this));
		rootPanel.setStylePrimaryName("root");
	}
	
	@UiFactory WinDeck makeWinDeck() {
		return new WinDeck(model.getWm());
	}
	
	@UiFactory WinTabBar makeWinTabBar() {
		return new WinTabBar(model.getWm());
	}
	
	@UiFactory ItemTreeView makeItemTreeView() {
		return new ItemTreeView(model.getServers(), model.getWm());
	}
	
	@UiFactory EntryView makeEntryView() {
		return new EntryView(model);
	}

	public void setController(Controller controller) {
//		this.controller = controller;
		itemTreeView.setController(controller);
		winTabBar.setController(controller);
		entryView.setController(controller);
	}
}
