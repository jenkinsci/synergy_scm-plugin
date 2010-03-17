package hudon.plugins.synergy.test.log;

import hudson.plugins.synergy.impl.GetProjectGroupingInfoCommand;

import java.io.IOException;

public class ReadProjectPurposeTest extends AbstractLogTest {
	/**
	 * Tests the update log 1.
	 */
	public void testLogSynergy65() throws IOException {
		GetProjectGroupingInfoCommand update = new GetProjectGroupingInfoCommand("All Sinistre/1.0 Integration Testing Projects");
		String log = readLog("logs/projectPurpose/projectPurpose1.log.txt");
		update.parseResult(log);
		String purpose = update.getProjectPurpose();
		String release = update.getRelease();
		
		// Parsing of the purpose should not be null.
		assertEquals("Integration Testing", purpose);
		assertEquals("Sinistre/1.0", release);
	}
	/**
	 * Tests the update log 2.
	 */
	public void testLogSynergy71() throws IOException {
		GetProjectGroupingInfoCommand update = new GetProjectGroupingInfoCommand("All Sinistre/1.0 Integration Testing Projects");
		String log = readLog("logs/projectPurpose/projectPurpose2.log.txt");
		update.parseResult(log);
		String purpose = update.getProjectPurpose();
		String release = update.getRelease();
		
		// Parsing of the purpose should not be null.
		assertEquals("Collaborative Development", purpose);
		assertEquals("FOOBAR/1.0_01", release);
	}

}
