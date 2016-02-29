package hudson.plugins.synergy.impl;

import java.util.Arrays;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;

public class CreateProjectBaselineCommand extends Command {
	private String name;
	private String template;
	private String project;
	private String release;
	private String purpose;
	
	private static final Logger LOGGER = Logger.getLogger(CreateProjectBaselineCommand.class.getName());
		
	public CreateProjectBaselineCommand(String name, String template, String project, String release, String purpose) {
		this.name = name;
		this.template = template;
		this.project = project;
		this.release = release;
		this.purpose = purpose;
	}
	@Override
	public String[] buildCommand(String ccmExe) {
		Vector<String> commands = new Vector<String>(Arrays.asList(new String[]{ccmExe, "baseline", "-create", name, "-p", project }));
		if(release != null &&  release.length() > 0 && purpose != null &&  purpose.length() > 0) {
			commands.add("-release");
			commands.add(release);
			commands.add("-purpose");
			commands.add(purpose);	
		}
		if(template != null && template.length() > 0) {
			commands.add("-version_template");
			commands.add(template);	
		}
		return commands.toArray(new String[]{});
	}
	@Override
	public void parseResult(String result) {
		// do nothing.
	}
}
