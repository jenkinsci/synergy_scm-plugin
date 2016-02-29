package hudon.plugins.synergy.test.log;

import hudson.plugins.synergy.SynergyPublisher.SynergyObject;

public class SynergyObjectTest extends AbstractLogTest {
	public void testLog1() {
		SynergyObject obj = new SynergyObject("HS_Restruct~BT7.1.0_Delivery:project:HBT#1");
		assertEquals("HS_Restruct",obj.name);
		assertEquals("BT7.1.0_Delivery",obj.version);
		assertEquals("project",obj.type);
		assertEquals("HBT#1",obj.instance);
		assertEquals("HS_Restruct~BT7.1.0_Delivery:project:HBT#1",obj.objectname);
	}

}
