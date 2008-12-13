package hudson.plugins.synergy.impl;


public abstract class Command {	
	public abstract String[] buildCommand(String ccmExe);
	public abstract void parseResult(String result);
	
	public boolean[] buildMask() {
		return new boolean[buildCommand(null).length];
	}
}
