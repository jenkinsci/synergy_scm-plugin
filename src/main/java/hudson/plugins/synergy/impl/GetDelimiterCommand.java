package hudson.plugins.synergy.impl;


/**
 * Get the database delimiter.
 */
public class GetDelimiterCommand extends Command {
	private String delim;	
	
	@Override
	public String[] buildCommand(String ccmExe) {	
		String[] commands = new String[] { ccmExe, "delim"};
		return commands;			
	}
	@Override
	public void parseResult(String result) {
		if (result != null) {
			delim = result.trim();
		}
	}
	
	public String getDelimiter() {
		return delim;
	}
}
