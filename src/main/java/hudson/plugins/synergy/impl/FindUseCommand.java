package hudson.plugins.synergy.impl;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.Set;

/**
 * Builds the path of a file in project command.
 */
public class FindUseCommand extends Command {
	/**
	 * The Synergy object we want to find the use.
	 */
	private String object;
	
	/**
	 * The Synergy projects we are interesting in knowing the use they makes of the object.
	 */
	private Set<String> projects;
	
	/**
	 * The Synergy version delimiter.
	 */
	private String delimiter;
	
	private String path;
	
	public FindUseCommand(String object, Set<String> projects, String delimiter) {
		super();
		this.object = object;
		this.projects = projects;
		this.delimiter = delimiter;
	}
	@Override
	public String[] buildCommand(String ccmExe) {	
		String[] commands = new String[] { ccmExe, "finduse", object };
		return commands;			
	}
	@Override
	public void parseResult(String result) {
		BufferedReader reader = new BufferedReader(new StringReader(result));
		try {
			String line = reader.readLine();
			while (line!=null) {				
				int projectIndex = line.indexOf('@');
				if (projectIndex!=-1) {
					String usingProject = line.substring(projectIndex+1);
					if (projects.contains(usingProject)) {
						path = line.substring(0, line.indexOf(delimiter)).trim();
						break;
					}
				}				
				line = reader.readLine();
			}
		} catch (IOException e) {
			
		}
	}
	
	/**
	 * Returns the path of the object in the project.
	 * The path begins with the project name and ends with the object name.
	 */
	public String getPath() {
		return path;
	}
}
