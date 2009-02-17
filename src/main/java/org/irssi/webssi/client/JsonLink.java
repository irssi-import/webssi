package org.irssi.webssi.client;

import java.util.HashMap;

import org.irssi.webssi.client.command.Command;
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
import com.google.gwt.json.client.JSONArray;
import com.google.gwt.json.client.JSONObject;
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

	private Controller listener;
	
	/**
	 * sessionId, replaced with the real sessionId with the first sync (init event).
	 */
	private String sessionId = "newSession";
	
	/**
	 * Queue of outgoing commands to be sent with the next sync
	 */
	private JSONArray commandQueue = new JSONArray();
	
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
		
		sync();
	}
	
	void setListener(Controller listener) {
		this.listener = listener;
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
		
		syncsRunning++;
		
		debug("syncing...");
		
		RequestBuilder builder = new RequestBuilder(RequestBuilder.POST, "/events.json");
		String data = sessionId + ' ' + commandQueue.toString();
		debug("sending: " + data);
		commandQueue = new JSONArray();
		try {
			// Request request =
			builder.sendRequest(data, new JsonRequestCallback());
			
		} catch (RequestException e) {
			listener.debugMessage("RequestException", e.getMessage());
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
				listener.debugMessage("error", "timeout");
				scheduleSync();
			} else {
				listener.debugMessage("fatal", exception.toString());
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
				listener.debugMessage("response", response.getStatusCode() + " "
						+ response.getStatusText());
			}
		}
	}

	/**
	 * Handle response: parse and call the appropriate event handlers
	 */
	private void processEvents(String json) {
//		try {
			JsArray<JsonEvent> events = eventsFromJson(json);
			for (int i = 0; i < events.length(); i++) {
				JsonEvent event = events.get(i);
				EventHandler<?> handler = eventHandlers.get(event.getType());
				if (handler != null) {
					callHandler(handler, event);
				} else {
					listener.debugMessage("unknown event", event.getType());
				}
			}
//		} catch (Throwable e) {
//			listener.debugMessage("error", e.toString());
//		}
	}

	/**
	 * Parse the given string into an array of JsonEvents
	 */
	private static final native JsArray<JsonEvent> eventsFromJson(String input) /*-{ 
		return eval('(' + input + ')') 
	}-*/;
	
	/**
	 * Helper method to capture the type (T) of the handler, cast the event, and call the handler.
	 */
	private <T extends JsonEvent> void callHandler(EventHandler<T> handler, JsonEvent event) {
		debug("Handling " + event.getType());
		handler.handle(event.<T>cast());
	}
	
	public void sendCommand(Command command) {
		JSONObject cmd = new JSONObject(command.createJS());
		commandQueue.set(commandQueue.size(), cmd);
		sync();
	}
	
	private void debug(String text) {
		System.err.println(text);
	}
}
