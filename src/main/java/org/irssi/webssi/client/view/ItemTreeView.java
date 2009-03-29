package org.irssi.webssi.client.view;

import org.irssi.webssi.client.control.Controller;
import org.irssi.webssi.client.model.Activity;
import org.irssi.webssi.client.model.Group;
import org.irssi.webssi.client.model.Server;
import org.irssi.webssi.client.model.Window;
import org.irssi.webssi.client.model.WindowItem;
import org.irssi.webssi.client.model.WindowManager;

import com.google.gwt.user.client.ui.TreeItem;

/**
 * Tree of servers and window items.
 */
public class ItemTreeView extends GroupTreeView implements WindowManager.Listener, Group.Listener<Window>, Window.Listener {
	
	private class ServerLevel implements GroupTreeView.Level<Server, WindowItem> {
		public Group<WindowItem> getChildren(Server server) {
			return server.getItems();
		}

		public Level<WindowItem, ?> getNextLevel() {
			return itemLevel;
		}

		public TreeItem getTreeItem(Server server) {
			TreeItem result = new TreeItem();
			result.setText(server.getTag());
			return result;
		}
		
		public void onTreeItemSelected(Server item) {
			// TODO if empty window, activate the server
			// otherwise, undo the selection?
		}
	}
	
	private class ItemLevel extends GroupTreeView.LeafLevel<WindowItem> {

		public TreeItem getTreeItem(WindowItem item) {
			return new ItemTreeItem(item);
		}
		
		public void onTreeItemSelected(WindowItem item) {
			controller.activateWindowItem(item);
		}
	}
	
	private class ItemTreeItem extends TreeItem implements Activity.Listener {
		private final WindowItem wi;

		private ItemTreeItem(WindowItem wi) {
			this.wi = wi;
			wi.getActivity().addListener(this);
			setText(wi.getVisibleName());
			refreshStyle();
		}
		
		private void refreshStyle() {
			this.setStyleName(ColorUtil.styleForActivity(wi.getActivity()));
		}
		
		public void activity() {
			refreshStyle();
		}
	}
	
	private final ServerLevel serverLevel = new ServerLevel();
	private final ItemLevel itemLevel = new ItemLevel();
	
	private final Group<Server> servers;
	private final WindowManager wm;
	private Controller controller;
	
	ItemTreeView(Group<Server> servers, WindowManager wm) {
		init(servers, serverLevel);
		this.servers = servers;
		this.wm = wm;
		wm.getWindows().addListener(this);
		wm.addListener(this);
	}
	
	public void setController(Controller controller) {
		this.controller = controller;
	}
	
	private void refreshSelected() {
		TreeItem treeItem;
		Window activeWindow = wm.getActiveWindow();
		if (activeWindow == null) {
			treeItem = null;
		} else {
			WindowItem activeItem = activeWindow.getActiveItem();
			if (activeItem != null && activeItem.getServer() != null) {
				Server server = activeItem.getServer();
				int serverIndex = servers.indexFor(server);
				int itemIndex = server.getItems().indexFor(activeItem);
				
				treeItem = getTree().getItem(serverIndex).getChild(itemIndex);
			} else { // TODO select active server for empty windows
				treeItem = null;
			}
		}
		setSelectedItem(treeItem);
	}

	/// Group.Listener<Window>
	
	public void itemAdded(Window item, int index) {
		item.addListener(this);
		refreshSelected();
	}

	public void itemMoved(Window item, int oldIndex, int newIndex) {
		// do nothing
	}

	public void itemRemoved(Window item, int index) {
		// do nothing
	}
	
	/// Window.Listener

	public void nameChanged(String name) {
		// nothing
	}

	public void textPrinted(String text) {
		// nothing
	}

	public void windowItemChanged(WindowItem item) {
		refreshSelected();
	}

	/// WindowManager.Listener
	
	public void windowChanged(Window win) {
		refreshSelected();
	}
}
