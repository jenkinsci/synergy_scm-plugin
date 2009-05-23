package hudon.plugins.synergy.test.log;

import java.io.IOException;
import java.util.List;

import hudson.plugins.synergy.impl.UpdateCommand;

public class ReadUpdateLogTest extends AbstractLogTest {
	/**
	 * Tests the update log 1.
	 */
	public void testLog1() throws IOException {
		UpdateCommand update = new UpdateCommand("project", false);
		String log = readLog("logs/updates/update1.log.txt");
		update.parseResult(log);
		List<String> updates = update.getUpdates();
		
		// Parsing of the log should not be null.
		assertNotNull(updates);
		
		// There are 3 updates in this log.
		assertEquals(3,updates.size());
		
		// Check the name of updated elements.
		assertEquals("runFlashGUI.cmd,3:ascii:1", updates.get(0));
		assertEquals("risk.ico,2:binary:1", updates.get(1));
		assertEquals("Increment1Frame.java,4:java:1", updates.get(2));
	}
}
