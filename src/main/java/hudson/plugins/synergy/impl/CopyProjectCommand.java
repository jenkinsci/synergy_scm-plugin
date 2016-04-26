package hudson.plugins.synergy.impl;

/**
 * Builds a workarea snapshot command.
 */
public class CopyProjectCommand extends Command {

    private String sourceProject;
    private String version;
    private String path;
    private String purpose;
    private String release;

    public CopyProjectCommand(String p_purpose, String p_release, String p_path, String p_version, String p_sourceProject) {
        this.purpose = p_purpose;
        this.release = p_release;
        this.path = p_path;
        this.version = p_version;
        this.sourceProject = p_sourceProject;
    }

    /**
     * Builds a workarea snapshot command.
     *
     *
     *
     * @param ccmExe
     * @return String[]
     */
    @Override
    public String[] buildCommand(String ccmExe) {
        String[] commands = new String[]{ccmExe, "copy_project", "-purpose", "\"" + purpose + "\"", "-release", "\"" + release + "\"", "-path", "\"" + path + "\"", "-to", "\"" + version + "\"", "\"" + sourceProject + "\""};
        return commands;
    }

    @Override
    public void parseResult(String result) {
        // nothing to do

    }
}
