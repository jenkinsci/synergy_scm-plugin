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
			BufferedReader reader = new BufferedReader(new StringReader(result));
			String line = reader.readLine();
			int linecount = 0;
			while (line!=null) {
				linecount++;
				line = line.trim();
				if (line.length()!=0) {
					if (linecount >= 2){
						// Creates regexp to extract.
						Pattern p = Pattern.compile("^.*\\S*\\s+(\\w+)\\s+\\w+\\s*$");

						// Look for updates.
						Matcher m = p.matcher(line);
						memberStatus = m.find() ? m.group(1) : null;
						if (memberStatus != null){
						  break;
						}
					}
				}
				line = reader.readLine();
			}
		} catch (IOException e) {
			// Should not happen with a StringReader.
		}
	}
	
	public String getMemberStatus() {
		return memberStatus;
	}
	
}


