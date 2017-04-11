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
 */
public class TaskCompletedInSourceFolderAndNotInTargetFolderCommand extends Command {

  private List<String> informations;
  private String sourceFolderId;
  private String targetFolderId;

  public TaskCompletedInSourceFolderAndNotInTargetFolderCommand(String sourceFolderId, String targetFolderId) {
    this.sourceFolderId = sourceFolderId;
    this.targetFolderId = targetFolderId;
  }

  @Override
  public String[] buildCommand(String ccmExe) {

    String[] commands = new String[]{ccmExe, "query", "-u", "-f", "%task_number",
      "is_task_in_folder_of('"
      + sourceFolderId + "') and not is_task_in_folder_of('" + targetFolderId
      + "')"};
    return commands;
  }

  @Override
  public void parseResult(String result) {
    informations = new ArrayList<String>();
    if (result != null) {
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
  }

  public List<String> getInformations() {
    return informations;
  }
}
