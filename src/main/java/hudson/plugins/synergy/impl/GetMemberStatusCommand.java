package hudson.plugins.synergy.impl;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * Get member status for a project purpose.
 */
public class GetMemberStatusCommand extends Command {
	private String projectPurpose;
	private String memberStatus;
	
	public GetMemberStatusCommand(String projectPurpose) {
		this.projectPurpose = projectPurpose;
	}
	@Override
	public String[] buildCommand(String ccmExe) {	
		String[] commands = new String[] { ccmExe, "project_purpose", "-show", projectPurpose};
		return commands;			
	}
	@Override
	public void parseResult(String result) {
		try {
			// Creates regexp to extract.
			Pattern p = Pattern.compile("^.*\\Q"+projectPurpose+"\\E\\s+(\\w+)\\s+\\w+\\s*$");
			BufferedReader reader = new BufferedReader(new StringReader(result));
			String line;
			while (memberStatus==null && ((line = reader.readLine())!=null)) {
				line = line.trim();
				if (line.length()!=0) {
					// Look for updates.
					Matcher m = p.matcher(line);
					memberStatus = m.matches() ? m.group(1) : null;
				}
			}
		} catch (IOException e) {
			// Should not happen with a StringReader.
		}
	}
	
	public String getMemberStatus() {
		return memberStatus;
	}
	
}


