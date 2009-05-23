package hudson.plugins.synergy.impl;

public class CopyFolderCommand extends Command {
		
	private String fromFolder;
	private String toFolder;
	
	public CopyFolderCommand(String fromFolder, String toFolder) {
		this.fromFolder = fromFolder;
		this.toFolder = toFolder;
	}
	@Override
	public String[] buildCommand(String ccmExe) {
		String[] commands = new String[]{ccmExe, "folder", "-copy", fromFolder, "-existing", toFolder};
		return commands;
	}
	@Override
	public void parseResult(String result) {
		// do nothing.
	}
}
