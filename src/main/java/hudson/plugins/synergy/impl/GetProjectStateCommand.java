package hudson.plugins.synergy.impl;

/**
 * Builds a get project state command.
 */
public class GetProjectStateCommand extends Command {
	private String project;
	private String state;
	
	public GetProjectStateCommand(String project) {
		this.project = project;
	}
	@Override
	public String[] buildCommand(String ccmExe) {
		String[] commands = new String[]{ccmExe, "attr", "-s", "status", "-p", project};
		return commands;
	}
	@Override
	public void parseResult(String result) {
		this.state = result;
	}
	public String getState() {
		return state;
	}
}
