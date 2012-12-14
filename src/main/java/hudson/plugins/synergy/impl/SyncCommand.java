package hudson.plugins.synergy.impl;

/**
 * Builds a sync command.
 *
 * @author ccosby@gmail.com
 */
public class SyncCommand extends Command {
	/**
	 * The spec of the project to sync.
	 */
	private String project;

    /**
     * Whether to recursively sync or not. Default is recurse.
     */
    private boolean recurse;

	public SyncCommand (String project, boolean recurse) {
		this.project = project;
        this.recurse = recurse;
	}

	@Override
	public void parseResult(String result) { }

	@Override
	public String[] buildCommand(String ccmExe) {
		String recurseRule = recurse ? "-recurse" : "-no_recurse";

		String[] commands = new String[] { ccmExe, "sync", recurseRule, "-project", project };
		return commands;
	}
}
