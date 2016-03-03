package hudson.plugins.synergy.impl;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;

public class GetFolderIDCommand extends Command {

  private String release;
  private String folderID;
  private String folderDescription;

  public String getRelease() {
    return release;
  }

  public void setRelease(String release) {
    this.release = release;
  }

  public String getFolderID() {
    return folderID;
  }

  public void setFolderID(String folderID) {
    this.folderID = folderID;
  }

  public GetFolderIDCommand(String release, String folderDescription) {
    this.release = release;
    this.folderDescription = folderDescription;

  }

  @Override
  public String[] buildCommand(String ccmExe) {
    // Construct Folder Name
    String folderName = release + getFolderDescription();
    String[] commands = new String[]{ccmExe, "query", "-u", "-f", "%objectname", "description = '" + folderName + "'"};
    return commands;
  }

  @Override
  public void parseResult(String result) {
    try {
      BufferedReader reader = new BufferedReader(new StringReader(result));
      folderID = reader.readLine();

    } catch (IOException e) {
      // Will not happen on a StringReader.
    }

  }

  /**
   * @return the folderDescription
   */
  public String getFolderDescription() {
    return folderDescription;
  }

  /**
   * @param folderDescription the folderDescription to set
   */
  public void setFolderDescription(String folderDescription) {
    this.folderDescription = folderDescription;
  }
}
