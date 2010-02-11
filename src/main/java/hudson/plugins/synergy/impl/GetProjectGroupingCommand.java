package hudson.plugins.synergy.impl;

/**
 * Get project grouping for release and member status.
 */
public class GetProjectGroupingCommand extends Command {
	private String release;	
	private String memberStatus;
	private String subsystem;
	private String projectGrouping;
	
	public GetProjectGroupingCommand(String release, String memberStatus, String subsystem) {
		this.release = release;
		this.memberStatus = memberStatus;
		this.subsystem = subsystem;
	}
	@Override
	public String[] buildCommand(String ccmExe) {	
		String[] commands = new String[] { ccmExe, "query", "-u", "-f", "%objectname", "type='project_grouping' and release='" + release + "' and member_status='" + memberStatus + "' and subsystem='" + subsystem + "'"};
		return commands;			
	}
	@Override
	public void parseResult(String result) {
		projectGrouping = result.trim();
	}
	public String getProjectGrouping() {
		return projectGrouping;
	}
	
}

