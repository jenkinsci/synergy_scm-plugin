package hudson.plugins.synergy.impl;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GetBaselineMatchingCommand extends Command {
	private String release;
	String purpose;
	String baseline;
	
	public GetBaselineMatchingCommand(String release, String purpose) {
		this.release = release; 
		this.purpose = purpose;
	}
	
  @Override
	public String[] buildCommand(String ccmExe) {
		return new String[] { ccmExe, "process_rule", "-show", "matching", release + ":" + purpose};
	}
	
  @Override
	public void parseResult(String result) {
		Matcher m = Pattern.compile("Process Rule " + release + ":" + purpose + ": " + "(\\S*)").matcher(result);
		if(m.find()) baseline = m.group(1);
	}

	public String getBaseline() {
		return baseline;
	}
}

