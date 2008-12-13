package hudson.plugins.synergy.impl;

import java.io.IOException;
import java.io.OutputStream;

public abstract class StreamCommand {
	public abstract String[] buildCommand(String ccmExe);
	public abstract OutputStream buildResultOutputer() throws IOException, InterruptedException;
	
	public boolean[] buildMask() {
		return new boolean[buildCommand(null).length];
	}
}
