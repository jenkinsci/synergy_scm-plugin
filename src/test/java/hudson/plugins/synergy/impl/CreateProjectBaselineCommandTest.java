package hudson.plugins.synergy.impl;

import static org.junit.Assert.*;
import org.junit.Test;

public class CreateProjectBaselineCommandTest {

  @Test
  public void testNoReleaseAndPurpose() {
    CreateProjectBaselineCommand cmd = new CreateProjectBaselineCommand("Test_baseline", "%baseline_name", "TestProj", "", "");
    String[] cmdList = cmd.buildCommand("/opt/ccm/bin/ccm");
    for (int i = 0; i < cmdList.length; i++) {
      if (cmdList[i] == "-r") {
        fail("-r options should not be here");
      }
    }

    for (int i = 0; i < cmdList.length; i++) {
      if (cmdList[i] == "-purpose") {
        fail("-p option should not be here");
      }
    }

  }
}
