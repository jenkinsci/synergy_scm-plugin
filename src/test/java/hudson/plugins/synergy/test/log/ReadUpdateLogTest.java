package hudson.plugins.synergy.test.log;

import java.io.IOException;
import java.util.List;

import hudson.plugins.synergy.impl.UpdateCommand;

public class ReadUpdateLogTest extends AbstractLogTest {
	/**
	 * Tests the update log 1.
	 */
	public void testLog1() throws IOException {
		UpdateCommand update = new UpdateCommand(UpdateCommand.PROJECT, "project", false);
		String log = readLog("logs/updates/update1.log.txt");
		update.parseResult(log);
		List<String> updates = update.getUpdates().get("HS_Restruct~BT7.1.0_Delivery:project:HBT#1");
		
		// Parsing of the log should not be null.
		assertNotNull(updates);
		
		// There are 3 updates in this log.
		assertEquals(1,updates.size());
		
		// Check the name of updated elements.
		assertEquals("OrgModelAdminFacade.java~129:java:HBT#1", updates.get(0));
	}
}
