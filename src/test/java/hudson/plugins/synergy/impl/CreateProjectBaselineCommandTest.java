package hudson.plugins.synergy.impl;

import java.util.Arrays;
import java.util.Vector;

import org.junit.Test;
import static org.junit.Assert.*;

public class CreateProjectBaselineCommandTest {
	@Test
	public void testNoReleaseAndPurpose() {
		CreateProjectBaselineCommand cmd = new CreateProjectBaselineCommand("Test_baseline", "TestProj", "", "");
		String[] cmdList =  cmd.buildCommand("/opt/ccm/bin/ccm");
		for(int i = 0; i<cmdList.length; i++) {
			if(cmdList[i] == "-r")
				fail("-r options should not be here");
		}
		
		for(int i = 0; i<cmdList.length; i++) {
			if(cmdList[i] == "-purpose")
				fail("-p option should not be here");
		}

	}
}
