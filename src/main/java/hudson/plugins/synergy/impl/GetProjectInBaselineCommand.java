package hudson.plugins.synergy.impl;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

public class GetProjectInBaselineCommand extends Command {
	private String baseline;
	private List<String> projects = new ArrayList<String>();
	
	public GetProjectInBaselineCommand(String baselineObjectName) {
		this.baseline = baselineObjectName;
	}

	@Override
	public String[] buildCommand(String ccmExe) {
		String[] commands = new String[]{ccmExe, "query", "-u", "-f", "%objectname", "is_project_in_baseline_of('" + baseline + "')"};
		return commands;
	}

	@Override
	public void parseResult(String result) {
		BufferedReader reader = new BufferedReader(new StringReader(result));
		String line;
		try {
			line = reader.readLine();		
			while (line!=null) {
				projects.add(line);
				line = reader.readLine();
			}
		} catch (IOException e) {
			// Should not happen whith a StringReader.
		}
		
	}
	
	public List<String> getProjects() {
		return projects;
	}
}
