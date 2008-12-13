package hudson.plugins.synergy.impl;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;

/**
 * Builds the path of a file in project command.
 */
public class FindUseCommand extends Command {
	private String object;
	private String project;
	private String delimiter;
	private String path;
	
	public FindUseCommand(String object, String project, String delimiter) {
		super();
		this.object = object;
		this.project = project;
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
		String lookedFor = "@" + project;
		try {
			String line = reader.readLine();
			while (line!=null) {				
				int start = line.indexOf(lookedFor);
				if (start!=-1) {
					path = line.substring(0, line.indexOf(delimiter)).trim();
					break;
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
