package hudson.plugins.synergy.impl;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

/**
 * Finds the projects in a project grouping.
 * @author jrbe
 */
public class FindProjectInProjectGrouping extends Command {
	/**
	 * The project grouping.
	 */
	private String projectGrouping;
	
	/**
	 * The founded projects.
	 */
	private List<String> projects;
	
	public FindProjectInProjectGrouping(String projectGrouping) {
		this.projectGrouping = projectGrouping;
	}
	
	@Override
	public String[] buildCommand(String ccmExe) {
		String[] commands = new String[] { ccmExe, "query", "-t", "project", "-u", "-f", "%objectname", "has_project_grouping('" + projectGrouping + "')" };
		return commands;
	}
	@Override
	public void parseResult(String result) {
		projects = new ArrayList<String>();
		try {
			BufferedReader reader = new BufferedReader(new StringReader(result));
			String sousProjet = reader.readLine();
			while (sousProjet!=null) {
				projects.add(sousProjet.trim());			
				sousProjet = reader.readLine();				
			}
		} catch (IOException e) {
			// Ignore on StringReader.
		}
	}
	
	public List<String> getProjects() {
		return projects;
	}
}
