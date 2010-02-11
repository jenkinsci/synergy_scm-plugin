package hudson.plugins.synergy.impl;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;


/**
 * Get project grouping information.
 */
public class GetProjectGroupingInfoCommand extends Command {
	private String pgSpec;
	private String release;	
	private String projectPurpose;	
	
	public GetProjectGroupingInfoCommand(String pgSpec) {
		this.pgSpec = pgSpec;
	}
	@Override
	public String[] buildCommand(String ccmExe) {	
		String[] commands = new String[] { ccmExe, "project_grouping", "-show", "info", pgSpec};
		return commands;			
	}
	@Override
	public void parseResult(String result) {
		try {
			BufferedReader reader = new BufferedReader(new StringReader(result));
			String line = reader.readLine();
			int linecount = 0;
			while (line!=null) {
				linecount++;
				line = line.trim();
				if (line.length()!=0) {
					if (linecount == 2){
						release = line;
					}
					if (linecount == 3){
						projectPurpose = line;
					}
				}
				line = reader.readLine();
			}
		} catch (IOException e) {
			// Should not happen with a StringReader.
		}
	}
	public String getRelease() {
		return release;
	}
	public String getProjectPurpose() {
		return projectPurpose;
	}
	
}

