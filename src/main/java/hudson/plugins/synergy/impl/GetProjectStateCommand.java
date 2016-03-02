package hudson.plugins.synergy.impl;

import java.util.regex.Pattern;

/**
 * Builds a get project state command.
 */
public class GetProjectStateCommand extends Command {

  private String project;
  private String state;
  private Pattern p = Pattern.compile("Invalid value .* for the project_spec argument");

  public GetProjectStateCommand(String project) {
    this.project = project;
  }

  @Override
  public String[] buildCommand(String ccmExe) {
    String[] commands = new String[]{ccmExe, "attr", "-s", "status", "-p", project};
    return commands;
  }

  @Override
  public void parseResult(String result) {
    this.state = p.matcher(result).find() ? null : result;
  }

  public String getState() {
    return state;
  }

  /**
   * Return true if the given return status is ok for the command. The default is to return true if the status is 0 or 2
   * with empty output.
   *
   * Ignore error if project does not exist and return null as state.
   *
   * @param status	The ccm process return code
   * @param output	The ccm process output
   */
  @Override
  public boolean isStatusOK(int status, String output) {
    return status == 0 || ((status == 2) && (p.matcher(output).find()));
  }
}
