package hudson.plugins.synergy.impl;

/**
 * Builds a set attribute command.
 */
public class SetProjectAttributeCommand extends Command {
	private String project;
	private String attribute;
	private String value;
	
	public SetProjectAttributeCommand(String project, String attribute, String value) {
		this.project = project;
		this.attribute = attribute;
		this.value = value;
	}
	@Override
	public String[] buildCommand(String ccmExe) {	
		String[] commands = new String[]{ccmExe, "attr", "-m", attribute, "-v", value, "-project", project};
		return commands;		
	}
	
	@Override
	public void parseResult(String result) {
		// do nothing.
	}
	
	@Override
	public boolean isStatusOK(int status, String output) {
		// Don't check empty output (can get a warning if the attribute does not exist)
		return status==0 || status==1;
	}
}
