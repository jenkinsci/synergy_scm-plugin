 package hudson.plugins.synergy.impl;

import java.util.Arrays;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;

public class CreateProjectBaselineCommand extends Command {
	private String name;
	private String project;
	private String release;
	private String purpose;
	
	private static final Logger LOGGER = Logger.getLogger(CreateProjectBaselineCommand.class.getName());
		
	public CreateProjectBaselineCommand(String name, String project, String release, String purpose) {
		this.name = name;
		this.project = project;
		this.release = release;
		this.purpose = purpose;
	}
	@Override
	public String[] buildCommand(String ccmExe) {
		Vector<String> commands = new Vector<String>(Arrays.asList(new String[]{ccmExe, "baseline", "-create", name, "-p", project }));
		if(this.release.trim() != "") {
			commands.add("-r");
			commands.add(release);
		}
		
		if(this.purpose.trim() != "") {
			commands.add("-purpose");
			commands.add(purpose);	
		}
		
		return commands.toArray(new String[]{});
	}
	@Override
	public void parseResult(String result) {
		// do nothing.
	}
}
