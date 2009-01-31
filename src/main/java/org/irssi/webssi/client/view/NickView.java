package org.irssi.webssi.client.view;

import org.irssi.webssi.client.model.Nick;

import com.google.gwt.user.client.ui.HTML;

class NickView extends HTML implements Nick.Listener {
	private final Nick nick;
	
	private static String toHTML(Nick nick) {
		return nick.getName();
	}
	
	NickView(Nick nick) {
		super(nick.getName());
		this.nick = nick;
		nick.setListener(this);
	}

	public void nameChanged() {
		setHTML(toHTML(nick));
	}
}
