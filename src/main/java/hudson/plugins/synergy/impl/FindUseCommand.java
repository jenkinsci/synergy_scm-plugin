package hudson.plugins.synergy.impl;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.commons.lang.StringUtils;

/**
 * Builds the path of a file in project command.
 */
public class FindUseCommand extends Command {

  /**
   * The Synergy objects we want to find the use.
   */
  private List<String> object;
  /**
   * The Synergy projects we are interesting in knowing the use they makes of the object.
   */
  protected Set<String> projects;
  /**
   * The Synergy version delimiter.
   */
  protected String delimiter;
  /**
   * Is the object a project.
   */
  private boolean project;
  protected Map<String, String> path = new HashMap<String, String>();

  /**
   * Public constructor
   *
   * @param object	The object we want to find the use in other projects
   * @param projects	The projects we want to kown if and where they contains the object
   * @param delimiter	The version delimiter
   * @param project	Is the object we want to find the use is a project
   */
  public FindUseCommand(List<String> object, Set<String> projects, String delimiter, boolean project) {
    super();
    this.object = object;
    this.projects = projects;
    this.delimiter = delimiter;
    this.project = project;
  }

  @Override
  public String[] buildCommand(String ccmExe) {
    List<String> commands = new ArrayList<String>();
    commands.add(ccmExe);
    commands.add("finduse");
    if (project) {
      commands.add("-p");
    }
    for (String l_object : object) {
      commands.add("\"" + l_object + "\"");
    }

    return commands.toArray(new String[0]);
  }

  @Override
  public void parseResult(String result) {
    if (result != null) {
      BufferedReader reader = new BufferedReader(new StringReader(result));
      try {
        String line = reader.readLine();
        while (line != null) {
          int projectIndex = line.indexOf('@');
          if (projectIndex != -1) {
            String usingProject = line.substring(projectIndex + 1);
            if (projects.contains(usingProject)) {
              String l_trim = line.substring(0, line.indexOf(delimiter)).trim();
              path.put(StringUtils.substringAfterLast(l_trim, File.separator), l_trim);
            }
          }
          line = reader.readLine();
        }
      } catch (IOException e) {
      }
    }
  }

  /**
   * Returns the path of the object in the project. The path begins with the project name and ends with the object name.
   */
  public Map<String, String> getPath() {
    return path;
  }
}
