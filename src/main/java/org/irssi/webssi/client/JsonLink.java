package org.irssi.webssi.client;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.irssi.webssi.client.control.Commander;
import org.irssi.webssi.client.events.CompositeEventHandler;
import org.irssi.webssi.client.events.EventHandler;
import org.irssi.webssi.client.events.InitEvent;
import org.irssi.webssi.client.events.JsonEvent;

import com.google.gwt.core.client.JsArray;
import com.google.gwt.http.client.Request;
import com.google.gwt.http.client.RequestBuilder;
import com.google.gwt.http.client.RequestCallback;
import com.google.gwt.http.client.RequestException;
import com.google.gwt.http.client.RequestTimeoutException;
import com.google.gwt.http.client.Response;
import com.google.gwt.user.client.DeferredCommand;
import com.google.gwt.user.client.Timer;

/**
 * Handles communcation with irssi using Json.
 */
class JsonLink implements Link {
	private static final int STATUS_CODE_OK = 200;
	
	/**
	 * Mapping event name to eventhandler. Note that there can only be one handler per event.
	 */
	private HashMap<String, EventHandler<?>> eventHandlers = new HashMap<String, EventHandler<?>>();

	private Commander commander;
	
	/**
	 * sessionId, replaced with the real sessionId with the first sync (init event).
	 */
	private String sessionId = "newSession";
	
	/**
	 * Number of sync requests running
	 */
	private int syncsRunning = 0;
	
	/**
	 * If the syncTimer has been started to do a sync soon
	 */
	private boolean syncScheduled = false;
	
	/**
	 * Set to true when the link should be shut down
	 */
	private boolean shutdown = false;
	
	/**
	 * Timer to schedule a sync
	 */
	private final Timer syncTimer = new Timer() {
		@Override
		public void run() {
			syncScheduled = false;
			sync();
		}
	};
	
	public JsonLink() {
		addEventHandler("init", new EventHandler<InitEvent>() {
			public void handle(InitEvent event) {
				JsonLink.this.sessionId = event.getSessionId();
			}
		});
		addEventHandler("request superseded", new EventHandler<JsonEvent>() {
			public void handle(JsonEvent event) {
				// do nothing
			}
		});
		
		scheduleSyncFast();
	}
	
	void setCommander(Commander commander) {
		this.commander = commander;
	}
	
	public void addEventHandler(String type, EventHandler<?> handler) {
		EventHandler<?> existingHandler = eventHandlers.get(type);
		if (existingHandler == null) {
			eventHandlers.put(type, handler);
		} else {
			EventHandler<?> composite = CompositeEventHandler.compose(existingHandler, handler);
			eventHandlers.put(type, composite);
		}
	}
	
	/**
	 * Do a sync soon, if one isn't scheduled or running already.
	 */
	public void scheduleSync() {
		if (syncScheduled || syncsRunning > 0)
			return;
		syncScheduled = true;
		syncTimer.schedule(1000); // TODO we should probably not wait so long...
		debug("scheduled sync");
	}
	
	private boolean syncScheduledFast;
	
	/**
	 * Abort a scheduleSyncFast() (because a sync is started after it was scheduled,
	 * it's faster than fast!)
	 */
	private boolean abortNextFastScheduledSync;
	
	/**
	 * Schedule a sync really soon, but not immediately
	 */
	public void scheduleSyncFast() {
		if (syncScheduledFast)
			return;
		syncScheduledFast = true;
		DeferredCommand.addCommand(new com.google.gwt.user.client.Command() {
			public void execute() {
				syncScheduledFast = false;
				if (abortNextFastScheduledSync) {
					abortNextFastScheduledSync = false;
				} else {
					sync();
				}
			}
		});
	}
	
	/**
	 * Do a sync now
	 */
	private void sync() {
		if (shutdown)
			return;
		
		if (syncScheduled) {
			syncTimer.cancel();
			syncScheduled = false;
		}
		
		if (syncScheduledFast)
			abortNextFastScheduledSync = true;
		
		syncsRunning++;
		
		debug("syncing...");
		
		RequestBuilder builder = new RequestBuilder(RequestBuilder.POST, "/events.json");
		String data = sessionId + ' ' + commander.popCommands();
		debug("sending: " + data);
		try {
			// Request request =
			builder.sendRequest(data, new JsonRequestCallback());
			
		} catch (RequestException e) {
			debug("RequestException " + e.getMessage());
		}
	}
	
	/**
	 * Stop all communication.
	 */
	void shutdown() {
		shutdown = true;
		if (syncScheduled) {
			syncTimer.cancel();
		}
	}

	/**
	 * Handles http response from irssi
	 */
	private class JsonRequestCallback implements RequestCallback {

		public void onError(Request request, Throwable exception) {
			syncsRunning--;
			if (exception instanceof RequestTimeoutException) {
				debug("timeout");
				scheduleSync();
			} else {
				debug("http error: " + exception.toString());
			}
		}

		public void onResponseReceived(Request request, Response response) {
			if (shutdown)
				return;
			syncsRunning--;
			if (STATUS_CODE_OK == response.getStatusCode()) {
				//listener.debugMessage("JSON", response.getText());
				debug("JSON: " + response.getText());
				processEvents(response.getText());
				scheduleSync();
			} else {
				debug("response: " + response.getStatusCode() + " "
						+ response.getStatusText());
			}
		}
	}

	/**
	 * Handle response: parse and call the appropriate event handlers
	 */
	private void processEvents(String json) {
		for (JsonEvent event : eventsFromJson(json)) {
			boolean shouldIgnore = commander.preProcessEvent(event);
			if (shouldIgnore) {
				debug("ignoring echo for " + event.getType());
			} else {
				EventHandler<?> handler = eventHandlers.get(event.getType());
				if (handler != null) {
					callHandler(handler, event);
				} else {
					debug("unknown event: " + event.getType());
				}
			}
		}
		commander.postProcessEvents();
	}

	/**
	 * Parse the given String into a List of JsonEvents
	 */
	private static List<JsonEvent> eventsFromJson(String input) {
		JsArray<JsonEvent> jsArray = evalEvents(input);
		List<JsonEvent> events = new ArrayList<JsonEvent>();
		for (int i = 0; i < jsArray.length(); i++) {
			events.add(jsArray.get(i));
		}
		return events;
	}
	
	/**
	 * Parse the given string into a JsArray of JsonEvents
	 */
	private static final native JsArray<JsonEvent> evalEvents(String input) /*-{ 
		return eval('(' + input + ')') 
	}-*/;
	
	/**
	 * Helper method to capture the type (T) of the handler, cast the event, and call the handler.
	 */
	private <T extends JsonEvent> void callHandler(EventHandler<T> handler, JsonEvent event) {
		debug("Handling " + event.getType());
		handler.handle(event.<T>cast());
	}
	
	private void debug(String text) {
		System.err.println(text);
	}
}
