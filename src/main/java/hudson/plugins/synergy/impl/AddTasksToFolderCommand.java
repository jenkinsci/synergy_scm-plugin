package hudson.plugins.synergy.impl;

import java.util.Collection;

import org.apache.commons.lang.StringUtils;

public class AddTasksToFolderCommand extends Command {

  private Collection<String> tasks;
  private String folderId;

  public Collection<String> getTasks() {
    return tasks;
  }

  public void setTasks(Collection<String> tasks) {
    this.tasks = tasks;
  }

  public String getFolderId() {
    return folderId;
  }

  public void setFolderId(String folderId) {
    this.folderId = folderId;
  }

  public AddTasksToFolderCommand(Collection<String> tasks, String folderId) {
    this.tasks = tasks;
    this.folderId = folderId;
  }

  @Override
  public String[] buildCommand(String ccmExe) {

    // FIXME max query Length
    String allTasks = StringUtils.join(tasks, ",");

    String[] commands = new String[]{ccmExe, "folder", "-modify", "-add_tasks", allTasks, "-q", folderId};
    return commands;
  }

  @Override
  public void parseResult(String result) {
    // do nothing.
  }
}
