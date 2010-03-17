package hudon.plugins.synergy.test.log;

import hudson.plugins.synergy.impl.GetProjectGroupingInfoCommand;

import java.io.IOException;

public class ReadProjectPurposeTest extends AbstractLogTest {
	/**
	 * Tests the update log 1.
	 */
	public void testLog1() throws IOException {
		GetProjectGroupingInfoCommand update = new GetProjectGroupingInfoCommand("All Sinistre/1.0 Integration Testing Projects");
		String log = readLog("logs/projectPurpose/projectPurpose1.log.txt");
		update.parseResult(log);
		String purpose = update.getProjectPurpose();
		String release = update.getRelease();
		
		// Parsing of the purpose should not be null.
		assertEquals("Integration Testing", purpose);
		assertEquals("Sinistre/1.0", release);
	}
}
