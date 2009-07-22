package hudson.plugins.synergy.impl;

/**
 * Builds a get attribute command.
 */
public class GetProjectAttributeCommand extends Command {
	public static final String MAINTAIN_WORKAREA = "maintain_wa";
	public static final String RELATIVE = "is_relative";
	public static final String WORKAREA_PATH = "wa_path";
	
	private String attribute;
	private String project;
	private String value;
	
	public GetProjectAttributeCommand(String project, String attribute) {
		super();
		this.attribute = attribute;
		this.project = project;
	}
	@Override
	public String[] buildCommand(String ccmExe) {
		String[] commands = new String[]{ccmExe, "attr", "-s", attribute, "-project", project};
		return commands;		
	}
	@Override
	public void parseResult(String result) {
		this.value = result;
	}
	public String getValue() {
		return value;
	};
	
	@Override
	public boolean isStatusOK(int status, String output) {
		// Don't check empty output (can get a warning if the attribute does not exist)
		return status==0 || status==1;
	}
}
