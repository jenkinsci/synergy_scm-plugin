package hudson.plugins.synergy.impl;

/**
 * Builds a stop session command.
 */
public class StopCommand extends Command {
	@Override
	public String[] buildCommand(String ccmExe) {	
		return new String[]{ccmExe, "stop"};		
	}
	@Override
	public void parseResult(String result) {
		// do nothing.
	}
}
