package org.irssi.webssi.client.view;

import org.irssi.webssi.client.model.Channel;
import org.irssi.webssi.client.model.Group;
import org.irssi.webssi.client.model.Nick;
import org.irssi.webssi.client.model.WindowItem;

import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.Widget;

class NicklistView extends Composite implements Group.Listener<Nick> {
	private final FlexTable table;
	
	NicklistView() {
		this.table = new FlexTable();
		initWidget(table);
	}
	
	void setItem(WindowItem item)  {
		if (item instanceof Channel) {
			Channel channel = (Channel) item;
			channel.getNicks().addListener(this);
			
			table.clear();
			int row = 0;
			for (Nick nick : channel.getNicks().getSortedItems()) {
				fillRow(nick, row++);
			}
		}
	}
	
	public void itemAdded(Nick nick, int index) {
		table.insertRow(index);
		fillRow(nick, index);
	}
	
	private void fillRow(Nick nick, int index) {
		//table.setText(index, 0, nick.getName());
		table.setWidget(index, 0, new NickView(nick));
	}

	public void itemRemoved(Nick item, int index) {
		table.removeRow(index);
	}

	public void itemMoved(Nick item, int oldIndex, int newIndex) {
		Widget nickView = table.getWidget(oldIndex, 0);
		table.removeRow(oldIndex);
		table.insertRow(newIndex);
		table.setWidget(newIndex, 0, nickView);
	}
}
