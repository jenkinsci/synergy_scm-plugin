package hudson.plugins.synergy.impl;

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
		return new String[]{ccmExe, "conflicts", "-t", "-r", "-noformat", project};
	}

	@Override
	public void parseResult(String line) {
		line = line.trim();
		if (line.length()==0) {
			return;
		}
		if (line.startsWith("Project")) {
			return;
		}
		if (line.indexOf("No conflicts detected")!=-1) {
			return;
		}
		
		StringTokenizer tokenizer = new StringTokenizer(line, "\t");
		String objectname = tokenizer.nextToken();
		String task = tokenizer.nextToken();
		String message = tokenizer.nextToken();
		int index = message.lastIndexOf("-");
		String type = message.substring(index+1).trim();
		Conflict conflict = new Conflict(objectname, task, type, message);
		conflicts.add(conflict);
	}
	
	public List<Conflict> getConflicts() {
		return conflicts;
	}
}
