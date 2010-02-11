package hudson.plugins.synergy.impl;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

public class FindAssociatedTaskCommand extends Command {
	private String objectname;
	private String projectGrouping;
	private List<String> tasks;
	
	public FindAssociatedTaskCommand(String objectname) {
		this.objectname = objectname;
		this.projectGrouping = null;
	}
	
	public FindAssociatedTaskCommand(String objectname, String projectGrouping) {
		this.objectname = objectname;
		this.projectGrouping = projectGrouping;
	}
	
	@Override
	public String[] buildCommand(String ccmExe) {
		if (projectGrouping != null) {
			return new String[]{ccmExe, "query", "-u", "-f", "%objectname", "has_associated_cv('" + objectname + "') and (is_saved_task_in_pg_of('" + projectGrouping + "') or is_added_task_in_pg_of('" + projectGrouping + "'))"};
		} else {
			return new String[]{ccmExe, "query", "-u", "-f", "%objectname", "has_associated_cv('" + objectname + "')"};
		}
	}
	@Override
	public void parseResult(String result) {
		tasks = new ArrayList<String>(1);
		try {
			BufferedReader reader = new BufferedReader(new StringReader(result));
			String line = reader.readLine();
			while (line!=null) {
				line = line.trim();
				if (line.length()!=0) {
					tasks.add(line);
				}
				line = reader.readLine();
			}
		} catch (IOException e) {
			// Should not happen with a StringReader.
		}
	}
	
	public List<String> getTasks() {
		return tasks;
	}
}
