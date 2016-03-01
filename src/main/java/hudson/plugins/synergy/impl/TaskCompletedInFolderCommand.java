
package hudson.plugins.synergy.impl;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

/**
 * Get information about tasks.
 *
 * @author jrbe
 *
 * TODO : work with several task at once.
 */
public class TaskCompletedInFolderCommand extends Command {

  private List<String> informations;
  private String projectSpec;
  private String projectInstance = "MPH#1";
  private String folderId;

  public TaskCompletedInFolderCommand(String projectSpec, String folderId) {
    this.projectSpec = projectSpec;
    this.folderId = folderId;
  }

  @Override
  public String[] buildCommand(String ccmExe) {
    
    String[] commands = new String[] { ccmExe, "query", "-u", "-f", "%task_number",
                                       "status = 'completed' and is_task_in_folder_of(is_folder_in_rp_of('"
                                       + projectSpec + "')) and not is_task_in_folder_of('" + folderId
                                       + "')" };
    return commands;
  }

  @Override
  public void parseResult(String result) {
    informations = new ArrayList<String>();
    String line;
    try {
      BufferedReader reader = new BufferedReader(new StringReader(result));
      while ((line = reader.readLine()) != null) {
        informations.add(line);
      }

    } catch (IOException e) {
      // TODO: log parsing problems to hudson logfile
      // Will not happen on a StringReader.
    }
  }

  public List<String> getInformations() {
    return informations;
  }
}

