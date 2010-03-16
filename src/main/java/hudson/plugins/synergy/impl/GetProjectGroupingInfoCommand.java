package hudson.plugins.synergy.impl;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

/**
 * Get project grouping information.
 */
public class GetProjectGroupingInfoCommand extends Command {
	private String pgSpec;
	private String release;	
	private String projectPurpose;	

	Pattern re_release = Pattern.compile("^.*Release:\\s*(..*)$");
	Pattern re_purpose = Pattern.compile("^.*Purpose:\\s*(..*)$");
	
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
					Matcher m_purpose = re_purpose.matcher(line);
					if (m_purpose.matches()){
						projectPurpose = m_purpose.group(1);
					}
					Matcher m_release = re_release.matcher(line);
					if (m_release.matches()){
						release = m_release.group(1);
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

