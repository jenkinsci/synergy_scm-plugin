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

  /**
   * Test method for {@link hudson.plugins.synergy.SynergySCM#createOptimizedSubLists(java.util.HashSet)}.
   */
  @Test
  public void testCreateOptimizedSubLists() {


    SynergySCM l_synergySCM =
        new SynergySCM(null, null, null, null, null, null, null, null, null, null, null, null, false, false, true, false, false, null,
            false, null, null);

    List<String> l_alle =
        Arrays.asList("status_bereitgestellt24.png~2:png:MPH#1", "ordner_schulungsort24.png~2:png:MPH#1",
            "dozent_zuordnen24.png~1:png:MPH#1", "warteliste24.png~2:png:MPH#1", "teilnehmer_auffuellen16.png~2:png:MPH#1",
            "status_inplanung24.png~2:png:MPH#1", "konflikt_akzeptieren16.png~1:png:MPH#2", "idee24.png~2:png:MPH#1",
            "bild_vorschau24.png~1:png:MPH#1", "akte_unternehmen16.png~2:png:MPH#1", "akte_seminar16.png~2:png:MPH#1",
            "akte_seminar24.png~2:png:MPH#1", "broschuere24.png~2:png:MPH#1", "IconsTest.java~25:java:MPH#1",
            "leeren_platz_entfernen16.png~2:png:MPH#1", "status_storniert24.png~2:png:MPH#1", "seminarart24.png~1:png:MPH#1",
            "teilnehmer_zuordnen16.png~2:png:MPH#1", "konflikt24.png~1:png:MPH#1", "pause_einfuegen16.png~2:png:MPH#1",
            "akte_schulungsort24.png~2:png:MPH#1", "schulungsort24.png~2:png:MPH#1", "akte_versicherter16.png~2:png:MPH#2",
            "legacy~22:dir:MPH#4", "status_abgeschlossen24.png~2:png:MPH#1", "schulungsraum16.png~1:png:MPH#1", "dozent24.png~2:png:MPH#1",
            "schulungsort16.png~2:png:MPH#1", "status_planung_abgeschlossen24.png~2:png:MPH#1", "idee16.png~2:png:MPH#1",
            "status_indurchfuehrung24.png~2:png:MPH#1", "akte_dozent16.png~1:png:MPH#1", "konflikt_loesen16.png~2:png:MPH#1",
            "teilnehmer_favorit16.png~2:png:MPH#1", "ordner_seminar24.png~2:png:MPH#1", "schulungsraum24.png~1:png:MPH#1",
            "dozent16.png~2:png:MPH#1", "ordner_seminarart24.png~2:png:MPH#1", "ziel24.png~2:png:MPH#1", "link_vorschau24.png~1:png:MPH#1",
            "teilnehmer_entfernen16.png~2:png:MPH#1", "akte_seminarart16.png~2:png:MPH#1", "ordner_seminartermin24.png~2:png:MPH#1",
            "status_durchgefuehrt24.png~2:png:MPH#1", "status24.png~1:png:MPH#1", "konflikt_loesen24.png~1:png:MPH#1",
            "status_ueberfaellig24.png~2:png:MPH#1", "konflikt_akzeptieren24.png~1:png:MPH#2",
            "konflikt_nicht_akzeptieren16.png~2:png:MPH#1", "ziel16.png~2:png:MPH#1", "ordner_broschuere24.png~2:png:MPH#1",
            "thema_teilen16.png~2:png:MPH#1", "broschuere_vorschau16.png~1:png:MPH#1", "bild_vorschau16.png~2:png:MPH#1",
            "broschuere16.png~2:png:MPH#1", "broschuere_exportieren16.png~2:png:MPH#1", "akte_unternehmen24.png~2:png:MPH#1",
            "leeren_platz_hinzufuegen16.png~2:png:MPH#1", "pause_einfuegen24.png~1:png:MPH#1", "akte_seminarart24.png~2:png:MPH#1",
            "seminarart16.png~2:png:MPH#1", "seminar_bearbeiten16.png~2:png:MPH#1", "seminar24.png~2:png:MPH#1",
            "ordner_mitgliedschaft24.png~2:png:MPH#1", "konflikt_nicht_akzeptieren24.png~2:png:MPH#1", "warteliste16.png~2:png:MPH#1",
            "ordner_unternehmen24.png~2:png:MPH#1", "seminar16.png~2:png:MPH#1", "konflikt16.png~2:png:MPH#1",
            "ordner_dozent24.png~2:png:MPH#1", "akte_dozent24.png~2:png:MPH#1", "seminarart_bearbeiten16.png~2:png:MPH#1",
            "dozent_zuordnen16.png~2:png:MPH#1", "teilnehmer_auf_warteliste16.png~2:png:MPH#1", "akte_schulungsort16.png~2:png:MPH#1",
            "teilnehmer_bearbeiten16.png~2:png:MPH#1", "teilnehmer_hinzufuegen16.png~2:png:MPH#1", "link_vorschau16.png~2:png:MPH#1",
            "Icons.java~25:java:MPH#1", "akte_versicherter24.png~2:png:MPH#4");

    HashSet<String> l_hashSet = new HashSet<String>(l_alle);

    List<List<String>> l_createOptimizedSubLists = l_synergySCM.createOptimizedSubLists(l_hashSet);
    assertTrue(l_createOptimizedSubLists.size() == 6);
    for (List<String> l_list : l_createOptimizedSubLists) {
      assertTrue(("\"" + StringUtils.join(l_list, "\", \"") + "\"").length() < 512);
      for (String l_string : l_list) {
        assertTrue(l_hashSet.contains(l_string));
        l_hashSet.remove(l_string);
      }
    }
    assertTrue(l_hashSet.isEmpty());

  }

}
