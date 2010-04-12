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

		/* 
		 * Due to a bug in Synergy 7, setting attribute maintain_wa by "ccm attr" does not work with web communication mode.
       * 
		 * The following command should work with all synergy releases.
       */
		if (attribute.equals("maintain_wa")){
		   String maintain_wa;
		   if (value.equals("TRUE")){
				maintain_wa = "-wa";
			}else{
				maintain_wa = "-nwa";
			}
			commands = new String[]{ccmExe, "wa", maintain_wa , "-project", project};
		}
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
