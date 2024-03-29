package org.irssi.webssi.client.control;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.irssi.webssi.client.Link;
import org.irssi.webssi.client.control.Command.Predictable;
import org.irssi.webssi.client.events.CommandEvent;
import org.irssi.webssi.client.events.EventHandler;
import org.irssi.webssi.client.events.JsonEvent;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.json.client.JSONArray;
import com.google.gwt.json.client.JSONObject;

/**
 * Manages the commands.
 * Manages the list of pending commands, and deals with echo and replaying.
 */
public class Commander {
	/**
	 * State of a prediction
	 */
	private static class PredictionState {
		private static class Missed extends PredictionState {
			private final Command firstMissedCommand;

			public Missed(Command firstMissedCommand) {
				this.firstMissedCommand = firstMissedCommand;
			}
			
			@Override
			void postProcess() {
				firstMissedCommand.undo();
			}
		}
		
		/**
		 * The prediction is still correct as far as we know (as far as we've processed).
		 */
		static final PredictionState OK = new PredictionState();
		
		/**
		 * The prediction failed, we got an event affection the prediction that wasn't expected.
		 */
		static final PredictionState FAILED = new PredictionState();
		
		/**
		 * Returns an instance of the third possible kind of state,
		 * indicating a prediction expected an event, but nothing was received.
		 */
		static PredictionState missed(Command firstMissedCommand) {
			return new Missed(firstMissedCommand);
		}
		
		/**
		 * Called after events have been processed, to repair any damage done by a wrong prediction if necessary.
		 */
		void postProcess() {
		}
	}
	
	private final Link link;
	
	/**
	 * List of commands that we sent but haven't received confirmation for
	 */
	private final List<Command> pendingCommands = new LinkedList<Command>();

	/**
	 * The command who's response is being processed.
	 */
	private Command processingCommand;
	
	/**
	 * True if the value of the property the processingCommand predicted was changed
	 * while that command was being processed.
	 * If this is still false at the end of processing for that command, that means no
	 * relevant event was received, so we are missing an echo. 
	 */
	private boolean processingCommandPredictableWasAffected;
	
	/**
	 * Maps failed predictions onto either
	 * * null if we did have an echo (event changing the predicted property) but its value didn't match what we predicted
	 * * if no echo was received at all, the first command being processed that affects this property (and missed the echo)
	 */
	private final Map<Predictable, PredictionState> predictions = new HashMap<Predictable, PredictionState>();
	
	/**
	 * Id for the next executed command.
	 */
	private int commandIdCounter = 1;
	
	public Commander(Link link) {
		this.link = link;
		link.addEventHandler("command", new EventHandler<CommandEvent>(){
			public void handle(CommandEvent event) {
				// if the predictable's value was not changed when the previous command ran
				// in other words, if we missed an echo
				if (processingCommand != null && !processingCommandPredictableWasAffected) {
					Predictable predictable = processingCommand.getPredictable();
					// if this prediction didn't already fail
					if (predictable != null && isOk(predictable)) {
						// remember that this command was the first one missing an echo
						predictions.put(predictable, PredictionState.missed(processingCommand));
					}
				}
				Integer commandId = event.getCommandId();
				if (commandId == -1) {
					processingCommand = null;
				} else {
					processingCommand = pendingCommands.remove(0);
					assert event.getCommandId() == processingCommand.getId();
					processingCommandPredictableWasAffected = false;
				}
			}
		});
	}
	
	private boolean isOk(Predictable predictable) {
		assert predictions.containsKey(predictable);
		return predictions.get(predictable) == PredictionState.OK;
	}

	/**
	 * Execute the given command, add it to the queue to send to irssi, and schedule a sync really soon 
	 */
	void execute(Command command) {
		pendingCommands.add(command);
		command.setId(commandIdCounter++);
		command.execute();
		if (command.isNoop()) { // no need to actually send this command
			// remove it from the queue again.
			// Note the add can't be moved after this if() because then the
			//  order of commands that trigger other commands when executing would be wrong
			pendingCommands.remove(command);
			return;
		}
		Predictable predictable = command.getPredictable();
		if (predictable != null && ! predictions.containsKey(predictable)) {
			predictions.put(predictable, PredictionState.OK);
		}
		link.scheduleSyncFast();
	}
	
	private static native void addCommandId(JavaScriptObject command, int id) /*-{
		command['id'] = id;
	}-*/;
	
	/**
	 * Returns the queue of pending commands converted in a String containing JSON to be sent to irssi.
	 */
	public String getPendingCommandsAsJson() {
		JSONArray commands = new JSONArray();
		for (Command command : pendingCommands) {
			JavaScriptObject js = command.getJS();
			addCommandId(js, command.getId());
			commands.set(commands.size(), new JSONObject(js));
		}
		
		return commands.toString();
	}
	
	/**
	 * Called for every incoming event right before it is processed.
	 * If this returns true, the event will not be handled by its normal listeners.
	 */
	public boolean preProcessEvent(JsonEvent event) {
		if (processingCommand != null) {
			Predictable predictable = processingCommand.getPredictable();
			if (predictable != null && predictable.affectedBy(event)) {
				processingCommandPredictableWasAffected = true;
				if (processingCommand.echoMatches(event) && isOk(predictable)) {
					// prediction was ok and still is ok
					return true;
				} else {
					// either a mismatched echo (affects the prediction, but not in the way we predicted)
					// or previous predictions for commands for this predictable failed.
					// In both cases we need to process this event and all following events that affect this predictable
					
					// Note that if we were already remembering a command that missed an echo for this predictable,
					// that one is forgotten here; because we do not need to undo that one anymore because this event
					// will already fix that wrong prediction
					predictions.put(predictable, PredictionState.FAILED);
				}
				return false;
			}
		}
		
		// the event is not triggered by a command, check if it makes any predictions fail
		
		for (Predictable predictable : predictions.keySet()) {
			if (predictable.affectedBy(event)) {
				predictions.put(predictable, PredictionState.FAILED);
			}
		}
		
		return false;
	}
	
	/**
	 * Called after events have been processed, to repair any damage done by failed predictions
	 */
	public void postProcessEvents() {
		// if a command missed its echo, and no relevant events were received after that,
		// revert the state to that from before the command was run.
		for (PredictionState state : predictions.values()) {
			state.postProcess();
		}
		
		// for commands that are still pending whose old prediction was based on a predicted value
		// that just turned out to be wrong, re-predict it.
		for (Command pendingCommand : pendingCommands) {
			if (pendingCommand.getPredictable() != null && ! isOk(pendingCommand.getPredictable())) {
				pendingCommand.execute();
			}
		}
		
		// all damage from failed predictions has been repaired now, so forgot about it:
		// drop old predictions and put all pending predictions in OK state.
		predictions.clear();
		for (Command pendingCommand : pendingCommands) {
			Predictable predictable = pendingCommand.getPredictable();
			if (predictable != null)
				predictions.put(predictable, PredictionState.OK);
		}
	}
	
	/**
	 * For tests only.
	 */
	List<Command> getPendingCommands() {
		return pendingCommands;
	}
}
