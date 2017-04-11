package hudson.plugins.synergy.impl;


import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

public class RecursiveProjectQueryCommand extends Command {
	private String project;
	private List<String> subProjects;
	
	public RecursiveProjectQueryCommand(String project) {
		this.project = project;
	}
	
	@Override
	public String[] buildCommand(String ccmExe) {
		String[] commands = new String[] { ccmExe, "query", "-u", "-f", "%displayname", "cvtype='project' and recursive_is_member_of('" + project +"', '')"};
//		String[] commands = new String[] { ccmExe, "query", "-u", "-f", "%displayname", "cvtype='project' and hierarchy_project_members('" + project +"', '')"};
		return commands;
	}
	@Override
	public void parseResult(String result) {
                
		subProjects = new ArrayList<String>();
                if (result != null) {
		try {
			BufferedReader reader = new BufferedReader(new StringReader(result));
			String sousProjet = reader.readLine();
			while (sousProjet!=null) {
				subProjects.add(sousProjet.trim());			
				sousProjet = reader.readLine();				
			}
		} catch (IOException e) {
			// Ignore on StringReader.
		}
	}
	}
	
	public List<String> getSubProjects() {
		return subProjects;
	}
}
