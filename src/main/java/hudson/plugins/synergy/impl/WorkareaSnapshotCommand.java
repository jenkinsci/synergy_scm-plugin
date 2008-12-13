package hudson.plugins.synergy.impl;

/**
 * Builds a workarea snapshot command.
 */
public class WorkareaSnapshotCommand extends Command {
	private String project;
	private String path;
	
	
	
	public WorkareaSnapshotCommand(String project, String path) {
		this.path = path;
		this.project = project;
	}

	/**
	 * Builds a workarea snapshot command.
	 * @param project
	 * @return
	 */
	@Override
	public String[] buildCommand(String ccmExe) {
		String[] commands = new String[] {ccmExe, "copy_to_file_system", project, "-path", path, "-r"};
		return commands;
	}
	
	@Override
	public void parseResult(String result) {
		// TODO Auto-generated method stub
		
	}
}
