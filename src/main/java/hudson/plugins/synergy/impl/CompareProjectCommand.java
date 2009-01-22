package hudson.plugins.synergy.impl;

import java.util.ArrayList;
import java.util.List;

/**
 * Builds a compare project command.
 */
public class CompareProjectCommand extends Command {
	private String newProject;
	private String oldProject;
	
	private List<String> differences;
	
	public CompareProjectCommand(String newProject, String oldProject) {
		this.newProject = newProject;
		this.oldProject = oldProject;
	}
	
	@Override
	public String[] buildCommand(String ccmExe) {		
		String[] query = new String[] {
			ccmExe, "query", "\"type!='project' and type!='dir' and is_member_of('" + newProject+ "') and not is_member_of('" + oldProject+ "')\"", 
			"-u",
			"-f",
			"%objectname"				
		};
		return query;	
	}
	@Override
	public void parseResult(String result) {
		String[] resultAsArray = result.split("\n"); // TODO this leaves the result with \r at the end of the String on Windows 
		differences = new ArrayList<String>(resultAsArray.length);
		for (String difference : resultAsArray) {
			String trim = difference.trim();
			if (trim.length()>0) {
				differences.add(trim);
			}
		}
	}
	
	/**
	 * Returns the objectname of the objects that are members of the newProject but not of the oldProject. 
	 */
	public List<String> getDifferences() {
		return differences;
	}
}
