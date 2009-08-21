package org.irssi.webssi.client;

import static org.irssi.webssi.client.model.TestEntryListener.CONTENT_CHANGED;
import static org.irssi.webssi.client.model.TestGroupListener.WIN;
import static org.irssi.webssi.client.model.TestWindowListener.WINDOW_ITEM_CHANGED;
import static org.irssi.webssi.client.model.TestWindowListener.WINDOW_NAME_CHANGED;
import static org.irssi.webssi.client.model.TestWindowManagerListener.WINDOW_CHANGED;

import org.irssi.webssi.client.events.EventHandler;
import org.irssi.webssi.client.events.JsonEvent;
import org.irssi.webssi.client.expect.Expectable;
import org.irssi.webssi.client.expect.ExpectedCall;
import org.irssi.webssi.client.expect.Reaction;
import org.irssi.webssi.client.expect.Ref;
import org.irssi.webssi.client.expect.SimpleReaction;
import org.irssi.webssi.client.model.Channel;
import org.irssi.webssi.client.model.Model;
import org.irssi.webssi.client.model.Window;
import org.irssi.webssi.client.model.WindowItem;

import com.google.gwt.event.dom.client.KeyCodes;

/**
 * Tests some scenarios.
 * This test requires an irssi with the script loaded, authentication disabled,
 * connected to a server.
 */
public class WebssiTest extends AbstractExpectTest {
	private Link link;
	private Webssi webssi;
	private Model model;
	
	private void sendCommand(String command) {
		webssi.getController().sendLine(null, command);
	}
	
	private Reaction<Object> sendCommandReaction(final Ref<Window> win, final String command) {
		return new SimpleReaction() {
			public void run() {
				webssi.getController().sendLine(win.value, command);
			}
		};
	}
	
	private Reaction<Window> addWindowListenerReaction() {
		return new Reaction<Window>() {
			public void run(Window param) {
				listen(param);
			}
		};
	}
	
	@Override
	protected void gwtSetUp() throws Exception {
		super.gwtSetUp();
		webssi = new Webssi();
		webssi.onModuleLoad();
		link = webssi.getLink();
		model = webssi.getModel();
	}
	
	@Override
	protected void gwtTearDown() throws Exception {
		super.gwtTearDown();
		webssi.shutdown();
	}
	
	private static final Expectable<String> EVENT = Expectable.declareExpectable("event");
	
	protected final ExpectedCall<?> event(final String eventName) {
		return expectSession.createRoot()
		.react(new Reaction<Object>() {
			public void run(Object param) {
				link.addEventHandler(eventName, new EventHandler<JsonEvent>() {
					boolean done;
					public void handle(JsonEvent event) {
						if (!done) {
							done = true;
							expectSession.called(EVENT, eventName);
						}
					}
				});
			}
		}).followedBy(EVENT).withParam(eventName);
	}
	
	private ExpectedCall<?> joinChannel(final String channelName, final Ref<Window> channelWin, final Ref<Channel> channel) {
		return expectSession.createRoot()
		.react(new SimpleReaction() {
			public void run() {
				listen(model.getWm());
				listen(WIN, model.getWm().getWindows());
				sendCommand("/join " + channelName);
			}
		}).followedBy(WIN.ITEM_ADDED).react(new Reaction<Window>(){
			@Override
			public void run(Window win) {
				channelWin.value = win;
				listen(win);
			}
		}).followedBy(WINDOW_CHANGED).react(new Reaction<Window>() {
			@Override
			public void run(Window win) {
				assertSame(channelWin.value, win);
			}
		}).followedBy(WINDOW_ITEM_CHANGED).react(new Reaction<WindowItem>() {
			public void run(WindowItem item) {
				assertTrue(item instanceof Channel);
				assertEquals(channelName, item.getVisibleName());
			};
		});
	}

	private ExpectedCall<?> partChannel(final String channelName, final Ref<Window> channelWin) {
		return expectSession.createRoot().react(new Reaction<Object>() {
			@Override
			public void run(Object param) {
				sendCommand("/part " + channelName);
			};
		}).followedBy(WINDOW_ITEM_CHANGED).withParam(null)
		.followedBy(WINDOW_CHANGED)
		.followedBy(WIN.ITEM_REMOVED).react(new Reaction<Window>() {
			@Override
			public void run(Window win) {
				if (channelWin != null)
					assertSame(channelWin.value, win);
			}
		});
	}
	
	private ExpectedCall<?> init() {
		return event("init").followedBy(later());
	}
	

	public void testJoin() {
		delayTestFinish();
		final Ref<Window> testWindow = newRef();
		final Ref<Channel> testChannel = newRef();
		
		expectSession.beginning()
		.followedBy(init())
		.followedBy(joinChannel("#webssi-test", testWindow, testChannel))
		.react(new SimpleReaction() {
			public void run() {
				assertEquals("", testWindow.value.getName());
				assertEquals("#webssi-test", testWindow.value.getTitle());
			}
		}).followedBy(partChannel("#webssi-test", testWindow))
		;
		
		expectSession.start();
	}
	
	public void test2Joins() {
		delayTestFinish();
		final Ref<Window> testWindow = newRef();
		final Ref<Channel> testChannel = newRef();
		final Ref<Window> testWindow2 = newRef();
		final Ref<Channel> testChannel2 = newRef();
		
		expectSession.beginning()
		.followedBy(init())
		.followedBy(joinChannel("#webssi2-test", testWindow, testChannel))
		.followedBy(joinChannel("#webssi2-test2", testWindow2, testChannel2))
		.followedBy(partChannel("#webssi2-test2", testWindow2))
		.followedBy(partChannel("#webssi2-test", testWindow))
		;
		
		expectSession.start();
	}
	
	private ExpectedCall<?> createNamedWindow(final String name, final Ref<Window> window) {
		return expectSession.createRoot()
		.react(new SimpleReaction() {
			public void run() {
				listen(model.getWm());
				listen(WIN, model.getWm().getWindows());
				sendCommand("/window new hide");
			}
		}).followedBy(WIN.ITEM_ADDED).react(Reaction.saveParam(window))
		.followedBy(WINDOW_CHANGED).withParamRef(window)
		.react(addWindowListenerReaction())
		.react(sendCommandReaction(window, "/window name " + name))
		.followedBy(WINDOW_NAME_CHANGED).withParam(name)
		.react(new SimpleReaction() {
			public void run() {
				assertEquals(name, window.value.getName());
				assertEquals(name, window.value.getTitle());
			}
		})
		;
	}
	
	private ExpectedCall<?> closeWindow(Ref<Window> window) {
		return expectSession.createRoot()
		.react(sendCommandReaction(window, "/window close"))
		.followedBy(WINDOW_CHANGED).optional()
		.followedBy(WIN.ITEM_MOVED).zeroOrMoreTimes()  // if windows_auto_renumber ON
		.followedBy(WIN.ITEM_REMOVED);
	}
	
	public void testWindowMove() {
		delayTestFinish();
		final Ref<Window> window2 = newRef();
		final Ref<Window> window3 = newRef();
		
		expectSession.beginning()
		.followedBy(init())
		.followedBy(createNamedWindow("window2", window2))
		.followedBy(createNamedWindow("window3", window3))
		.react(new SimpleReaction() {
			public void run() {
				assertEquals(2, window2.value.getRefnum());
				assertEquals(3, window3.value.getRefnum());
			}
		}).react(sendCommandReaction(window3, "/window move 2"))
		.followedBy(WIN.ITEM_MOVED)
		.followedBy(WIN.ITEM_MOVED)
		.react(new SimpleReaction() {
			public void run() {
				assertEquals(3, window2.value.getRefnum());
				assertEquals(2, window3.value.getRefnum());
			}
		}).followedBy(closeWindow(window3))
		.react(new SimpleReaction() {
			public void run() {
				// window 2 got back to refnum 2 because of windows_auto_renumber
				assertEquals(2, window2.value.getRefnum());
			}
		}).followedBy(closeWindow(window2))
		;
		
		expectSession.start();
	}
	
	private ExpectedCall<?> activateWindow(final Ref<Window> window) {
		return expectSession.createRoot()
		.react(new SimpleReaction() {
			public void run() {
				listen(model.getWm());
				webssi.getController().activateWindow(window.value);
			}
		}).followedBy(WINDOW_CHANGED).withParamRef(window);
	}
	
	public void testActivateWindow() {
		delayTestFinish();
		final Ref<Window> window2 = newRef();
		final Ref<Window> window3 = newRef();
		
		expectSession.beginning()
		.followedBy(init())
		.followedBy(createNamedWindow("window2", window2))
		.followedBy(createNamedWindow("window3", window3))
		.followedBy(activateWindow(window2))
		.followedBy(closeWindow(window3))
		.followedBy(closeWindow(window2))
		;
		
		expectSession.start();
	}
	
	public void testEntry() {
		final DummyKeyEvent dummyKeyEvent = new DummyKeyEvent();
		delayTestFinish();
		expectSession.beginning()
		.followedBy(init())
		.react(new SimpleReaction() {
			public void run() {
				listen(model.getEntry());
				webssi.getController().keyPressed('A', 'a', dummyKeyEvent);
				webssi.getController().keyPressed('B', 'b', dummyKeyEvent);
				webssi.getController().keyPressed('C', 'c', dummyKeyEvent);
				webssi.getController().keyPressed(KeyCodes.KEY_ENTER, '\0', dummyKeyEvent);
			}
		}).followedBy(CONTENT_CHANGED).withParam("a")
		.followedBy(CONTENT_CHANGED).withParam("ab")
		.followedBy(CONTENT_CHANGED).withParam("abc")
		.followedBy(CONTENT_CHANGED).withParam("")
		;
		
		expectSession.start();
	}
}
