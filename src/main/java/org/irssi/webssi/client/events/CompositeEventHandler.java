package org.irssi.webssi.client.events;

/**
 * {@link EventHandler} delegating to two handlers. Composite pattern.
 */
public class CompositeEventHandler<T extends JsonEvent, U extends JsonEvent> implements EventHandler<JsonEvent> {
	
	public static <T extends JsonEvent, U extends JsonEvent> EventHandler<?> compose(EventHandler<T> handler1, EventHandler<U> handler2) {
		return new CompositeEventHandler<T,U>(handler1, handler2);
	}
	
	private final EventHandler<T> handler1;
	private final EventHandler<U> handler2;
	
	private CompositeEventHandler(EventHandler<T> handler1, EventHandler<U> handler2) {
		this.handler1 = handler1;
		this.handler2 = handler2;
	}
	
	public void handle(JsonEvent event) {
		handler1.handle(event.<T>cast());
		handler2.handle(event.<U>cast());
	}

}
