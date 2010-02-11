package hudson.plugins.synergy.impl;

/**
 * Builds a get project owner command.
 */
public class GetProjectOwnerCommand extends Command {
	private String project;
	private String owner;
	
	public GetProjectOwnerCommand(String project) {
		this.project = project;
	}
	@Override
	public String[] buildCommand(String ccmExe) {
		String[] commands = new String[]{ccmExe, "attr", "-s", "owner", "-p", project};
		return commands;
	}
	@Override
	public void parseResult(String result) {
		this.owner = result;
	}
	public String getOwner() {
		return owner;
	}
}
