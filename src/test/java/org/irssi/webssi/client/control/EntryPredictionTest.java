package org.irssi.webssi.client.control;

import java.util.Arrays;
import java.util.List;

import org.irssi.webssi.client.TestWebssi;

import com.google.gwt.junit.client.GWTTestCase;

/**
 * Tests for the predictions of the entry content.
 */
public class EntryPredictionTest extends GWTTestCase {
	private TestWebssi webssi;
	private int eventIdCounter = 1;
	
	@Override
	protected void gwtSetUp() throws Exception {
		super.gwtSetUp();
		webssi = new TestWebssi();
		webssi.onModuleLoad();
	}
	
	public String getModuleName() {
		return "org.irssi.webssi.Webssi";
	}
	
	/**
	 * "a" key resulting simply in "a"
	 */
	public void testCorrectPrediction() {
		webssi.getController().keyPressed('A', 'a', 0);
		assertEntryEquals("a", 1);
		
		List<Command> commands = popCommands(1);
		
		fakeIncoming(
			commandEvent(commands.get(0)),
			entryChangedEvent("a", 1),
			commandEvent(null)
		);
		
		assertEntryEquals("a", 1);
	}
	
	/**
	 * "a" key resulting in "b"
	 */
	public void testFailedPrediction() {
		webssi.getController().keyPressed('A', 'a', 0);
		assertEntryEquals("a", 1);
		
		List<Command> commands = popCommands(1);
		
		fakeIncoming(
			commandEvent(commands.get(0)),
			entryChangedEvent("b", 1),
			commandEvent(null)
		);
		
		assertEntryEquals("b", 1);
	}
	
	/**
	 * "a" key resulting in "x", with a "b" still pending => replay "b" giving "xb"
	 */
	public void testFailReplayPending() {
		webssi.getController().keyPressed('A', 'a', 0);
		webssi.getController().keyPressed('B', 'b', 0);
		assertEntryEquals("ab", 2);
		
		List<Command> commands = popCommands(2);
		
		fakeIncoming(
			commandEvent(commands.get(0)),
			entryChangedEvent("x", 1),
			commandEvent(null)
		);
		
		assertEntryEquals("xb", 2);
	}
	
	/**
	 * "a" + "b" + "c", with "a" having effect, but "b" + "c" both having no result => undo "b", leaving "a"
	 */
	public void testMiss() {
		webssi.getController().keyPressed('A', 'a', 0);
		webssi.getController().keyPressed('B', 'b', 0);
		webssi.getController().keyPressed('C', 'c', 0);
		assertEntryEquals("abc", 3);
		
		List<Command> commands = popCommands(3);
		
		fakeIncoming(
				commandEvent(commands.get(0)),
				entryChangedEvent("a", 1),
				commandEvent(commands.get(1)),
				commandEvent(commands.get(2)),
				commandEvent(null)
		);
		
		assertEntryEquals("a", 1);
	}
	
	/**
	 * "a" + "b" resulting in "a" having no result, and "b" still pending
	 * => undo "a", and replay "b", resulting in just "b"
	 */
	public void testMissReplayPending() {
		webssi.getController().keyPressed('A', 'a', 0);
		webssi.getController().keyPressed('B', 'b', 0);
		assertEntryEquals("ab", 2);
		
		List<Command> commands = popCommands(2);
		
		fakeIncoming(
				commandEvent(commands.get(0)),
				commandEvent(null)
		);
		
		assertEntryEquals("b", 1);
	}
	
	/**
	 * "a" + "b" resulting in an unexpected "x" (before command) + "a" + b still pending
	 * => "xab"
	 */
	public void testUnexpected() {
		webssi.getController().keyPressed('A', 'a', 0);
		webssi.getController().keyPressed('B', 'b', 0);
		assertEntryEquals("ab", 2);
		
		List<Command> commands = popCommands(2);
		
		fakeIncoming(
				entryChangedEvent("x", 1),
				commandEvent(commands.get(0)),
				entryChangedEvent("xa", 2),
				commandEvent(null)
		);
		
		assertEntryEquals("xab", 3);
	}
	
	/**
	 * "a" + "b" + "c" resulting in "x", "ab" + "c" still pending.
	 * In other words, a prediction ("ab") coming true, but only 'by coincidence'; previous predictions ("a") failed
	 * => replay "c" (even though the last prediction was correct) => "abc"
	 */
	public void testCorrectAfterFail() {
		webssi.getController().keyPressed('A', 'a', 0);
		webssi.getController().keyPressed('B', 'b', 0);
		webssi.getController().keyPressed('C', 'c', 0);
		assertEntryEquals("abc", 3);
		
		List<Command> commands = popCommands(3);
		
		fakeIncoming(
				commandEvent(commands.get(0)),
				entryChangedEvent("x", 1),
				commandEvent(commands.get(1)),
				entryChangedEvent("ab", 2),
				commandEvent(null)
		);
		
		assertEntryEquals("abc", 3);
	}
	
	/**
	 * "a" + "b" + "c" resulting in nothing + "b" + "c" still pending.
	 * In other words, a prediction ("ab") coming true, but only 'by coincidence'; previous predictions ("a") was missed
	 * => replay "c" (even though the last prediction was correct) => "abc"
	 */
	public void testCorrectAfterMiss() {
		webssi.getController().keyPressed('A', 'a', 0);
		webssi.getController().keyPressed('B', 'b', 0);
		webssi.getController().keyPressed('C', 'c', 0);
		assertEntryEquals("abc", 3);
		
		List<Command> commands = popCommands(3);
		
		fakeIncoming(
				commandEvent(commands.get(0)),
				commandEvent(commands.get(1)),
				entryChangedEvent("ab", 2),
				commandEvent(null)
		);
		
		assertEntryEquals("abc", 3);
	}
	
	private void fakeIncoming(String... events) {
		webssi.fakeIncoming(Arrays.toString(events));
	}
	
	private String commandEvent(Command command) {
		return "{\"type\":\"command\",\"id\":" + (command == null ? -1 : command.getId()) + ",\"i\": " + (eventIdCounter++) + "}";
	}
	
	private String entryChangedEvent(String content, int cursorPos) {
		return "{\"type\":\"entry changed\", \"content\":\""+ content + "\",\"cursorPos\":"+cursorPos + ",\"i\": " + (eventIdCounter++) + "}";
	}
	
	private List<Command> popCommands(int expectedPendingCommandCount) {
		List<Command> commands = webssi.getCommander().getPendingCommands();
		assertEquals(expectedPendingCommandCount, commands.size());
		return commands;
	}
	
	private void assertEntryEquals(String content, int cursorPos) {
		assertEquals(content, webssi.getModel().getEntry().getContent());
		assertEquals(cursorPos, webssi.getModel().getEntry().getCursorPos());
	}
}
