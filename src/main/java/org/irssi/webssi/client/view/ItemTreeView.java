package org.irssi.webssi.client.view;

import org.irssi.webssi.client.model.Group;
import org.irssi.webssi.client.model.Server;
import org.irssi.webssi.client.model.WindowItem;
import org.irssi.webssi.client.view.GroupTreeView.Level;

import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.TreeItem;

/**
 * Tree of servers and window items.
 */
public class ItemTreeView extends Composite {
	
	private static class ServerLevel implements GroupTreeView.Level<Server, WindowItem> {
		public Group<WindowItem> getChildren(Server server) {
			return server.getItems();
		}

		public Level<WindowItem, ?> getNextLevel() {
			return ITEM_LEVEL;
		}

		public TreeItem getTreeItem(Server server) {
			TreeItem result = new TreeItem();
			result.setText(server.getTag());
			return result;
		}
	}
	
	private static class ItemLevel extends GroupTreeView.LeafLevel<WindowItem> {

		public TreeItem getTreeItem(WindowItem item) {
			TreeItem result = new TreeItem();
			result.setText(item.getVisibleName());
			return result;
		}
	}
	
	private static final ServerLevel SERVER_LEVEL = new ServerLevel();
	private static final ItemLevel ITEM_LEVEL = new ItemLevel();
	
	ItemTreeView(Group<Server> servers) {
		initWidget(new GroupTreeView(servers, SERVER_LEVEL));
	}
}
