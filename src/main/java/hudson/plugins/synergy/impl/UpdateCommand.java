package hudson.plugins.synergy.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Builds an update workarea command.
 * 
 * @author jrbe
 */
public class UpdateCommand extends Command {
	public static final String PROJECT = "-p";
	public static final String PROJECT_GROUPING = "-pg";
	
	/**
	 * The type of element to update
	 */
	private String type;
	
	/**
	 * The spec of the project to update.
	 */
	private String project;

	/**
	 * The list of members that have been added to the workarea.
	 */
	private List<String> names;

	/**
	 * The displayname of the project grouping.
	 */
	private String pgName;

	/**
	 * Should the subprojects be replaced?
	 */
	private boolean replaceSubprojects;

	private List<Conflict> conflicts = new ArrayList<Conflict>();

	public UpdateCommand(String type, String project, boolean replaceSubprojects) {
		this.type = type;
		this.project = project;
		this.replaceSubprojects = replaceSubprojects;
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
			names.add(newElement.substring(1, newElement.length() - 1));
		}

		// Look for new elements.
		Matcher mBound = pBoundUnder.matcher(result);
		while (mBound.find()) {
			String group = mBound.group();
			Matcher mObjectNames = pObjectName.matcher(group);
			String newElement = mObjectNames.find() ? mObjectNames.group() : null;
			String elementParent = mObjectNames.find() ? mObjectNames.group() : null;
			names.add(newElement.substring(1, newElement.length() - 1));
		}

		Pattern updateWarningPattern = Pattern.compile("Warning:\\s.*");
		Matcher mUpdateWarningPattern = updateWarningPattern.matcher(result);
		boolean foundWarning = mUpdateWarningPattern.find();
		if (foundWarning) {
			String objectname = "Update Warning found!";
			String task = "No Task";
			String type = "";
			String message = mUpdateWarningPattern.group();

			Conflict conflict = new Conflict(objectname, task, type, message);
			conflicts.add(conflict);
		}

		// Look for project grouping name
		Pattern pgNamePattern = Pattern.compile("Refreshing baseline and tasks for project grouping '([^']+)'");
		Matcher mPgNamePattern = pgNamePattern.matcher(result);
		pgName = mPgNamePattern.find() ? mPgNamePattern.group(1) : null;
}

	@Override
	public String[] buildCommand(String ccmExe) {
		String subprojectUpdateRule = replaceSubprojects ? "-replace_subprojects" :  "-keep_subprojects";		

		String[] commands = new String[] { ccmExe, "update", "-r", subprojectUpdateRule, type, project };
		return commands;
	}

	public List<String> getUpdates() {
		return names;
	}

	public boolean isStatusOK(int status) {
		return status == 0;
	}

	public Collection<Conflict> getConflicts() {
		return conflicts;
	}

	public boolean isUpdateWarningsExists() {
		return !getConflicts().isEmpty();
	}
	
	public String getPgName() {
		return pgName;
	}

}
