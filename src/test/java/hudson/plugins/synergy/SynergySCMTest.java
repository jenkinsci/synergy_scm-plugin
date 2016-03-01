/*
	Â© Copyright 2012 BG-Phoenics GmbH, Hannover
	Alle Rechte sind vorbehalten.

	%created_by: u48jfe %
	%version: 2 %
	%create_time: CREATETIME%
**/

package hudson.plugins.synergy;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import hudson.console.ConsoleNote;
import hudson.model.TaskListener;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.StringUtils;
import org.junit.Test;


/**
 * @author u48jfe
 *
 */
public class SynergySCMTest {

  /**
   *
   */
  private static final String _I = File.separator;

  /**
   * @author u48jfe
   *
   */
  private final class TaskListenerImplementation implements TaskListener {

    public void hyperlink(String p_url, String p_text) throws IOException {
    }

    public PrintStream getLogger() {
      return System.out;
    }

    public PrintWriter fatalError(String p_format, Object... p_args) {
      return fatalError(String.format(p_format, p_args));
    }

    public PrintWriter fatalError(String p_msg) {
      System.out.print("FATAL: ");
      System.out.println(p_msg);
      return new PrintWriter(System.out);
    }

    public PrintWriter error(String p_format, Object... p_args) {
      return error(String.format(p_format, p_args));
    }

    public PrintWriter error(String p_msg) {
      System.out.print("ERROR: ");
      System.out.println(p_msg);
      return new PrintWriter(System.out);
    }

    public void annotate(ConsoleNote p_ann) throws IOException {
      p_ann.encodeTo(System.out);

    }
  }

  /**
   * Test method for {@link hudson.plugins.synergy.SynergySCM#isOneObjectSignificant(java.util.Map)}.
   */
  @Test
  public void testIsOneObjectSignificant() {


    SynergySCM l_synergySCM =
        new SynergySCM(null, null, null, null, null, null, null, null, null, null, null, null, false, false, true, false, false, null,
            false, null, ".dbx;.xmi");

    Map<String, String> l_map = new HashMap<String, String>();
    l_map.put("FWK.xmi", "BatchFwk_Maven" + _I + "xmi" + _I + "FWK.xmi");
    l_map.put("HS_WorkflowService.xmi", "HS_Restruct" + _I + "xmi" + _I + "HS_WorkflowService.xmi");

    // all objects match insignificant list -> no change
    assertFalse(l_synergySCM.isOneObjectSignificant(l_map, new TaskListenerImplementation()));

  }

  /**
   * Test method for {@link hudson.plugins.synergy.SynergySCM#isOneObjectSignificant(java.util.Map)}.
   */
  @Test
  public void testIsOneObjectSignificantTrue() {


    SynergySCM l_synergySCM =
        new SynergySCM(null, null, null, null, null, null, null, null, null, null, null, null, false, false, true, false, false, null,
            false, null, ".dbx;.xmi");

    Map<String, String> l_map = new HashMap<String, String>();
    l_map.put("FWK.xmi", "BatchFwk_Maven" + _I + "xmi" + _I + "FWK.xmi");
    l_map.put("HS_WorkflowService.xmi", "HS_Restruct" + _I + "xmi" + _I + "HS_WorkflowService.xmi");
    l_map.put("Test.java", "HS_Restruct" + _I + "xmi" + _I + "Test.java");
    l_map.put("Test.dbx", "HS_Restruct" + _I + "xmi" + _I + "Test.dbx");

    // at least one object does not match filter -> significant change
    assertTrue(l_synergySCM.isOneObjectSignificant(l_map, new TaskListenerImplementation()));

  }

  /**
   * Test method for {@link hudson.plugins.synergy.SynergySCM#isOneObjectSignificant(java.util.Map)}.
   */
  @Test
  public void testIsOneObjectSignificantNull() {


    SynergySCM l_synergySCM =
        new SynergySCM(null, null, null, null, null, null, null, null, null, null, null, null, false, false, true, false, false, null,
            false, null, null);

    Map<String, String> l_map = new HashMap<String, String>();
    l_map.put("FWK.xmi", "BatchFwk_Maven" + _I + "xmi" + _I + "FWK.xmi");
    l_map.put("HS_WorkflowService.xmi", "HS_Restruct" + _I + "xmi" + _I + "HS_WorkflowService.xmi");
    l_map.put("Test.java", "HS_Restruct" + _I + "xmi" + _I + "Test.java");
    l_map.put("Test.dbx", "HS_Restruct" + _I + "xmi" + _I + "Test.dbx");

    // no filter -> all modifications significant
    assertTrue(l_synergySCM.isOneObjectSignificant(l_map, new TaskListenerImplementation()));

  }



}
