package hudson.plugins.synergy.impl;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang.StringUtils;

/**
 * Finds the projects in a project grouping. ccm query -t project -u -f
 * %objectname "release='AT10.0.0_MAVEN' and status='prep'"
 *
 * @author jrbe
 */
public class FindBaselineProjectByProcessRule extends Command {

  /**
   * The project release.
   */
  private String processRule;
  private String projectName;
  private static Map<String, String> projectMapping = new HashMap<String, String>();

  /**
   * The founded projects.
   */
  private List<String> projects;

  public FindBaselineProjectByProcessRule(String p_processRule, String p_projectName) {
    this.processRule = p_processRule;
    this.projectName = p_projectName;
    projectMapping.put("AP", "AT");
    projectMapping.put("BS", "BT");
    projectMapping.put("Doktyp-Root", "Doktyp");
    projectMapping.put("Avantis", "Avantis_Maven");
    projectMapping.put("EAkte", "EAkte_Maven");
    projectMapping.put("FF", "FF_Maven");
    projectMapping.put("FwkKern", "FwkKern_Maven");
    projectMapping.put("HS", "HS_Restruct");
    projectMapping.put("Lib", "Lib_Maven");
    projectMapping.put("SE", "SE_Maven");
    projectMapping.put("TDT", "TDT_Maven");
    projectMapping.put("TestFwk", "TestFwk_Maven");

  }

  @Override
  public String[] buildCommand(String ccmExe) {
    // ccm process_rule -show baseline_projects
    String[] commands = new String[]{ccmExe, "process_rule", "-show", "baseline_projects", "\"" + processRule + "\""};
    return commands;
  }

  @Override
  public void parseResult(String result) {
    if (result != null) {
      projects = new ArrayList<String>();
      try {
        BufferedReader reader = new BufferedReader(new StringReader(result));
        String sousProjet = reader.readLine();
        while (sousProjet != null) {
          if (sousProjet.trim().equals(projectMapping.get(projectName)) || sousProjet.trim().equals(projectName)) {
            projects.add(sousProjet.trim());
          }
          sousProjet = reader.readLine();
        }
      } catch (IOException e) {
        // Ignore on StringReader.
      }
    }
  }

  public List<String> getProjects() {
    return projects;
  }
}
