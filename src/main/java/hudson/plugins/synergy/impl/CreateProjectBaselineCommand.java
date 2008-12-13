package hudson.plugins.synergy.impl;

public class CreateProjectBaselineCommand extends Command {
	private String name;
	private String project;
	private String release;
	private String purpose;
		
	public CreateProjectBaselineCommand(String name, String project, String release, String purpose) {
		this.name = name;
		this.project = project;
		this.release = release;
		this.purpose = purpose;
	}
	@Override
	public String[] buildCommand(String ccmExe) {
		String[] commands = new String[]{ccmExe, "baseline", "-create", name, "-p", project, "-r", release, "-purpose", purpose};
		return commands;
	}
	@Override
	public void parseResult(String result) {
		// do nothing.
	}
}
