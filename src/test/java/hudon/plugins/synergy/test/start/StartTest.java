package hudon.plugins.synergy.test.start;

import hudson.plugins.synergy.impl.StartCommand;
import junit.framework.TestCase;

public class StartTest extends TestCase {
	/**
	 * Test the simplest good case
	 */
	public void testSimple() {
		StartCommand start = new StartCommand("database", "engine", "login", "password", false, "ccm.Exe", true);
		String result = "L30153:1833:192.168.1.10:10.52.130.54";
		start.parseResult(result);
		String ccmAddr = start.getCcmAddr();
		assertEquals("Fail to extract CCM_ADDR", result, ccmAddr);
	}
	
	/**
	 * Test with a warning.
	 * (HUDSON-4937)
	 */
	public void testWarning() {
		StartCommand start = new StartCommand("database", "engine", "login", "password", false, "ccm.Exe", true);
		String addr = "L30153:1833:192.168.1.10:10.52.130.54";
		String result = "Warning: Syntax error(s) in attribute 'users' of base/model/base/1.\n" + addr;
		start.parseResult(result);
		String ccmAddr = start.getCcmAddr();
		assertEquals("Fail to extract CCM_ADDR", addr, ccmAddr);		
	}
	
	/**
	 * Test password encoding.
	 */
	public void testPassword() {
		StartCommand start = new StartCommand("database", "engine", "login", "password", false, "ccm.Exe", true);
		String[] commands = start.buildCommand("ccm.exe");
		boolean[] mask = start.buildMask();
		for (int i=0;i<commands.length;i++) {
			if ("password".equals(commands[i])) {
				assertTrue("Bad password index",mask[i]);
				return;
			}
		}
		fail("No password index");
	}
}
