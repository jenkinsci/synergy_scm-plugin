package hudson.plugins.synergy.impl;

public class SetRoleCommand extends Command {
	public static final String BUILD_MANAGER = "build_mgr";
	
	private String role;
	
	public SetRoleCommand(String role) {
		this.role = role;
	}

	/**
	 * Builds a set role command.
	 */
	@Override
	public String[] buildCommand(String ccmExe) {		
		String[] commands = new String[]{ccmExe, "set", "role", role};
		return commands;	
	}
	
	@Override
	public void parseResult(String result) {
		// do nothing.		
	}
}
