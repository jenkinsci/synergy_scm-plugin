package hudson.plugins.synergy.impl;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
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
  
  public FindProjectGroupingCommand(String release, String purpose, String owner) {
		this.release = release;
		this.purpose = purpose;
    this.owner = owner;
	}
	
	@Override
	public String[] buildCommand(String ccmExe) {
		List<String> commands = new ArrayList(Arrays.asList(ccmExe, "pg", "-l", "-u", "-f", "%objectname", "-r", release, "-purpose", purpose ));
    if (owner != null) {
      commands.add("-owner");
      commands.add(owner);
    }
		return commands.toArray(new String[0]);
	}
	@Override
	public void parseResult(String result) {
                if (result != null) {
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
	}
	
	public List<String> getProjectGroupings() {
		return projectGroupings;
	}
}
