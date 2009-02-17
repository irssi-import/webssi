package org.irssi.webssi.client.command;

import com.google.gwt.core.client.JavaScriptObject;

/**
 * Commmand sent from client to irssi.
 */
public abstract class Command {
	public abstract JavaScriptObject createJS();
}
