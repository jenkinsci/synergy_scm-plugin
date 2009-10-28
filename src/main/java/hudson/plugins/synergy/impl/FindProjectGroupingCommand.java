package hudson.plugins.synergy.impl;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

/**
 * Finds project groupings.
 * @author jrbe
 */
public class FindProjectGroupingCommand extends Command {
	/**
	 * The project grouping release.
	 */
	private String release;
	
	/**
	 * The project grouping purpose.
	 */
	private String purpose;
	
	/**
	 * The project grouping owner.
	 */
	private String owner;
	
	/**
	 * The founded projects.
	 */
	private List<String> projectGroupings;
	
	public FindProjectGroupingCommand(String release, String purpose) {
		this.release = release;
		this.purpose = purpose;
	}
	
	@Override
	public String[] buildCommand(String ccmExe) {
		String[] commands = new String[] { ccmExe, "pg", "-l", "-u", "-f", "%objectname", "-r", release, "-purpose", purpose };
		return commands;
	}
	@Override
	public void parseResult(String result) {
		projectGroupings = new ArrayList<String>();
		try {
			BufferedReader reader = new BufferedReader(new StringReader(result));
			String sousProjet = reader.readLine();
			while (sousProjet!=null) {
				projectGroupings.add(sousProjet.trim());			
				sousProjet = reader.readLine();				
			}
		} catch (IOException e) {
			// Ignore on StringReader.
		}
	}
	
	public List<String> getProjectGroupings() {
		return projectGroupings;
	}
}
