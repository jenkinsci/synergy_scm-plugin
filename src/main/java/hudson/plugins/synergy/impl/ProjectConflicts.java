package hudson.plugins.synergy.impl;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

/**
 * Synergy command that checks for object and task conflicts
 * in the specified project and its subprojects.
 */
public class ProjectConflicts extends Command {
	private String project;
	private List<Conflict> conflicts = new ArrayList<Conflict>();
	
	public ProjectConflicts(String project) {
		this.project = project;
	}

	@Override
	public String[] buildCommand(String ccmExe) {
		return new String[]{ccmExe, "conflicts", "-r", "-noformat", project};
	}

	@Override
	public void parseResult(String result) {
		BufferedReader reader = new BufferedReader(new StringReader(result));
		try {
			String line = reader.readLine();
			while (line!=null) {
				line = line.trim();
				if (line.length()!=0 && !line.startsWith("Project:") && line.indexOf("No conflicts detected")==-1) {				
					
					StringTokenizer tokenizer = new StringTokenizer(line, "\t");
					String objectname = tokenizer.nextToken();
					String task = tokenizer.hasMoreTokens() ? tokenizer.nextToken() : "No Task";
					String message = "";
					String type = "";
					if(tokenizer.hasMoreTokens()){
						message = tokenizer.nextToken();
						int index = message.lastIndexOf("-");
						type = message.substring(index+1).trim();
					}
					
					Conflict conflict = new Conflict(objectname, task, type, message);
					conflicts.add(conflict);
				}
				line = reader.readLine();
			}
		} catch (IOException e) {
			// Ignore with a StringReader.
		}
	}
	
	public List<Conflict> getConflicts() {
		return conflicts;
	}
}
