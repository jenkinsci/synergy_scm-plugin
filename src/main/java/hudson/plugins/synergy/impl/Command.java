package hudson.plugins.synergy.impl;

/**
 * Synergy text command returning some text.
 */
public abstract class Command {
	/**
	 * Return the Synergy command line to execute as an array.
	 * The line must start with the ccmExe location, which is given as the first method parameter. 
	 */
	public abstract String[] buildCommand(String ccmExe);
	
	/**
	 * Parse the command result.
	 */
	public abstract void parseResult(String result);
	
	/**
	 * Build a mask of values in the command line that should not be logged.
	 */
	public boolean[] buildMask() {
		return new boolean[buildCommand(null).length];
	}
	
	/**
	 * Return true if the given return status is ok for the command.
	 * The default is to return true if the status is 0 or 1. 
	 */
	public boolean isStatusOK(int status) {
		return status==0 || status==1;
	}
}
