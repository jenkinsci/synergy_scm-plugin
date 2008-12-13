package hudson.plugins.synergy.impl;

import hudson.FilePath;

import java.io.IOException;
import java.io.OutputStream;

public class WriteObjectCommand extends StreamCommand {
	private String name;
	private FilePath path;
	
	public WriteObjectCommand(String name, FilePath path) {
		this.name = name;
		this.path = path;
	}
	
	@Override
	public String[] buildCommand(String ccmExe) {
		String[] command = new String[]{ccmExe, "cat", name};
		return command;
	}
	@Override
	public OutputStream buildResultOutputer() throws IOException, InterruptedException {		
		OutputStream out = path.write();
		return out;
	}
}
