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
		/* 
		 * TODO: 
		 * Due to a bug in Synergy 7, setting attribute maintain_wa by "ccm attr" does not work 
		 * ("cmm attr -m maintain_wa -v TRUE/FALSE -project foobar_int_1.1.1")
		 * 
		 *
		 * This might be fixed in the next weeks, this command might by splitted in two commands:
		 * "ccm wa -set $HOME/ccm_wa @20"
		 * "ccm wa -nwa/-wa -project <projectname>" 
		 *
		 * Executing the last command manually can be used as a workaround.
		 */
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
