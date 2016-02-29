package hudson.plugins.synergy.impl;

import java.util.ArrayList;
import java.util.List;


/**
 * Synergy command that syncs
 * the specified project and its subprojects.
 */
public class ReconcileCommand extends Command {

  public enum PARAMS {
    UWA, UDB, CU, IF, MWAF
  };

  private String project;
  private PARAMS[] mode;

  public ReconcileCommand(String project, PARAMS... updateMode) {
    this.project = project;
    this.mode = updateMode;
  }

  @Override
  public String[] buildCommand(String ccmExe) {
    List<String> params = new ArrayList<String>();
    params.add(ccmExe);
    params.add("reconcile");
    params.add("-r");
    for (PARAMS l_string : mode) {
      params.add("-" + l_string);
    }
    params.add("-p");
    params.add(project);
    return params.toArray(new String[0]);
  }

  @Override
  public void parseResult(String result) {
    // do nothing.
  }

}
