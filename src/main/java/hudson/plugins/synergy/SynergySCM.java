package hudson.plugins.synergy;

import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.BuildListener;
import hudson.model.TaskListener;
import hudson.plugins.synergy.SynergyChangeLogSet.LogEntry;
import hudson.plugins.synergy.SynergyChangeLogSet.Path;
import hudson.plugins.synergy.impl.CheckoutResult;
import hudson.plugins.synergy.impl.Commands;
import hudson.plugins.synergy.impl.CompareProjectCommand;
import hudson.plugins.synergy.impl.Conflict;
import hudson.plugins.synergy.impl.FindAssociatedTaskCommand;
import hudson.plugins.synergy.impl.FindCompletedSinceDateCommand;
import hudson.plugins.synergy.impl.FindProjectGroupingCommand;
import hudson.plugins.synergy.impl.FindProjectInProjectGrouping;
import hudson.plugins.synergy.impl.FindUseCommand;
import hudson.plugins.synergy.impl.FindUseWithoutVersionCommand;
import hudson.plugins.synergy.impl.GetDelimiterCommand;
import hudson.plugins.synergy.impl.GetMemberStatusCommand;
import hudson.plugins.synergy.impl.GetProjectAttributeCommand;
import hudson.plugins.synergy.impl.GetProjectGroupingCommand;
import hudson.plugins.synergy.impl.GetProjectGroupingInfoCommand;
import hudson.plugins.synergy.impl.GetProjectInBaselineCommand;
import hudson.plugins.synergy.impl.GetProjectOwnerCommand;
import hudson.plugins.synergy.impl.GetProjectStateCommand;
import hudson.plugins.synergy.impl.RecursiveProjectQueryCommand;
import hudson.plugins.synergy.impl.ProjectConflicts;
import hudson.plugins.synergy.impl.SetProjectAttributeCommand;
import hudson.plugins.synergy.impl.SetRoleCommand;
import hudson.plugins.synergy.impl.SubProjectQueryCommand;
import hudson.plugins.synergy.impl.SynergyException;
import hudson.plugins.synergy.impl.TaskCompleted;
import hudson.plugins.synergy.impl.TaskInfoCommand;
import hudson.plugins.synergy.impl.TaskShowObjectsCommand;
import hudson.plugins.synergy.impl.UpdateCommand;
import hudson.plugins.synergy.impl.WorkareaSnapshotCommand;
import hudson.plugins.synergy.impl.WriteObjectCommand;
import hudson.plugins.synergy.util.SessionUtils;
import hudson.scm.ChangeLogParser;
import hudson.scm.SCM;
import hudson.scm.SCMDescriptor;
import hudson.util.FormValidation;
import hudson.util.Secret;
import static hudson.Util.fixEmpty;
import hudson.scm.PollingResult;
import hudson.scm.SCMRevisionState;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Serializable;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.sf.json.JSONObject;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;

/**
 * Synergy SCM
 * 
 * @author Jean-Noel RIBETTE
 */
public class SynergySCM extends SCM implements Serializable {
	public static final class DescriptorImpl extends SCMDescriptor<SynergySCM> {
		@Extension
		public static final DescriptorImpl DESCRIPTOR = new DescriptorImpl();

		/**
		 * Path to ccm executable.
		 */
		private String ccmExe;

		/**
		 * Path to ccm_ui log.
		 */
		private String ccmUiLog;

		/**
		 * Path to ccm_eng log.
		 */
		private String ccmEngLog;

		/**
		 * Path name.
		 */
		private String pathName;

		public String getCcmEngLog() {
			return ccmEngLog;
		}

		public String getCcmUiLog() {
			return ccmUiLog;
		}

		public String getPathName() {
			return pathName;
		}

		private DescriptorImpl() {
			super(SynergySCM.class, null);
			load();
		}

		@Override
		public boolean configure(StaplerRequest request, JSONObject formData) throws FormException {
			ccmExe = request.getParameter("synergy.ccmExe");
			ccmUiLog = request.getParameter("synergy.ccmUiLog");
			ccmEngLog = request.getParameter("synergy.ccmEngLog");
			pathName = request.getParameter("synergy.pathName");
			save();
			return true;
		}

		@Override
		public SCM newInstance(StaplerRequest req, JSONObject formData) throws FormException {
			return new SynergySCM(
					req.getParameter("synergy.project"), 
					req.getParameter("synergy.database"), 
					req.getParameter("synergy.release"), 
					req.getParameter("synergy.purpose"), 
					req.getParameter("synergy.username"), 
					req.getParameter("synergy.password"), 
					req.getParameter("synergy.engine"), 
					req.getParameter("synergy.oldProject"), 
					req.getParameter("synergy.baseline"), 
					req.getParameter("synergy.oldBaseline"), 
					req.getParameter("synergy.ccmHome"),
					"true".equals(req.getParameter("synergy.remoteClient")), 
					"true".equals(req.getParameter("synergy.detectConflict")), 
					"true".equals(req.getParameter("synergy.replaceSubprojects")), 
					"true".equals(req.getParameter("synergy.checkForUpdateWarnings")),
					"true".equals(req.getParameter("synergy.leaveSessionOpen")),
					"true".equals(req.getParameter("synergy.maintainWorkarea")),
					"true".equals(req.getParameter("synergy.checkTaskModifiedObjects")));
		}

		/**
		 * Checks if ccm executable exists.
		 */
		public FormValidation doCcmExeCheck(@QueryParameter String value) {
			return FormValidation.validateExecutable(value);
		}

		@Override
		public String getDisplayName() {
			return "Synergy";
		}

		public String getCcmExe() {
			if (ccmExe == null) {
				return "ccm";
			} else {
				return ccmExe;
			}
		}
	}

	public static final String CCM_SESSION_MAP_FILE_NAME = "ccmSessionMap.properties";

	/**
	 * The Synergy project name.
	 * This is the raw project name : 
	 * if the project name contains variable, they are not substituted here.
	 */
	private String project;

	/**
	 * The Synergy database containing the project.
	 */
	private String database;

	/**
	 * The Synergy projet release. 
	 * This is used to create baseline.
	 */
	private String release;

	/**
	 * The Synergy project purpose. 
     * This is used to create baseline.
	 */
	private String purpose;

	/**
	 * The Synergy engine host name.
	 */
	private String engine;

	/**
	 * The user login
	 */
	private String username;

	/**
	 * The user password.
	 */
	private final Secret password;

	/**
	 * The old project for differential delivery.
	 */
	private String oldProject;

	/**
	 * The baseline
	 */
	private String baseline;

	/**
	 * The old baseline
	 */
	private String oldBaseline;

	/**
	 * Remote client connection flag.
	 */
	private boolean remoteClient;

	/**
	 * Detect conflict flag.
	 */
	private boolean detectConflict;	

	/**
	 * Deep detect changes flag.
	 * Deep detect will check wich project is modified by a task.
	 */
	private boolean checkTaskModifiedObjects;

	private transient ThreadLocal<Commands> commands;

	/**
	 * Should subprojects be replaced?
	 */
	private boolean replaceSubprojects;
	
	/**
	 * Close session after update
	 */
	private boolean leaveSessionOpen;

	/**
	 * Abort the build if there are updateWarnings.
	 */
	private boolean checkForUpdateWarnings;
	
	/**
	 * Maintain a workarea for the project.
	 * The default value "null" is matched to "true" 
	 */
	private Boolean maintainWorkarea;
	
	/**
	 * The CCM_HOME location on UNIX systems.
	 * This is used to locate ccm executable and set the remote
	 * UNIX environment.
	 */
	private String ccmHome;
	
	/**
	 * 
	 * @param project
	 * @param database
	 * @param release
	 * @param purpose
	 * @param username
	 * @param password
	 * @param engine
	 * @param oldProject
	 * @param baseline
	 * @param oldBaseline
	 * @param ccmHome
	 * @param remoteClient
	 * @param detectConflict
	 * @param replaceSubprojects
	 * @param checkForUpdateWarnings
	 * @param leaveSessionOpen
	 * @param maintainWorkarea
	 */
	@DataBoundConstructor
	public SynergySCM(String project, String database, String release, String purpose, String username, String password, String engine,
			String oldProject, String baseline, String oldBaseline, String ccmHome, boolean remoteClient, boolean detectConflict, 
			boolean replaceSubprojects, boolean checkForUpdateWarnings, boolean leaveSessionOpen, Boolean maintainWorkarea, boolean checkTaskModifiedObjects) {

		this.project = project;
		this.database = database;
		this.release = release;
		this.purpose = purpose;
		this.username = username;
		this.password = fixEmpty(password)!=null ? Secret.fromString(password) : null;
		this.engine = engine;
		this.oldProject = oldProject;
		this.baseline = baseline;
		this.oldBaseline = oldBaseline;
		this.ccmHome = ccmHome;
		this.remoteClient = remoteClient;
		this.detectConflict = detectConflict;
		this.replaceSubprojects = replaceSubprojects;
		this.checkForUpdateWarnings = checkForUpdateWarnings;
		this.leaveSessionOpen = leaveSessionOpen;
		this.maintainWorkarea = maintainWorkarea;
		this.checkTaskModifiedObjects = checkTaskModifiedObjects;
	}

	@Override
	public boolean checkout(AbstractBuild build, Launcher launcher, FilePath path, BuildListener listener, File changeLogFile) throws IOException, InterruptedException {	
		try {
			// Start Synergy.
			setCommands(SessionUtils.openSession(path, this, listener, launcher));

			// Compute dynamic names (replace variable by their values).
			String projectName = computeDynamicValue(build, project);
			String oldProjectName = computeDynamicValue(build, oldProject);
			String baselineName = computeDynamicValue(build, baseline);
			String oldBaselineName = computeDynamicValue(build, oldBaseline);

			// Check projet state.
			if (project != null && project.length() != 0) {
				// Work on a Synergy project.
				CheckoutResult result = checkoutProject(path, changeLogFile, projectName, oldProjectName);
				if (result != null) {
					writeChangeLog(changeLogFile, result.getLogs());
					if (result.getConflicts() != null && !result.getConflicts().isEmpty()) {
						listener.getLogger().println("Error(no project provided) : conflicts detected for project " + projectName);
						return false;
					}
				}
			} else if (baseline != null && baseline.length() != 0) {
				// Work on a Synergy baseline.
				checkoutBaseline(path, changeLogFile, baselineName, oldBaselineName);
			} else if (release!=null && release.length()!=0) {
				// Work on a Synergy project grouping.
				CheckoutResult result = checkoutProjectGrouping(path, changeLogFile, release, purpose);
				if (result != null) {
					writeChangeLog(changeLogFile, result.getLogs());
					if (result.getConflicts() != null && !result.getConflicts().isEmpty()) {
						listener.getLogger().println("Error(no release provided) : conflicts detected for project " + projectName);
						return false;
					}
				} else {
					return false;
				}
			} else {
				listener.getLogger().println("Error : neither project nor baseline nor release is specified");
				return false;
			}
		} catch (SynergyException e) {
			return false;
		} finally {
			// Stop Synergy.			
			try {
				SessionUtils.closeSession(path, this, getCommands());
				setCommands(null);
			} catch (SynergyException e) {
				setCommands(null);
				return false;
			}		
		}
		return true;
	}
	
	/**
	 * "Checkout" a project grouping
	 * 
	 * @param path				Hudson workarea path
	 * @param changeLogFile		Hudson changelog file
	 * @param release			The project grouping release
	 * @param purpose			The project grouping purpose
	 * 
	 * @throws IOException
	 * @throws InterruptedException
	 * @throws SynergyException
	 */
	private CheckoutResult checkoutProjectGrouping(FilePath path, File changeLogFile, String release, String purpose) throws IOException, InterruptedException, SynergyException {
		// Find project grouping.
		FindProjectGroupingCommand findCommand = new FindProjectGroupingCommand(release, purpose);
		getCommands().executeSynergyCommand(path, findCommand);
		List<String> projectGroupings = findCommand.getProjectGroupings();
		if (projectGroupings.size()!=1) {
			getCommands().getTaskListener().error("Error : multiple or no project grouping found");
			return null;
		}
		String projectGrouping = projectGroupings.get(0);
		
		// Update members.		
		UpdateCommand updateCommand = new UpdateCommand(UpdateCommand.PROJECT_GROUPING, projectGrouping, isReplaceSubprojects());
		getCommands().executeSynergyCommand(path, updateCommand);
		List<String> updates = updateCommand.getUpdates();
			
		// Generate changelog
		Collection<LogEntry> logs = generateChangeLog(updates, projectGrouping, changeLogFile, path);
			
		// Check update warnings.
		if(isCheckForUpdateWarnings() && updateCommand.isUpdateWarningsExists()){
			return new CheckoutResult(updateCommand.getConflicts(), logs);		
		}				
		
		// Check conflicts.
		List<Conflict> conflicts = new ArrayList<Conflict>();
		if (detectConflict) {
			// Find the project to detect conflicts into.
			FindProjectInProjectGrouping findProjectCommand = new FindProjectInProjectGrouping(projectGrouping);
			getCommands().executeSynergyCommand(path, findProjectCommand);
			List<String> projects = findProjectCommand.getProjects();
			
			for (String project : projects) {
				ProjectConflicts conflictsCommand = new ProjectConflicts(project);
				getCommands().executeSynergyCommand(path, conflictsCommand);
				List<Conflict> projectConflicts = conflictsCommand.getConflicts();
				if (projectConflicts!=null) {
					conflicts.addAll(projectConflicts);
				}
			}
		}
		
		return new CheckoutResult(conflicts, logs);
	}

	/**
	 * "Checkout" a baseline
	 * 
	 * @param path				Hudson workarea path
	 * @param changeLogFile		Hudson changelog file
	 * @param baselineName		The name of the baseline to checkout
	 * @param oldBaselineName	The name of the old baseline (for differential delivery)
	 * 
	 * @throws IOException
	 * @throws InterruptedException
	 * @throws SynergyException
	 */
	private void checkoutBaseline(FilePath path, File changeLogFile, String baselineName, String oldBaselineName) throws IOException, InterruptedException, SynergyException {
		// Get delimiter.
		GetDelimiterCommand getDelim = new GetDelimiterCommand();
		getCommands().executeSynergyCommand(path, getDelim);
		String delim = getDelim.getDelimiter();

		// Get projects.
		String baselineObjectName = baselineName;
		if (baselineObjectName.indexOf(":baseline:") == -1) {
			baselineObjectName = baselineName + delim + "1:baseline:1";
		}
		GetProjectInBaselineCommand projectsCommand = new GetProjectInBaselineCommand(baselineObjectName);
		getCommands().executeSynergyCommand(path, projectsCommand);
		List<String> projects = projectsCommand.getProjects();

		// Mapping of old/new projects.
		Map<String, String> projectsMapping = new HashMap<String, String>();

		// Extract the project names.
		for (String newProject : projects) {
			String newProjectName = newProject.substring(0, newProject.indexOf(':'));
			projectsMapping.put(newProjectName, null);
		}

		// Get old projects.
		if (oldBaselineName != null && oldBaselineName.length() != 0) {
			String oldBaselineObjectName = oldBaselineName;
			if (oldBaselineObjectName.indexOf(":baseline:") == -1) {
				oldBaselineObjectName = oldBaselineName + delim + "1:baseline:1";
			}
			projectsCommand = new GetProjectInBaselineCommand(oldBaselineObjectName);
			getCommands().executeSynergyCommand(path, projectsCommand);
			List<String> oldProjects = projectsCommand.getProjects();

			// Map each old project to a new project.
			// TODO this won't work if multiple project with the same name is authorized.
			Map<String, String> projectNames = new HashMap<String, String>();

			// Extract project information.
			for (String oldProject : oldProjects) {
				// From "project~version:project:instance" to "project"
				String projectNameWithoutVersion = oldProject.substring(0, oldProject.indexOf(delim));

				// From ""project~version:project:instance" to "project~version"
				String oldProjectNameWithVersion = oldProject.substring(0, oldProject.indexOf(':'));

				// Update project list.
				projectNames.put(projectNameWithoutVersion, oldProjectNameWithVersion);
			}

			for (String newProject : projects) {
				String newProjectNameWithoutVersion = newProject.substring(0, newProject.indexOf(delim));
				String newProjectNameWithVersion = newProject.substring(0, newProject.indexOf(':'));
				String oldProjectNameWithVersion = projectNames.get(newProjectNameWithoutVersion);
				projectsMapping.put(newProjectNameWithVersion, oldProjectNameWithVersion);
			}

		}

		// Clear workarea
		path.deleteContents();

		// Checkout projects.
		// TODO This could be done in a one big request.
		Collection<LogEntry> allEntries = new ArrayList<LogEntry>();
		for (Map.Entry<String, String> project : projectsMapping.entrySet()) {
			CheckoutResult result = checkoutStaticProject(path, changeLogFile, project.getKey(), project.getValue());
			Collection<LogEntry> entries = result.getLogs();
			if (entries!=null) {
				allEntries.addAll(entries);
			}
		}

		// Write change log.
		// TODO Task spawning on several project will be reported multiple times
		writeChangeLog(changeLogFile, allEntries);
	}

	/**
	 * Checkout a project
	 * 
	 * @param path				Hudson workarea path
	 * @param changeLogFile		Hudson changelog file
	 * @param projectName		The name of the project to checkout
	 * @param oldProjectName	The name of the old project (for differential delivery)
	 * 
	 * @throws IOException
	 * @throws InterruptedException
	 * @throws SynergyException
	 */
	private CheckoutResult checkoutStaticProject(FilePath path, File changeLogFile, String projectName, String oldProjectName) throws IOException, InterruptedException, SynergyException {
		// Compute workare path
		String desiredWorkArea = getCleanWorkareaPath(path);

		if (oldProjectName != null && oldProjectName.length() != 0) {
			// Compute difference.
			CompareProjectCommand compareCommand = new CompareProjectCommand(projectName, oldProjectName);
			getCommands().executeSynergyCommand(path, compareCommand);
			List<String> result = compareCommand.getDifferences();

			Collection<LogEntry> entries = generateChangeLog(result, projectName, changeLogFile, path);
			copyEntries(path, entries);
			return new CheckoutResult(null, entries);
		} else {
			// Create snapshot.
			WorkareaSnapshotCommand workareaSnapshotCommand = new WorkareaSnapshotCommand(projectName, desiredWorkArea);
			getCommands().executeSynergyCommand(path, workareaSnapshotCommand);

			// TODO compute and write changelog
			return new CheckoutResult(null, null);
		}
	}

	/**
	 * Checkout a project
	 * 
	 * @param path 					Hudson workarea path
	 * @param changeLogFile			Hudson changelog file
	 * @param projectName			The name of the project to checkout
	 * @param oldProjectName		The name of the old project (for differential delivery)
	 * 
	 * @throws IOException
	 * @throws InterruptedException
	 * @throws SynergyException
	 */
	private CheckoutResult checkoutProject(FilePath path, File changeLogFile, String projectName, String oldProjectName) throws IOException, InterruptedException, SynergyException {
		if (isStaticProject(projectName, path)) {
			// Clear workarea.
			path.deleteContents();

			return checkoutStaticProject(path, changeLogFile, projectName, oldProjectName);
		} else {
			return checkoutDynamicProject(path, changeLogFile, projectName);
		}
	}

	private CheckoutResult checkoutDynamicProject(FilePath path, File changeLogFile, String projectName) throws IOException, InterruptedException, SynergyException {
		// Configure workarea.
		// Assume a null value means TRUE 
		// (as it was the default behavior before the addition of this parameter) 
		if (shouldMaintainWorkarea()) {
			setAbsoluteWorkarea(path, projectName);
		}

		// Update members.
		UpdateCommand updateCommand = new UpdateCommand(UpdateCommand.PROJECT, projectName, isReplaceSubprojects());
		getCommands().executeSynergyCommand(path, updateCommand);
		List<String> updates = updateCommand.getUpdates();

		// Generate changelog
		String pgName = updateCommand.getPgName();
		Collection<LogEntry> logs = generateChangeLog(updates, projectName, changeLogFile, path, pgName);
		
		// Check update warnings.
		if(isCheckForUpdateWarnings() && updateCommand.isUpdateWarningsExists()){
			return new CheckoutResult(updateCommand.getConflicts(), logs);
		}
		
		// Check conflicts.
		List<Conflict> conflicts = null;
		if (detectConflict) {
			ProjectConflicts conflictsCommand = new ProjectConflicts(projectName);
			getCommands().executeSynergyCommand(path, conflictsCommand);
			conflicts = conflictsCommand.getConflicts();
		}
		
		return new CheckoutResult(conflicts, logs);
	}

	/**
	 * Copy the specified entries in the workarea.
	 * 
	 * @param ccmAddr
	 * @throws InterruptedException
	 * @throws IOException
	 * @throws SynergyException
	 */
	private void copyEntries(FilePath path, Collection<LogEntry> entries) throws IOException, InterruptedException, SynergyException {
		// Iterate over the tasks.
		for (LogEntry entry : entries) {
			for (Path object : entry.getPaths()) {
				String id = object.getId();
				if (id.indexOf(":dir:") == -1) {
					String pathInProject = object.getValue();
					FilePath pathInWorkarea = path.child(pathInProject);
					WriteObjectCommand command = new WriteObjectCommand(id, pathInWorkarea);
					getCommands().executeSynergyCommand(path, command);
				}
			}
		}
	}

	/**
	 * Clean the workarea path from extra dots.
	 */
	private String getCleanWorkareaPath(FilePath path) {
		String desiredWorkArea = path.getRemote();
		desiredWorkArea = desiredWorkArea.replace("\\.\\", "\\");
		return desiredWorkArea;
	}

	/**
	 * Replace an expression in the form ${name} in the given String
	 * by the value of the matching environment variable.
	 */
	private String computeDynamicValue(AbstractBuild build, String parameterizedValue) throws IllegalStateException, InterruptedException, IOException {
		if (parameterizedValue != null && parameterizedValue.indexOf("${") != -1) {
			int start = parameterizedValue.indexOf("${");
			int end = parameterizedValue.indexOf("}", start);
			String parameter = parameterizedValue.substring(start + 2, end);
			String value = (String) build.getEnvironment(TaskListener.NULL).get(parameter);
			if (value == null) {
				throw new IllegalStateException(parameter);
			}
			return parameterizedValue.substring(0, start) + value + (parameterizedValue.length() > end + 1 ? parameterizedValue.substring(end + 1) : "");
		} else {
			return parameterizedValue;
		}
	}

	/**
	 * Check the project state.
	 * 
	 * @return true if the project is a static project
	 */
	private boolean isStaticProject(String project, FilePath workspace) throws IOException, InterruptedException, SynergyException {
		// Get project state.
		GetProjectStateCommand command = new GetProjectStateCommand(project);
		getCommands().executeSynergyCommand(workspace, command);
		String state = command.getState();

		// Compute result.
		if ("prep".equals(state)) {
			// Integration testing, become build manager.
			SetRoleCommand setRoleCommand = new SetRoleCommand(SetRoleCommand.BUILD_MANAGER);
			getCommands().executeSynergyCommand(workspace, setRoleCommand);
			return false;
		} else if ("working".equals(state)) {
			 // Development project.
			return false;
		} else {
			// Released project part of a baseline.
			return true;
		}
	}

	/**
	 * Configure the Synergy workarea for a top or subproject.
	 * 
	 * @param project		The Synergy project name
	 * @param relative		Should the workarea be relative or asbolute
	 * @param launcher		The Hudson launcher to use to launch commands
	 * @param workspace		The Hudon project workspace path
	 * @param ccmAddr		The current Synergy sesison address
	 * @param listener		The Hudson build listener
	 * @return True if the workarea was configured sucessfully
	 * @throws IOException
	 * @throws InterruptedException
	 * @throws SynergyException
	 */
	private void configureWorkarea(String project, boolean relative, FilePath workspace) throws IOException, InterruptedException, SynergyException {
		// Check maintain workarea.
		GetProjectAttributeCommand getProjectAttributeCommand = new GetProjectAttributeCommand(project, GetProjectAttributeCommand.MAINTAIN_WORKAREA);
		getCommands().executeSynergyCommand(workspace, getProjectAttributeCommand);
		String maintainWorkArea = getProjectAttributeCommand.getValue();
		boolean changeMaintain = false;


		// Check relative/absolute wo.
		if (relative) {
			getProjectAttributeCommand = new GetProjectAttributeCommand(project, GetProjectAttributeCommand.RELATIVE);
			getCommands().executeSynergyCommand(workspace, getProjectAttributeCommand);
			String relativeWorkArea = getProjectAttributeCommand.getValue();

			if (relative && !"TRUE".equals(relativeWorkArea)) {
				// If asked for relative workarea, and workarea is not relative, set relative workarea.
				SetProjectAttributeCommand setProjectAttributeCommand = new SetProjectAttributeCommand(project, GetProjectAttributeCommand.RELATIVE, relative ? "TRUE" : "FALSE");
				getCommands().executeSynergyCommand(workspace, setProjectAttributeCommand);
			}
		}

		// Check workarea path.
		getProjectAttributeCommand = new GetProjectAttributeCommand(project, GetProjectAttributeCommand.WORKAREA_PATH);
		getCommands().executeSynergyCommand(workspace, getProjectAttributeCommand);
		String currentWorkArea = getProjectAttributeCommand.getValue();

		String desiredWorkArea = relative ? currentWorkArea : getCleanWorkareaPath(workspace);
		if (!currentWorkArea.equals(desiredWorkArea) || changeMaintain) {
			// If current workarea location is not the desired one, change it.
			SetProjectAttributeCommand setProjectAttributeCommand = new SetProjectAttributeCommand(project, GetProjectAttributeCommand.WORKAREA_PATH, desiredWorkArea);
			getCommands().executeSynergyCommand(workspace, setProjectAttributeCommand);
		}

		if (!"TRUE".equals(maintainWorkArea)) {
			// If workarea is not maintain, maintain it.
			SetProjectAttributeCommand setProjectAttributeCommand = new SetProjectAttributeCommand(project, GetProjectAttributeCommand.MAINTAIN_WORKAREA, "TRUE");
			getCommands().executeSynergyCommand(workspace, setProjectAttributeCommand);
			changeMaintain = true;
		}

	}

	/**
	 * Configure the Synergy workarea for the main project and the subprojects.
	 * 
	 * @param path			The Hudson projet workspace path
	 * @param projectName	The Hudson project name
	 * @throws IOException
	 * @throws InterruptedException
	 * @throws SynergyException
	 */
	private void setAbsoluteWorkarea(FilePath path, String projectName) throws IOException, InterruptedException, SynergyException {
		// Check main project.
		configureWorkarea(projectName, false, path);

		// Get subproject.
		SubProjectQueryCommand command = new SubProjectQueryCommand(projectName);
		try {
			getCommands().executeSynergyCommand(path, command);
		} catch (SynergyException e) {
			// 1 and 6 is ok (means the query returns nothing).
			// (For Synergy 7.1 and above exitcode 6 provides the information that the result of the command was empty)
			if ((e.getStatus() != 1) && (e.getStatus() != 6)){
			   System.out.println("ERROR: "+command+" EXITCODE :"+e.getStatus());
				throw e;
			}
		}
		List<String> subProjects = command.getSubProjects();

		if (subProjects != null && subProjects.size() > 0) {
			for (String subProject : subProjects) {
				configureWorkarea(subProject, true, path);
			}
		}
	}

	/**
	 * Gets the file that stores the revision.
	 */
	public static File getRevisionFile(AbstractBuild build) {
		return new File(build.getRootDir(), "revision.txt");
	}

	/**
	 * Reads the revision file of the specified build.
	 * 
	 * @return map from {@link SvnInfo#url Subversion URL} to its revision.
	 */
	/* package */static Map<String, Long> parseRevisionFile(AbstractBuild build) throws IOException {
		Map<String, Long> revisions = new HashMap<String, Long>(); // module ->
		// revision
		{// read the revision file of the last build
			File file = getRevisionFile(build);
			if (!file.exists())
				// nothing to compare against
				return revisions;

			BufferedReader br = new BufferedReader(new FileReader(file));
			try {
				String line;
				while ((line = br.readLine()) != null) {
					int index = line.lastIndexOf('/');
					if (index < 0) {
						continue; // invalid line?
					}
					try {
						revisions.put(line.substring(0, index), Long.parseLong(line.substring(index + 1)));
					} catch (NumberFormatException e) {
						// perhaps a corrupted line. ignore
					}
				}
			} finally {
				br.close();
			}
		}

		return revisions;
	}

	/**
	 * Generate the changelog.
	 * 
	 * @param names			Names of the elements that have changed
	 * @param projects		Name of the Synergy project being build and that may contain changes
	 * @param changeLogFile	File to write the changelog into
	 * @param workarea		The Workarea path
	 * @return
	 * @throws IOException
	 * @throws InterruptedException
	 * @throws SynergyException
	 */
	private Collection<LogEntry> generateChangeLog(List<String> names, String projectName, File changeLogFile, FilePath workarea) throws IOException, InterruptedException, SynergyException {
		return generateChangeLog(names, projectName, changeLogFile, workarea, null);
	}
	
	/**
	 * Generate the changelog.
	 * 
	 * @param names			Names of the elements that have changed
	 * @param projects		Name of the Synergy project being build and that may contain changes
	 * @param changeLogFile	File to write the changelog into
	 * @param workarea		The Workarea path
	 * @param pgName		Optional name of the project grouping to which the project belongs
	 * @return
	 * @throws IOException
	 * @throws InterruptedException
	 * @throws SynergyException
	 */
	private Collection<LogEntry> generateChangeLog(List<String> names, String projectName, File changeLogFile, FilePath workarea, String pgName) throws IOException, InterruptedException, SynergyException {
		// Find information about the element.
		Map<String, LogEntry> logs = new HashMap<String, LogEntry>();
		DateFormat dateFormat = DateFormat.getDateTimeInstance();

		if (names != null) {
			// Compute the subproject list for the finduse project
			// in case the given project is a top level project.
			RecursiveProjectQueryCommand subProjectQuery = new RecursiveProjectQueryCommand(projectName);
//			SubProjectQueryCommand subProjectQuery = new SubProjectQueryCommand(projectName);
			getCommands().executeSynergyCommand(workarea, subProjectQuery);
			Set<String> projects = new HashSet<String>(subProjectQuery.getSubProjects());
			projects.add(projectName);

			// Find the delimiter.
			GetDelimiterCommand getDelim = new GetDelimiterCommand();
			getCommands().executeSynergyCommand(workarea, getDelim);
			String delimiter = getDelim.getDelimiter();

			// Get project state.
			GetProjectStateCommand stateCommand = new GetProjectStateCommand(projectName);
			getCommands().executeSynergyCommand(workarea, stateCommand);
			String state = stateCommand.getState();
			
			// Determine instance for project grouping
			String subsystem;
			if ("working".equals(state)) {
				GetProjectOwnerCommand ownerCommand = new GetProjectOwnerCommand(projectName);
				getCommands().executeSynergyCommand(workarea, ownerCommand);
				subsystem = ownerCommand.getOwner();
			} else {
				subsystem = "1";				
			}

			// Get project grouping release and purpose
			GetProjectGroupingInfoCommand projectGroupingInfo = new GetProjectGroupingInfoCommand(pgName);
			getCommands().executeSynergyCommand(workarea, projectGroupingInfo);
			String pgProjectPurpose = projectGroupingInfo.getProjectPurpose();
			String pgRelease = projectGroupingInfo.getRelease();
			
			// Determine member status from project purpose
			GetMemberStatusCommand statusCommand = new GetMemberStatusCommand(pgProjectPurpose);
			getCommands().executeSynergyCommand(workarea, statusCommand);
			String memberStatus = statusCommand.getMemberStatus();
			
			// Get project grouping object
			GetProjectGroupingCommand findCommand = new GetProjectGroupingCommand(pgRelease, memberStatus, subsystem);
			getCommands().executeSynergyCommand(workarea, findCommand);
			String projectGrouping = findCommand.getProjectGrouping();
			
			// Find the task associated to each change and the path of each changed object.
			for (String name : names) {
				// Entry to use.
				SynergyChangeLogSet.LogEntry entry = null;

				// Find associate task.
				FindAssociatedTaskCommand taskCommand = new FindAssociatedTaskCommand(name, projectGrouping);
				getCommands().executeSynergyCommand(workarea, taskCommand);
				List<String> taskIds = taskCommand.getTasks();
				if (taskIds != null && !taskIds.isEmpty()) {
					String taskId = taskIds.get(0);
					entry = logs.get(taskId);
					if (entry == null) {
						entry = new LogEntry();
						logs.put(taskId, entry);
					}
				}				

				// Deal with no task case (should not happen)
				if (entry == null) {
					entry = logs.get(null);
					if (entry == null) {
						entry = new LogEntry();
						entry.setMsg("Unknown task");
						logs.put(null, entry);
					}
				}

				// Find use of the element in the project.
				FindUseCommand command = new FindUseCommand(name, projects, delimiter, false);
				getCommands().executeSynergyCommand(workarea, command);
				String pathInProject = command.getPath();
				if (pathInProject != null) {
					if (!pathInProject.startsWith(projectName)) {
						// The element is in a subproject.
						// TODO add the subprojet path
					}
					Path path = new Path();
					path.setId(name);
					path.setValue(pathInProject);
					entry.addPath(path);
				}
			}
			
			// Find the info for each task.
			for (Map.Entry<String,LogEntry> entry : logs.entrySet()) {
				String taskId = entry.getKey();
				LogEntry log = entry.getValue();
				
				if (taskId!=null) {
					List<String> t = new ArrayList<String>(1);
					t.add(taskId);
					TaskInfoCommand taskInfoCommand = new TaskInfoCommand(t);
					getCommands().executeSynergyCommand(workarea, taskInfoCommand);
					List<TaskCompleted> infos = taskInfoCommand.getInformations();
					if (!infos.isEmpty()) {
						log.setMsg(infos.get(0).getSynopsis());
						log.setUser(infos.get(0).getResolver());
						log.setTaskId(infos.get(0).getId());
						log.setDate(infos.get(0).getDateCompleted() == null ? null : dateFormat.format(infos.get(0).getDateCompleted()));
					}						
				}
			}		
		}

		return logs.values();
	}

	/**
	 * Write the changelog file.
	 * 
	 * @param changeLogFile	The changelog file
	 * @param logs			The logs
	 */
	private void writeChangeLog(File changeLogFile, Collection<LogEntry> logs) {
		if (logs != null) {
			PrintWriter writer = null;
			try {
				writer = new PrintWriter(changeLogFile, "UTF-8");
				writer.println("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
				writer.println("<log>");

				for (LogEntry log : logs) {
					writer.println(String.format("\t<logentry revision=\"%s\">", log.getTaskId()));
					writer.println(String.format("\t\t<task>%s</task>", log.getTaskId()));
					writer.println(String.format("\t\t<author>%s</author>", log.getUser()));
					writer.println(String.format("\t\t<date>%s</date>", log.getDate()));
					writer.println(String.format("\t\t<msg><![CDATA[%s]]></msg>", log.getMsg()));

					writer.println("\t\t<paths>");
					for (SynergyChangeLogSet.Path path : log.getPaths()) {
						writer.println(String.format("\t\t\t<path action=\"%s\">%s</path>", path.getEditType(), path.getValue()));
					}
					writer.println("\t\t</paths>");

					writer.println("\t</logentry>");
				}

				writer.println("</log>");
			} catch (IOException e) {
				// TODO
			} finally {
				if (writer != null) {
					writer.close();
				}
			}
		}
	}

	@Override
	public ChangeLogParser createChangeLogParser() {
		return new SynergyChangeLogParser();
	}

	@Override
	public DescriptorImpl getDescriptor() {
		return DescriptorImpl.DESCRIPTOR;
	}

    @Override public boolean requiresWorkspaceForPolling() {
        return true;
    }

    private static final class SynergyRevisionState extends SCMRevisionState {
        final Calendar date;
        SynergyRevisionState(Calendar date) {
            this.date = date;
        }
    }

    @Override public SCMRevisionState calcRevisionsFromBuild(AbstractBuild<?, ?> build, Launcher launcher, TaskListener listener) throws IOException, InterruptedException {
        return new SynergyRevisionState(build.getTimestamp());
    }

    @Override protected PollingResult compareRemoteRevisionWith(AbstractProject<?, ?> project, Launcher launcher, FilePath path, TaskListener listener, SCMRevisionState baseline) throws IOException, InterruptedException {
		// Check release.
		if (release == null) {
			listener.getLogger().println("The release attribute is not set. It is required for change pooling.");
			return PollingResult.NO_CHANGES;
		}

		// Configure commands.
		setCommands(null);
		try {
			// Start Synergy.
			setCommands(SessionUtils.openSession(path, this, listener, launcher));

			// Find completed tasks.
			FindCompletedSinceDateCommand findCommand = new FindCompletedSinceDateCommand(((SynergyRevisionState) baseline).date, release);
			getCommands().executeSynergyCommand(path, findCommand);
			List<String> result = findCommand.getTasks();

			if (checkTaskModifiedObjects) {
			    return checkTaskModifiedObjects(result, path) ? PollingResult.SIGNIFICANT : PollingResult.NO_CHANGES;
			} else {
			    return result != null && !result.isEmpty() ? PollingResult.SIGNIFICANT : PollingResult.NO_CHANGES;
			}
		} catch (SynergyException e) {
			return PollingResult.NO_CHANGES;
		} finally {
			// Stop Synergy
			try {
				SessionUtils.closeSession(path, this, getCommands());
				setCommands(null);
			} catch (SynergyException e) {
				setCommands(null);
				return PollingResult.NO_CHANGES;
			}
		}
    }
	
	/**
	 * Check if the specified taks modify the current project.
	 * @param tasks		A list of task number
	 * @param path		The current project path.
	 * @return			If a task in the list modify the current project
	 * @throws SynergyException
	 * @throws InterruptedException
	 * @throws IOException
	 */
	private boolean checkTaskModifiedObjects(List<String> tasks, FilePath path) throws SynergyException, InterruptedException, IOException {
		if (tasks == null || tasks.isEmpty()) {
			return false;
		}

		// Find if the task updated some files
		TaskShowObjectsCommand showObjectsCommand = new TaskShowObjectsCommand(tasks);
		getCommands().executeSynergyCommand(path, showObjectsCommand);
		List<String> modifiedObjects = showObjectsCommand.getObjects();

		// Get project use of those files
		GetDelimiterCommand getDelim = new GetDelimiterCommand();
		getCommands().executeSynergyCommand(path, getDelim);
		String delimiter = getDelim.getDelimiter();
		Set<String> projects = Collections.singleton(this.project);
		for (String modifiedObject : modifiedObjects) {
			FindUseWithoutVersionCommand findUseCommand = new FindUseWithoutVersionCommand(modifiedObject, projects, delimiter);
			getCommands().executeSynergyCommand(path, findUseCommand);
			if (findUseCommand.getPath() != null) {
				return true;
			}
		}
		return false;
	}


	/**
	 * Returns the raw project name.
	 * If the project name contains variable, they are not substituted here.
	 * @return	The raw project name
	 */
	public String getProject() {
		return project;
	}

	public String getCcmHome() {
		return ccmHome;
	}

	public String getDatabase() {
		return database;
	}

	public String getUsername() {
		return username;
	}

	public Secret getPassword() {
        return password;
	}

	public String getRelease() {
		return release;
	}

	public String getPurpose() {
		return purpose;
	}

	public String getEngine() {
		return engine;
	}

	public void setEngine(String engine) {
		this.engine = engine;
	}

	public String getOldProject() {
		return oldProject;
	}

	public String getBaseline() {
		return baseline;
	}

	public String getOldBaseline() {
		return oldBaseline;
	}

	public boolean isRemoteClient() {
		return remoteClient;
	}

	public void setRemoteClient(boolean remoteClient) {
		this.remoteClient = remoteClient;
	}

	public boolean isDetectConflict() {
		return detectConflict;
	}

	public void setDetectConflict(boolean detectConflict) {
		this.detectConflict = detectConflict;
	}

	public boolean isReplaceSubprojects() {
		return replaceSubprojects;
	}

	public void setReplaceSubprojects(boolean replaceSubprojects) {
		this.replaceSubprojects = replaceSubprojects;
	}
	
	public boolean isCheckForUpdateWarnings(){
		return checkForUpdateWarnings;
	}
	
	public void setCheckForUpdateWarnings(boolean checkForUpdateWarnings){
		this.checkForUpdateWarnings = checkForUpdateWarnings;
	}
	
	public void setLeaveSessionOpen(boolean leaveSessionOpen) {
		this.leaveSessionOpen = leaveSessionOpen;
	}
	public boolean isLeaveSessionOpen() {
		return leaveSessionOpen;
	}
	public void setMaintainWorkarea(Boolean maintainWorkarea) {
		this.maintainWorkarea = maintainWorkarea;
	}
	public Boolean getMaintainWorkarea() {
		return maintainWorkarea;
	}
	public boolean shouldMaintainWorkarea() {
		return maintainWorkarea==null || maintainWorkarea.booleanValue();
	}
	public boolean isCheckTaskModifiedObjects() {
		return checkTaskModifiedObjects;
	}
	public void setCheckTaskModifiedObjects(boolean checkTaskModifiedObjects) {
		this.checkTaskModifiedObjects = checkTaskModifiedObjects;
	}

	private void setCommands(Commands commands) {
		if (this.commands==null)
			this.commands=new ThreadLocal<Commands>();
		this.commands.set(commands);
		if (commands==null)
			this.commands.remove();
	}

	private Commands getCommands() {
		if (this.commands==null)
			this.commands=new ThreadLocal<Commands>();
		return commands.get();
	}
}
