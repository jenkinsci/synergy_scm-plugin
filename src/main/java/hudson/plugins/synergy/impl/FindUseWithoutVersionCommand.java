package hudson.plugins.synergy.impl;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Performs finduse on specified object but ignoring the project version.
 */
public class FindUseWithoutVersionCommand extends FindUseCommand {
	
	public FindUseWithoutVersionCommand(String object, Set<String> projects, String delimiter) {
		super(object, projects, delimiter, false);
	}

	@Override
	public void parseResult(String result) {

		// Trim the version out of projects
		List<String> trimmedProjects = new ArrayList<String>();
		for (String project : projects)
		{
			int versionDelimIndex = project.indexOf(delimiter);
			if (versionDelimIndex != -1) {
				trimmedProjects.add(project.substring(0, versionDelimIndex));
			} else {
				trimmedProjects.add(project);
			}
		}

		// Find the object in various projects
		BufferedReader reader = new BufferedReader(new StringReader(result));
		try {
			String line = reader.readLine();
			while (line!=null) {				
				int projectIndex = line.indexOf('@');
				if (projectIndex!=-1) {
					String usingProject = line.substring(projectIndex+1);
					int versionDelimIndex = usingProject.indexOf(delimiter);
					if (versionDelimIndex != -1) {
						usingProject = usingProject.substring(0, versionDelimIndex);
					}
					if (trimmedProjects.contains(usingProject)) {
						path = line.substring(0, line.indexOf(delimiter)).trim();
						break;
					}
				}				
				line = reader.readLine();
			}
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
}
