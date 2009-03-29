package org.irssi.webssi.client.view;

import java.util.ArrayList;

import org.irssi.webssi.client.model.Group;

import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Tree;
import com.google.gwt.user.client.ui.TreeItem;
import com.google.gwt.user.client.ui.TreeListener;

/**
 * Shows a hierarchy of {@link Group}s in a tree.
 */
class GroupTreeView extends Composite {
	
	/**
	 * Handles one level of the tree.
	 *
	 * @param <T> Type of items on this level
	 * @param <S> Type of items on the next level
	 */
	interface Level<T, S extends Comparable<S>> {
		Group<S> getChildren(T item);
		Level<S, ?> getNextLevel();
		TreeItem getTreeItem(T item);
		void onTreeItemSelected(T item);
	}
	
	/**
	 * Level without children.
	 * We're using Integer as a dummy type for the children.
	 */
	abstract static class LeafLevel<T> implements Level<T, Integer> {
		public Group<Integer> getChildren(T item) {
			return null;
		}
		
		public Level<Integer, ?> getNextLevel() {
			return null;
		}
	}
	
	private static abstract class AbstractHandler<T extends Comparable<T>> implements Group.Listener<T> {
		private final Group<T> group;
		private final Level<T, ?> level;
		
		public AbstractHandler(Group<T> group, Level<T, ?> level) {
			this.group = group;
			this.level = level;
			group.addListener(this);
		}
		
		abstract void addItem(TreeItem subTreeItem);
		abstract TreeItem getTreeItem(int index);
		abstract void removeTreeItem(TreeItem treeItem);
		
		private <S extends Comparable<S>> TreeItem buildItem(T item, Level<T, S> level) {
			TreeItem treeItem = level.getTreeItem(item);
			treeItem.setUserObject(new TreeItemUserObject<T>(level, item));
			Level<S, ?> nextLevel = level.getNextLevel();
			if (nextLevel != null) {
				Group<S> children = level.getChildren(item);
				new SubHandler<S>(children, nextLevel, treeItem);
			}
			return treeItem;
		}
		
		public void itemAdded(T item, int index) {
			insertTreeItem(buildItem(item, level), index);
		}
		
		/**
		 * Insert the given treeItem at the given index.
		 */
		// TODO directly insert treeItem, requires gwt 1.6
		private void insertTreeItem(TreeItem treeItem, int index) {
			// current tree is one item smaller than the group,
			// because the item we are about to insert it still missing
			int size = group.getSortedItems().size() - 1;

			// remove all items after the to be inserted one
			ArrayList<TreeItem> tail = new ArrayList<TreeItem>();
			for (int i = index; i < size; i++) {
				TreeItem removing = getTreeItem(index);
				removeTreeItem(removing);
				tail.add(removing);
			}
			
			// add the new item
			addItem(treeItem);
			
			// add the removed items again
			for (TreeItem toAdd : tail) {
				addItem(toAdd);
			}
		}

		public void itemMoved(T item, int oldIndex, int newIndex) {
			TreeItem treeItem = getTreeItem(oldIndex);
			removeTreeItem(treeItem);
			insertTreeItem(treeItem, newIndex);
		}

		public void itemRemoved(T item, int index) {
			removeTreeItem(getTreeItem(index));
		}
	}
	
	private static class TopHandler<T extends Comparable<T>> extends AbstractHandler<T> {
		final Tree tree;
		
		TopHandler(Tree tree, Group<T> group, Level<T, ?> level) {
			super(group, level);
			this.tree = tree;
		}
		
		@Override
		void addItem(TreeItem subTreeItem) {
			tree.addItem(subTreeItem);
		}
		
		@Override
		void removeTreeItem(TreeItem treeItem) {
			tree.removeItem(treeItem);
		}
		
		@Override
		TreeItem getTreeItem(int index) {
			return tree.getItem(index);
		}
	}
	
	private static class SubHandler<T extends Comparable<T>> extends AbstractHandler<T> {
		private final TreeItem treeItem;

		public SubHandler(Group<T> group, Level<T, ?> level, TreeItem treeItem) {
			super(group, level);
			this.treeItem = treeItem;
		}
		
		@Override
		void addItem(TreeItem subTreeItem) {
			treeItem.addItem(subTreeItem);
			treeItem.setState(true); // open the node
		}
		
		@Override
		void removeTreeItem(TreeItem childTreeItem) {
			treeItem.removeItem(childTreeItem);
		}
		
		@Override
		TreeItem getTreeItem(int index) {
			return treeItem.getChild(index);
		}
	}
	
	private static class TreeItemUserObject<T> {
		private final Level<T, ?> level;
		private final T item;
		
		private TreeItemUserObject(Level<T, ?> level, T item) {
			super();
			this.level = level;
			this.item = item;
		}
		
		private void onTreeItemSelected() {
			level.onTreeItemSelected(item);
		}
	}
	
	private final TreeListener treeListener = new TreeListener() {
		public void onTreeItemSelected(TreeItem item) {
			if (selectedItem != null && selectedItem != item)
				selectedItem.setSelected(false);
			selectedItem = item;
			((TreeItemUserObject<?>)item.getUserObject()).onTreeItemSelected();
		}

		public void onTreeItemStateChanged(TreeItem item) {
			// do nothing
		}
	};
	
	private final Tree tree;
	private TreeItem selectedItem;
	
	GroupTreeView() {
		tree = new Tree();
		initWidget(tree);
		tree.addTreeListener(treeListener);
	}
	
	protected <T extends Comparable<T>> void init(Group<T> roots, Level<T, ?> topLevel) {
		new TopHandler<T>(tree, roots, topLevel);
	}
	
	/**
	 * Gets the tree component. Should only be used by subclasses.
	 */
	protected Tree getTree() {
		return tree;
	}
	
	protected TreeItem getSelectedItem() {
		return selectedItem;
	}
	
	protected void setSelectedItem(TreeItem selectedItem) {
		if (this.selectedItem != selectedItem) {
			if (this.selectedItem != null)
				this.selectedItem.setSelected(false);
			if (selectedItem != null)
				selectedItem.setSelected(true);
			this.selectedItem = selectedItem;
		}
	}
}
