package hudson.plugins.synergy;

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
import hudson.plugins.synergy.impl.FindUseCommand;
import hudson.plugins.synergy.impl.GetDelimiterCommand;
import hudson.plugins.synergy.impl.GetProjectAttributeCommand;
import hudson.plugins.synergy.impl.GetProjectInBaselineCommand;
import hudson.plugins.synergy.impl.GetProjectStateCommand;
import hudson.plugins.synergy.impl.ProjectConflicts;
import hudson.plugins.synergy.impl.SetProjectAttributeCommand;
import hudson.plugins.synergy.impl.SetRoleCommand;
import hudson.plugins.synergy.impl.StartCommand;
import hudson.plugins.synergy.impl.StopCommand;
import hudson.plugins.synergy.impl.SubProjectQueryCommand;
import hudson.plugins.synergy.impl.SynergyException;
import hudson.plugins.synergy.impl.TaskCompleted;
import hudson.plugins.synergy.impl.TaskInfoCommand;
import hudson.plugins.synergy.impl.UpdateCommand;
import hudson.plugins.synergy.impl.WorkareaSnapshotCommand;
import hudson.plugins.synergy.impl.WriteObjectCommand;
import hudson.scm.ChangeLogParser;
import hudson.scm.SCM;
import hudson.scm.SCMDescriptor;
import hudson.scm.SubversionSCM.SvnInfo;
import hudson.util.FormFieldValidator;

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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.ServletException;

import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;

/**
 * Synergy SCM
 * 
 * @author Jean-Noel RIBETTE
 */
public class SynergySCM extends SCM implements Serializable {
	public static final class DescriptorImpl extends SCMDescriptor<SynergySCM> {
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

		public String getCcmEngLog() {
			return ccmEngLog;
		}

		public String getCcmUiLog() {
			return ccmUiLog;
		}

		private DescriptorImpl() {
			super(SynergySCM.class, null);	
			load();
		}				
		
		@Override
		public boolean configure(StaplerRequest request) throws FormException {
			ccmExe = request.getParameter("synergy.ccmExe");
			ccmUiLog = request.getParameter("synergy.ccmUiLog");
			ccmEngLog = request.getParameter("synergy.ccmEngLog");
			save();
			return true;
		}
		
		@Override
        public SCM newInstance(StaplerRequest req) throws FormException {
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
                    "true".equals(req.getParameter("synergy.remoteClient")),
                    "true".equals(req.getParameter("synergy.detectConflict"))
                    );
        }
		
		/**
         * Checks if ccm executable exists.
         */
        public void doCcmExeCheck(StaplerRequest req, StaplerResponse rsp) throws IOException, ServletException {
            new FormFieldValidator.Executable(req,rsp).process();
        }
		
		
		@Override
		public String getDisplayName() {			
			return "Synergy";
		}
		
		public String getCcmExe() {
			if (ccmExe==null) {
				return "ccm";
			} else {
				return ccmExe;
			}
		}
	}
	
	/**
	 * The Synergy project name.
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
	private String password;
	
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
		
	private transient Commands commands;
	
	@DataBoundConstructor
    public SynergySCM(String project, String database, String release, String purpose, String username, String password, String engine, String oldProject, String baseline, String oldBaseline, boolean remoteClient, boolean detectConflict) {				

		this.project = project;
		this.database = database;		
		this.release = release;
		this.purpose = purpose;
		this.username = username;
		this.password = password;
		this.engine = engine;
		this.oldProject = oldProject;
		this.baseline  = baseline;
		this.oldBaseline = oldBaseline;
		this.remoteClient = remoteClient;
		this.detectConflict = detectConflict;
	}
	
	
	
	@Override
	public boolean checkout(AbstractBuild build, Launcher launcher, FilePath path, BuildListener listener, File changeLogFile) throws IOException, InterruptedException {
		// Configure commands.
		commands = new Commands();
		commands.setCcmExe(getDescriptor().getCcmExe());
		commands.setCcmUiLog(getDescriptor().getCcmUiLog());
		commands.setCcmEngLog(getDescriptor().getCcmEngLog());
		
		commands.setTaskListener(listener);
		commands.setLauncher(launcher);
		
		try {
			// Start Synergy.
			StartCommand command = new StartCommand(database, engine, username, password, remoteClient);
			commands.executeSynergyCommand(path, command);
			String ccmAddr = command.getCcmAddr();
			commands.setCcmAddr(ccmAddr);
			
			// Compute dynamic name.
			String projectName = computeDynamicValue(build, project);	
			String oldProjectName = computeDynamicValue(build, oldProject);
			String baselineName = computeDynamicValue(build, baseline);
			String oldBaselineName = computeDynamicValue(build, oldBaseline);
				
			// Set role to build manager.
			SetRoleCommand setRoleCommand = new SetRoleCommand(SetRoleCommand.BUILD_MANAGER);
			commands.executeSynergyCommand(path, setRoleCommand);
			
			// Check projet state.
			if (project!=null && project.length()!=0) {
				// Work on a Synergy project.
				CheckoutResult result = checkoutProject(path, changeLogFile, projectName, oldProjectName);
				if (result!=null) {
					writeChangeLog(changeLogFile, result.getLogs());
					if (result.getConflicts()!=null && !result.getConflicts().isEmpty()) {
						listener.getLogger().print("Error : conflicts detected for project " + projectName);
						return false;
					}
				}
			} else if (baseline!=null && baseline.length()!=0) {
				// Work on a Synergy baseline.
				checkoutBaseline(path, changeLogFile, baselineName, oldBaselineName);
			} else {
				listener.getLogger().print("Error : neither project nor baseline is specified");
				return false;
			}
		} catch (SynergyException e) {
			return false;		
		} finally {
			// Stop Synergy.
			StopCommand stopCommand = new StopCommand();
			try {
				commands.executeSynergyCommand(path, stopCommand);
			} catch (SynergyException e) {
				return false;
			}
		}
		return true;
	}
	
	/**
	 * "Checkout" a baseline
	 * 
	 * @param path					Hudson workarea path
	 * @param changeLogFile			Hudson changelog file
	 * @param baselineName			The name of the baseline to checkout
	 * @param oldBaselineName		The name of the old baseline (for differential delivery)
	 * 
	 * @throws IOException
	 * @throws InterruptedException
	 * @throws SynergyException
	 */
	private void checkoutBaseline(FilePath path, File changeLogFile, String baselineName, String oldBaselineName) throws IOException, InterruptedException, SynergyException {
		// Get delimiter.
		GetDelimiterCommand getDelim = new GetDelimiterCommand();
		commands.executeSynergyCommand(path, getDelim);
		String delim = getDelim.getDelimiter();
		
		// Get projects.
		String baselineObjectName = baselineName;
		if (baselineObjectName.indexOf(":baseline:")==-1) {
			baselineObjectName = baselineName + delim + "1:baseline:1";
		}
		GetProjectInBaselineCommand projectsCommand = new GetProjectInBaselineCommand(baselineObjectName);
		commands.executeSynergyCommand(path, projectsCommand);
		List<String> projects = projectsCommand.getProjects();
		
		// Mapping of old/new projects.
		Map<String, String> projectsMapping = new HashMap<String, String>();
		
		// Extract the project names.
		for (String newProject : projects) {
			String newProjectName = newProject.substring(0, newProject.indexOf(':'));
			projectsMapping.put(newProjectName, null);
		}
		
		// Get old projects.				
		if (oldBaselineName!=null && oldBaselineName.length()!=0) {
			String oldBaselineObjectName = oldBaselineName;
			if (oldBaselineObjectName.indexOf(":baseline:")==-1) {
				oldBaselineObjectName = oldBaselineName + delim + "1:baseline:1";
			}
			projectsCommand = new GetProjectInBaselineCommand(oldBaselineObjectName);
			commands.executeSynergyCommand(path, projectsCommand);
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
			allEntries.addAll(entries);
		}
		
		// Write change log.
		// TODO Task spawning on several project will be reported multiple times
		writeChangeLog(changeLogFile, allEntries);
	}
	
	/**
	 * Checkout a project
	 * 
	 * @param path					Hudson workarea path
	 * @param changeLogFile			Hudson changelog file
	 * @param projectName			The name of the project to checkout
	 * @param oldProjectName		The name of the old project (for differential delivery)
	 * 
	 * @throws IOException
	 * @throws InterruptedException
	 * @throws SynergyException
	 */
	private CheckoutResult checkoutStaticProject(FilePath path, File changeLogFile, String projectName, String oldProjectName) throws IOException, InterruptedException, SynergyException {
		// Compute workare path
		String desiredWorkArea = getCleanWorkareaPath(path);
								
		if (oldProjectName!=null && oldProjectName.length()!=0) {	
			// Compute difference.
			CompareProjectCommand compareCommand = new CompareProjectCommand(projectName, oldProjectName);
			commands.executeSynergyCommand(path, compareCommand);
			List<String> result = compareCommand.getDifferences();
			
			Collection<LogEntry> entries = generateChangeLog(result, projectName, changeLogFile, path);
			copyEntries(path, entries);
			return new CheckoutResult(null, entries);
		} else {									
			// Create snapshot.
			WorkareaSnapshotCommand workareaSnapshotCommand = new WorkareaSnapshotCommand(projectName, desiredWorkArea);
			commands.executeSynergyCommand(path, workareaSnapshotCommand);
			
			// TODO compute and write changelog
			return null;
		}										
	}

	/**
	 * Checkout a project
	 * 
	 * @param path					Hudson workarea path
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
		setAbsoluteWorkarea(path);
		
		// Update members.
		UpdateCommand updateCommand = new UpdateCommand(projectName);
		commands.executeSynergyCommand(path, updateCommand);
		List<String> updates = updateCommand.getUpdates();		
		
		// Generate changelog
		Collection<LogEntry> logs = generateChangeLog(updates, projectName, changeLogFile, path);
		
		// Check conflicts.
		List<Conflict> conflicts = null;
		if (detectConflict) {
			ProjectConflicts conflictsCommand = new ProjectConflicts(projectName);
			commands.executeSynergyCommand(path, conflictsCommand);
			conflicts = conflictsCommand.getConflicts();
		}
		
		return new CheckoutResult(conflicts, logs);		
	}
	
	/**
	 * Copy the specified entries in the workarea.
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
				if (id.indexOf(":dir:")==-1) {
					String pathInProject = object.getValue();
					FilePath pathInWorkarea = path.child(pathInProject);
					WriteObjectCommand command = new WriteObjectCommand(id, pathInWorkarea);
					commands.executeSynergyCommand(path, command);
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
	private String computeDynamicValue(AbstractBuild build, String parameterizedValue) throws IllegalStateException {
		if (parameterizedValue!=null && parameterizedValue.indexOf("${")!=-1) {
			int start = parameterizedValue.indexOf("${");
			int end = parameterizedValue.indexOf("}", start);
			String parameter = parameterizedValue.substring(start+2, end);
			String value = (String) build.getEnvVars().get(parameter);
			if (value==null) {
				throw new IllegalStateException(parameter);
			}
			return parameterizedValue.substring(0, start) + value + (parameterizedValue.length()>end+1 ? parameterizedValue.substring(end+1) : "");
		} else {
			return parameterizedValue;
		}
	}
	
	/**
	 * Check the project state.
	 * @return	true if the project is a static project
	 */
	private boolean isStaticProject(String project, FilePath workspace) throws IOException, InterruptedException, SynergyException {
		// Get project state.
		GetProjectStateCommand command = new GetProjectStateCommand(project);
		commands.executeSynergyCommand(workspace, command);
		String state = command.getState();
		
		// Compute result.
		if ("prep".equals(state) || "working".equals(state)) {
			// Integration testing or Development project.
			return false;
		} else {
			// Released project part of a baseline.
			return true;
		}
	}
	
	/**
	 * Configure the Synergy workarea for a top or subproject.
	 * @param project			The Synergy project name
	 * @param relative			Should the workarea be relative or asbolute
	 * @param launcher			The Hudson launcher to use to launch commands
	 * @param workspace			The Hudon project workspace path
	 * @param workarea			The desired Synergy project workarea path
	 * @param ccmAddr			The current Synergy sesison address
	 * @param listener			The Hudson build listener
	 * @return					True if the workarea was configured sucessfully
	 * @throws IOException
	 * @throws InterruptedException
	 * @throws SynergyException 
	 */
	private void configureWorkarea(String project, boolean relative, FilePath workspace, FilePath workarea) throws IOException, InterruptedException, SynergyException {
		// Check maintain workarea.
		GetProjectAttributeCommand getProjectAttributeCommand = new GetProjectAttributeCommand(project, GetProjectAttributeCommand.MAINTAIN_WORKAREA);
		commands.executeSynergyCommand(workspace, getProjectAttributeCommand);
		String maintainWorkArea = getProjectAttributeCommand.getValue();
		boolean changeMaintain = false;
		
		if (!"TRUE".equals(maintainWorkArea)) {
			// If workarea is not maintain, maintain it.
			SetProjectAttributeCommand setProjectAttributeCommand = new SetProjectAttributeCommand(project, GetProjectAttributeCommand.MAINTAIN_WORKAREA, "TRUE");
			commands.executeSynergyCommand(workspace, setProjectAttributeCommand);
			changeMaintain = true;
		}
		
		// Check relative/absolute wo.
		if (relative) {
			getProjectAttributeCommand = new GetProjectAttributeCommand(project, GetProjectAttributeCommand.RELATIVE);
			commands.executeSynergyCommand(workspace, getProjectAttributeCommand);
			String relativeWorkArea = getProjectAttributeCommand.getValue();
			
			if (relative && !"TRUE".equals(relativeWorkArea)) {
				// If asked for relative workarea, and workarea is not relative, set relative workarea.
				SetProjectAttributeCommand setProjectAttributeCommand = new SetProjectAttributeCommand(project, GetProjectAttributeCommand.RELATIVE, relative ? "TRUE" : "FALSE");
				commands.executeSynergyCommand(workspace, setProjectAttributeCommand);				
			}
		}
		
		// Check workarea path.
		getProjectAttributeCommand = new GetProjectAttributeCommand(project, GetProjectAttributeCommand.WORKAREA_PATH);
		commands.executeSynergyCommand(workspace, getProjectAttributeCommand);
		String currentWorkArea = getProjectAttributeCommand.getValue();

		String desiredWorkArea = getCleanWorkareaPath(workarea);
		if (!currentWorkArea.equals(desiredWorkArea) || changeMaintain) {
			// If current workarea location is not the desired one, change it.
			SetProjectAttributeCommand setProjectAttributeCommand = new SetProjectAttributeCommand(project, GetProjectAttributeCommand.WORKAREA_PATH, desiredWorkArea);
			commands.executeSynergyCommand(workspace, setProjectAttributeCommand);			
		}
	}
	
	/**
	 * Configure the Synergy workarea for the main project and the subprojects.
	 * 
	 * @param launcher					Launcher to use to launch command
	 * @param path						The Hudson projet workspace path
	 * @param ccmAddr					The current Synergy session address
	 * @param listener					The Hudson build listener
	 * @return							true if the Synergy workarea was configured propertly
	 * @throws IOException
	 * @throws InterruptedException
	 * @throws SynergyException 
	 */
	private void setAbsoluteWorkarea(FilePath path) throws IOException, InterruptedException, SynergyException {
		// Check main project.
		configureWorkarea(project, false, path, path);
		
		// Get subproject.
		SubProjectQueryCommand command = new SubProjectQueryCommand(project);
		try {
			commands.executeSynergyCommand(path, command);
		} catch (SynergyException e) {
			// 1 is ok (means the query returns nothing).
			if (e.getStatus()!=1) {				
				throw e;
			}
		}
		List<String> subProjects = command.getSubProjects();
		
		if (subProjects!=null && subProjects.size()>0) {
			GetDelimiterCommand getDelim = new GetDelimiterCommand();
			commands.executeSynergyCommand(path, getDelim);
			String delimiter = getDelim.getDelimiter();
			String projetPrincipal = project.split(delimiter)[0];
			for (String subProject : subProjects) {
				configureWorkarea(subProject, true, path, new FilePath(path, projetPrincipal));
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
	/* package */static Map<String, Long> parseRevisionFile(AbstractBuild build)
			throws IOException {
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
						revisions.put(line.substring(0, index), Long
								.parseLong(line.substring(index + 1)));
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
	 * @param names				Names of the elements that have changed
	 * @param projects			Name of the Synergy project being build and that may contain changes 
	 * @param changeLogFile		File to write the changelog into
	 * @param workarea			The Workarea path
	 * @return
	 * @throws IOException
	 * @throws InterruptedException
	 * @throws SynergyException
	 */
	private Collection<LogEntry> generateChangeLog(List<String> names, String projectName, File changeLogFile, FilePath workarea) throws IOException, InterruptedException, SynergyException {
		// Find information about the element.
		Map<String, LogEntry> logs = new HashMap<String, LogEntry>();
		DateFormat dateFormat = DateFormat.getDateTimeInstance();
		
		if (names!=null) {
			// Compute the subproject list for the finduse project
			// in case the given project is a top level project.
			SubProjectQueryCommand subProjectQuery = new SubProjectQueryCommand(projectName);
			commands.executeSynergyCommand(workarea, subProjectQuery);
			Set<String> projects = new HashSet<String>(subProjectQuery.getSubProjects());
			projects.add(projectName);
			
			// Find the delimiter.
			GetDelimiterCommand getDelim = new GetDelimiterCommand();
			commands.executeSynergyCommand(workarea, getDelim);
			String delimiter = getDelim.getDelimiter();

			
			// Compute the use of the subprojects in the project.
			Map<String, String> subProjectsUse = new HashMap<String, String>();
			if (projects.size()>1) {
				Set<String> set = new HashSet<String>();
				set.add(projectName);
				for (String project : projects) {
					if (!project.equals(projectName)) {
						FindUseCommand findUse = new FindUseCommand(project, set, delimiter);
						commands.executeSynergyCommand(workarea, findUse);
						String use = findUse.getPath();
						if (use!=null) {
							subProjectsUse.put(project, use);
						}
					}
				}
			}
			
			for (String name : names) {				
				// Entry to use.
				SynergyChangeLogSet.LogEntry entry = null;
				
				// Find associate task.
				FindAssociatedTaskCommand taskCommand = new FindAssociatedTaskCommand(name);
				commands.executeSynergyCommand(workarea, taskCommand);
				List<String> taskIds = taskCommand.getTasks();
				if (taskIds!=null && !taskIds.isEmpty()) {
					String taskId = taskIds.get(0);
					entry = logs.get(taskId);
					if (entry==null) {						
						entry = new LogEntry();
						
						// Find task info.
						List<String> t = new ArrayList<String>(1);
						t.add(taskId);
						TaskInfoCommand taskInfoCommand = new TaskInfoCommand(t);
						commands.executeSynergyCommand(workarea, taskInfoCommand);
						List<TaskCompleted> infos = taskInfoCommand.getInformations();
						if (!infos.isEmpty()) {
							entry.setMsg(infos.get(0).getSynopsis());
							entry.setUser(infos.get(0).getResolver());
							entry.setTaskId(infos.get(0).getId());
							entry.setDate(infos.get(0).getDateCompleted()==null ? null : dateFormat.format(infos.get(0).getDateCompleted()));
						}
						logs.put(taskId, entry);
					}
				}
				
				// Deal with no task case (should not happen)
				if (entry==null) {
					entry = logs.get(null);
					if (entry==null) {
						entry = new LogEntry();
						entry.setMsg("Unknown task");
						logs.put(null, entry);
					}
				}
				
				// Find use of the element in the project.
				FindUseCommand command = new FindUseCommand(name, projects, delimiter);
				commands.executeSynergyCommand(workarea, command);
				String pathInProject = command.getPath();
				if (pathInProject!=null) {
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
		}					
		
		return logs.values();
	}

	/**
	 * Write the changelog file.
	 * @param changeLogFile	The changelog file
	 * @param logs			The logs
	 */
	private void writeChangeLog(File changeLogFile, Collection<LogEntry> logs) {
		if (logs!=null) {
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
				if (writer!=null) {
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

	@Override
	public boolean pollChanges(AbstractProject project, Launcher launcher, FilePath path, TaskListener listener) throws IOException, InterruptedException {
		// Get last build.
		AbstractBuild lastBuild = (AbstractBuild) project.getLastBuild();
		
		// Check release.
        if (release==null) {
        	listener.getLogger().println("The release attribute is not set. It is required for change pooling.");
        	return false;
        }
		
		// No last build, build one now.
        if(lastBuild==null) {
            listener.getLogger().println("No existing build. Starting a new one");
            return true;
        }
        
        // Get last build date.
        Calendar date = lastBuild.getTimestamp();     
        
        // Configure commands.
		commands = new Commands();
		commands.setCcmExe(getDescriptor().getCcmExe());
		commands.setCcmUiLog(getDescriptor().getCcmUiLog());
		commands.setCcmEngLog(getDescriptor().getCcmEngLog());
		
		commands.setTaskListener(listener);
		commands.setLauncher(launcher);
		
		try {
			// Start Synergy.
			StartCommand command = new StartCommand(database, engine, username, password, remoteClient);
			commands.executeSynergyCommand(path, command);
			String ccmAddr = command.getCcmAddr();
			commands.setCcmAddr(ccmAddr);
			
			// Find completed tasks.
			FindCompletedSinceDateCommand findCommand = new FindCompletedSinceDateCommand(date, release);
			commands.executeSynergyCommand(path, findCommand);
			List<String> result = findCommand.getTasks();
			return result!=null && !result.isEmpty();
		} catch (SynergyException e) {
			return false;
		} finally {
			// Stop Synergy
			StopCommand stopCommand = new StopCommand();
			try {
				commands.executeSynergyCommand(path, stopCommand);
			} catch (SynergyException e) {
				return false;
			}
		}
	}
	
	

	public String getProject() {
		return project;
	}

	public String getDatabase() {
		return database;
	}

	public String getUsername() {
		return username;
	}

	public String getPassword() {
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
}
