package org.irssi.webssi.client.control;

import org.irssi.webssi.client.events.JsonEvent;

import com.google.gwt.core.client.JavaScriptObject;

/**
 * Command sent from client to irssi.
 */
abstract class Command {
	/**
	 * A property on a model object whose value is predicted by a command.
	 * Default implementation identifies the property by the type of event that affects it,
	 * and assumes all such events affect it.
	 */
	static class Predictable {
		private final String echoEventType;
		
		public Predictable(String echoEventType) {
			this.echoEventType = echoEventType;
		}

		/**
		 * Returns true if the given event changes the value of the predicted property.
		 */
		public boolean affectedBy(JsonEvent event) {
			return echoEventType.equals(event.getType());
		}
		
		@Override
		public int hashCode() {
			return echoEventType.hashCode();
		}
		
		@Override
		public boolean equals(Object obj) {
			return (obj instanceof Predictable) && ((Predictable)obj).echoEventType.equals(echoEventType);
		}
	};
	
	private final JavaScriptObject js;
	private int id;
	
	Command() {
		this(null);
	}
	
	Command(JavaScriptObject js) {
		this.js = js;
	}
	
	/**
	 * Returns the JavaScriptObject to send to irssi.
	 */
	JavaScriptObject getJS() {
		return js;
	}
	
	/**
	 * Applies this command in the model.
	 */
	abstract void execute();
	
	/**
	 * Called when an event that affect the prediction is sent while irssi processes this command.
	 * Returns true if the event matches what we predicted.
	 * (In that case, normal event handlers will not be called.) 
	 */
	boolean echoMatches(JsonEvent event) {
		return false;
	}
	
	/**
	 * The property this command predicts, or null if it doesn't predict anything.
	 */
	Predictable getPredictable() {
		return null;
	}

	/**
	 * Reverts the state of the model like it was before this command was last executed.
	 * Must be implemented by commands that return a non-null predictable.
	 */
	void undo() {
		assert false; // should never be called for commands that don't override it 
	}

	/**
	 * Returns the id of this command. Command id's are ascending in the order that they were executed.
	 */
	int getId() {
		return id;
	}

	void setId(int id) {
		this.id = id;
	}
}
