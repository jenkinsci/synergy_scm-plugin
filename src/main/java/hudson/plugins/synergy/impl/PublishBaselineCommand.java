package hudson.plugins.synergy.impl;

/**
 * Builds a publish baseline command. 
 */
public class PublishBaselineCommand extends Command {
	private String name;
	
	/**
	 * @param name	The name of the baseline to publish
	 */
	public PublishBaselineCommand(String name) {
		this.name = name;
	}
	
	@Override
	public String[] buildCommand(String ccmExe) {
		String[] commands = new String[]{ccmExe, "baseline", "-publish", name};
		return commands;
	}
	@Override
	public void parseResult(String result) {
		// do nothing.		
	}
}
