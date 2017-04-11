package hudson.plugins.synergy.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.lang.StringUtils;

/**
 * Builds an update workarea command.
 *
 * @author jrbe
 */
public class UpdateCommand extends Command {

  public static final String PROJECT = "-p";
  public static final String PROJECT_GROUPING = "-pg";

  /**
   * The type of element to update
   */
  private String type;

  /**
   * The spec of the project to update.
   */
  private String project;

  /**
   * The list of members that have been added to the workarea. Mapping new
   * objectname , oldobjectname
   */
  private HashMap<String, List<List<String>>> names;

  private Set<String> removedTasks = new HashSet<>();

  /**
   * The displayname of the project grouping.
   */
  private String pgName;

  /**
   * Should the subprojects be replaced?
   */
  private boolean replaceSubprojects;

  private List<Conflict> conflicts = new ArrayList<Conflict>();

  public UpdateCommand(String type, String project, boolean replaceSubprojects) {
    this.type = type;
    this.project = project;
    this.replaceSubprojects = replaceSubprojects;
  }

  @Override
  public void parseResult(String result) {
    // List of elements found.
    names = new HashMap<String, List<List<String>>>();

    // Creates regexps for what we are looking for in the log.
    Pattern pReplaces = Pattern.compile("'([^']+)'\\sreplaces\\s'([^']+)'\\sunder\\s'([^']+)'");
    Pattern pBoundUnder = Pattern.compile("'([^']+)'\\sis\\snow\\sbound\\sunder\\s'([^']+)'");
    Pattern pUpdateComplete = Pattern.compile("Update for '([^']+)' complete");
    Pattern pRemovedTasks = Pattern.compile("\\sRemoved the following tasks\\s");

    Matcher mUpdateComplete = pUpdateComplete.matcher(result);
    int start = 0;
    while (mUpdateComplete.find()) {
      List<List<String>> pnames = new ArrayList<List<String>>();
      String l_project = mUpdateComplete.group(1);
      names.put(l_project, pnames);
      int end = mUpdateComplete.end();
      String subresult = result.substring(start, end);
      start = end;

      // Look for updates.
      Matcher mReplaces = pReplaces.matcher(subresult);
      while (mReplaces.find()) {
        String group = mReplaces.group();
        String newElement = mReplaces.group(1);
        String oldElement = mReplaces.group(2);
        String elementParent = mReplaces.group(3);
        pnames.add(Arrays.asList(newElement, oldElement));
      }

      // Look for new elements.
      Matcher mBound = pBoundUnder.matcher(subresult);
      while (mBound.find()) {
        String group = mBound.group();
        String newElement = mBound.group(1);
        String elementParent = mBound.group(2);
        pnames.add(Arrays.asList(newElement, newElement));
      }

    }

    Pattern updateWarningPattern = Pattern.compile("Warning:\\s.*");
    Matcher mUpdateWarningPattern = updateWarningPattern.matcher(result);
    boolean foundWarning = mUpdateWarningPattern.find();
    if (foundWarning) {
      String objectname = "Update Warning found!";
      String task = "No Task";
      String type = "";
      String message = mUpdateWarningPattern.group();

      Conflict conflict = new Conflict(objectname, task, type, message);
      conflicts.add(conflict);
    }

    // Look for project grouping name
    Pattern pgNamePattern = Pattern.compile("Refreshing baseline and tasks for project grouping '([^']+)'");
    Matcher mPgNamePattern = pgNamePattern.matcher(result);
    pgName = mPgNamePattern.find() ? mPgNamePattern.group(1) : null;

    Matcher removedTasksMatcher = pRemovedTasks.matcher(result);
    while (removedTasksMatcher.find()) {
      int end = removedTasksMatcher.end();
      String substringRemovedLine = result.substring(end, result.indexOf("\r\n",end));
      if (!substringRemovedLine.contains("because they are in the baseline")) {
        String substring = result.substring(result.indexOf("\r\n", end) + 2);
        String[] split = StringUtils.splitPreserveAllTokens(substring, "\r\n");
        for (String line : split) {
          if (StringUtils.isEmpty(line)) {
            break;
          } else if (line.contains("Task MPH#")) {
            removedTasks.add(line.substring(line.indexOf("MPH#"), line.indexOf(":")));
          }
        }
      }
    }
  }

  @Override
  public String[] buildCommand(String ccmExe) {
    String subprojectUpdateRule = replaceSubprojects ? "-replace_subprojects" : "-keep_subprojects";

    String[] commands = new String[]{ccmExe, "update", "-r", subprojectUpdateRule, type, project};
    return commands;
  }

  public Map<String, List<List<String>>> getUpdates() {
    return names;
  }

  public boolean isStatusOK(int status) {
    return status == 0;
  }

  public Collection<Conflict> getConflicts() {
    return conflicts;
  }

  public boolean isUpdateWarningsExists() {
    return !getConflicts().isEmpty();
  }

  public String getPgName() {
    return pgName;
  }

  /**
   * @return the removedTasks
   */
  public Set<String> getRemovedTasks() {
    return removedTasks;
  }

}
