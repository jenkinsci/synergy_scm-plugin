package hudson.plugins.synergy.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Builds an update workarea command.
 * @author jrbe
 */
public class UpdateCommand extends Command {
	/**
	 * The spec of the project to update.
	 */
	private String project;
	
	/**
	 * The list of members that have been added to the workarea.
	 */
	private List<String> names;
	
	public UpdateCommand(String project) {
		this.project = project;
	}

	@Override
	public void parseResult(String result) {
		// List of elements found.
		names = new ArrayList<String>();
		
		// Creates regexps for what we are looking for in the log.
		Pattern pReplaces = Pattern.compile("'[^']+'\\sreplaces\\s'[^']+'\\sunder\\s'[^']+'");
		Pattern pBoundUnder = Pattern.compile("'[^']+'\\sis\\snow\\sbound\\sunder\\s'[^']+'");
		Pattern pObjectName = Pattern.compile("'[^']+'");
		
		// Look for updates.
		Matcher mReplaces = pReplaces.matcher(result);
		while (mReplaces.find()) {
			String group = mReplaces.group();
			Matcher mObjectNames = pObjectName.matcher(group);
			String newElement = mObjectNames.find() ? mObjectNames.group() : null;
			String oldElement = mObjectNames.find() ? mObjectNames.group() : null;
			String elementParent = mObjectNames.find() ? mObjectNames.group() : null;
			names.add(newElement.substring(1, newElement.length()-1));
		}
		
		// Look for new elements.
		Matcher mBound = pBoundUnder.matcher(result);
		while (mBound.find()) {
			String group = mBound.group();
			Matcher mObjectNames = pObjectName.matcher(group);
			String newElement = mObjectNames.find() ? mObjectNames.group() : null;
			String elementParent = mObjectNames.find() ? mObjectNames.group() :null;
			names.add(newElement.substring(1, newElement.length()-1));
		}			
	}
	
	@Override
	public String[] buildCommand(String ccmExe) {
		String[] commands = new String[] {ccmExe, "update", "-r", "-rs", "-p", project};
		return commands;
	}

	public List<String> getUpdates() {
		return names;
	}
	
	public boolean isStatusOK(int status) {
		return status==0;
	}
}
