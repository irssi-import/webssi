package org.irssi.webssi.client;

import org.irssi.webssi.client.control.Commander;
import org.irssi.webssi.client.control.Controller;
import org.irssi.webssi.client.model.Model;
import org.irssi.webssi.client.sync.Synchronizers;
import org.irssi.webssi.client.view.View;

import com.google.gwt.core.client.EntryPoint;

/**
 * Our main class, gwt Entry point.
 */
public class Webssi implements EntryPoint {
	private Synchronizers synchronizers;
	private Controller controller;
	private JsonLink link;
	private Model model;
	
	/**
	 * This is the entry point method.
	 */
	public void onModuleLoad() {
		model = new Model();
		JsonLink jsonLink = new JsonLink();
		link = jsonLink;
		synchronizers = new Synchronizers(model, link);
		
		View view = new View(model);

		Commander commander = new Commander(link);
		link.setCommander(commander);
		controller = new Controller(model, view, commander, synchronizers);
		jsonLink.setCommander(commander);
	}

	public Link getLink() {
		return link;
	}
	
	public Model getModel() {
		return model;
	}

	public Controller getController() {
		return controller;
	}
	
	/**
	 * For JUnit tests
	 */
	void shutdown() {
		link.shutdown();
	}
}
