package org.irssi.webssi.client;

import java.util.HashSet;
import java.util.Set;

import org.irssi.webssi.client.expect.ExpectListener;
import org.irssi.webssi.client.expect.ExpectSession;
import org.irssi.webssi.client.expect.Expectable;
import org.irssi.webssi.client.expect.ExpectedCall;
import org.irssi.webssi.client.expect.Reaction;
import org.irssi.webssi.client.expect.Ref;
import org.irssi.webssi.client.model.Entry;
import org.irssi.webssi.client.model.Group;
import org.irssi.webssi.client.model.TestEntryListener;
import org.irssi.webssi.client.model.TestGroupListener;
import org.irssi.webssi.client.model.TestWindowListener;
import org.irssi.webssi.client.model.TestWindowManagerListener;
import org.irssi.webssi.client.model.Window;
import org.irssi.webssi.client.model.WindowManager;

import com.google.gwt.junit.client.GWTTestCase;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.DeferredCommand;

/**
 * Provides infrastructure for easily writing asynchronous tests.
 * 
 * @author Wouter Coekaerts <wouter@coekaerts.be>
 */
public abstract class AbstractExpectTest extends GWTTestCase {
	protected ExpectSession expectSession;
	
	/**
	 * Set of objects we're listening on.
	 * (We could have different sets for different kind of objects/listeners,
	 *  but we only use it to check if something is already in it,
	 *  so there's no need for more type safety)
	 */
	private Set<Object> listening = new HashSet<Object>();
	
	@Override
	protected void gwtSetUp() throws Exception {
		expectSession = new ExpectSession(new ExpectListener() {
			boolean done;
			public <T> void called(Expectable<T> expectable, T param) {
				log("called " + expectable + "(" + param + ")");
				if(!done)
					delayTestFinish();
			}
			
			public void log(String msg) {
				AbstractExpectTest.this.log(msg);
			}
			
			public void done() {
				log("done!");
				finishTest();
				done = true;
				
			}
		});
	}
	
	protected final <T> Reaction<T> finish() {
		return new Reaction<T>() {
			public void run(T param) {
				finishTest();
			};
		};
	}
	
	private void log(String msg) {
		System.err.println("***" + msg);
	}
	
	protected void delayTestFinish() {
		delayTestFinish(10000);
		log("delay!");
	}

	public String getModuleName() {
		return "org.irssi.webssi.TestWebssi";
	}
	
	static protected <T> Ref<T> newRef() {
		return new Ref<T>();
	}
	
	protected final <T extends Comparable<T>> void listen(TestGroupListener.ElementType<T> type, Group<T> group) {
		if (listening.add(group))
			TestGroupListener.listen(expectSession, type, group);
	}
	
	protected final void listen(WindowManager wm) {
		if (listening.add(wm))
			TestWindowManagerListener.listen(expectSession, wm);
	}
	
	protected final void listen(Window win) {
		if (listening.add(win))
			TestWindowListener.listen(expectSession, win);
	}
	
	protected final void listen(Entry entry) {
		if (listening.add(entry))
			TestEntryListener.listen(expectSession, entry);
	}
	
	private static final Expectable<Object> LATER = Expectable.declareExpectable("later");
	
	protected final ExpectedCall<Object> later() {
		final Object dummyParam = new Object();
		return expectSession.createRoot()
		.react(new Reaction<Object>() {
			public void run(Object param) {
				DeferredCommand.addCommand(new Command() {
					public void execute() {
						expectSession.called(LATER, dummyParam);
					}
				});
			}
		}).followedBy(LATER).withParam(dummyParam);
	}
}
