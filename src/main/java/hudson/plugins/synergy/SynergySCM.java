package hudson.plugins.synergy;

import hudson.AbortException;
import hudson.EnvVars;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.Util;
import static hudson.Util.fixEmpty;
import hudson.model.AbstractBuild;
import hudson.model.Job;
import hudson.model.Run;
import hudson.model.StringParameterValue;
import hudson.model.TaskListener;
import hudson.plugins.synergy.SynergyLogEntry.Path;
import hudson.plugins.synergy.impl.CheckFolderModifiedSinceDateCommand;
import hudson.plugins.synergy.impl.CheckoutResult;
import hudson.plugins.synergy.impl.Commands;
import hudson.plugins.synergy.impl.CompareProjectCommand;
import hudson.plugins.synergy.impl.Conflict;
import hudson.plugins.synergy.impl.CopyProjectCommand;
import hudson.plugins.synergy.impl.FindCompletedSinceDateCommand;
import hudson.plugins.synergy.impl.FindProjectGroupingCommand;
import hudson.plugins.synergy.impl.FindProjectInProjectGrouping;
import hudson.plugins.synergy.impl.FindProjectWithReleaseAndState;
import hudson.plugins.synergy.impl.FindUseWithoutVersionCommand;
import hudson.plugins.synergy.impl.GetDelimiterCommand;
import hudson.plugins.synergy.impl.GetProjectAttributeCommand;
import hudson.plugins.synergy.impl.GetProjectInBaselineCommand;
import hudson.plugins.synergy.impl.GetProjectStateCommand;
import hudson.plugins.synergy.impl.ProjectConflicts;
import hudson.plugins.synergy.impl.QueryCommand;
import hudson.plugins.synergy.impl.ReconcileCommand;
import hudson.plugins.synergy.impl.ReconcileCommand.PARAMS;
import hudson.plugins.synergy.impl.RecursiveProjectQueryCommand;
import hudson.plugins.synergy.impl.SetProjectAttributeCommand;
import hudson.plugins.synergy.impl.SetRoleCommand;
import hudson.plugins.synergy.impl.SetSessionPropertyCommand;
import hudson.plugins.synergy.impl.SubProjectQueryCommand;
import hudson.plugins.synergy.impl.SyncCommand;
import hudson.plugins.synergy.impl.SynergyException;
import hudson.plugins.synergy.impl.TaskCompleted;
import hudson.plugins.synergy.impl.TaskInfoCommand;
import hudson.plugins.synergy.impl.TaskShowObjectsCommand;
import hudson.plugins.synergy.impl.UpdateCommand;
import hudson.plugins.synergy.impl.WorkareaSnapshotCommand;
import hudson.plugins.synergy.impl.WriteObjectCommand;
import hudson.plugins.synergy.remote.SynergyQueryService;
import hudson.plugins.synergy.util.QueryUtils;
import hudson.plugins.synergy.util.SessionUtils;
import hudson.scm.ChangeLogParser;
import hudson.scm.ChangeLogSet;
import hudson.scm.PollingResult;
import hudson.scm.PollingResult.Change;
import hudson.scm.SCM;
import hudson.scm.SCMDescriptor;
import hudson.scm.SCMRevisionState;
import hudson.util.FormValidation;
import hudson.util.Secret;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.Serializable;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import net.sf.json.JSONObject;
import org.apache.commons.lang.StringUtils;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;

/**
 * Synergy SCM
 *
 * @author Jean-Noel RIBETTE
 */
public class SynergySCM extends SCM implements Serializable {

  private static final long serialVersionUID = 1L;

  private Collection<? extends String> getOtherProjectMappings(Set<String> fromProjects, String delimiter) {

    Set<String> result = new HashSet<>();

    Set<String> projects = new HashSet<>();
    for (String fromProject : fromProjects) {
      int versionDelimIndex = fromProject.indexOf(delimiter);
        if (versionDelimIndex != -1) {
          projects.add(fromProject.substring(0, versionDelimIndex));
        } else {
          projects.add(fromProject);
        }
    }
    
    
    if (projects.contains("Doktyp") || projects.contains("Doktyp-Root")) {
      result.add("Doktyp");
      result.add("Doktyp-Root");
    }

    if (projects.contains("Doktyp-Maven") || projects.contains("Doktyp")) {
      result.add("Doktyp");
      result.add("Doktyp-Maven");
    }

    if (projects.contains("AT") || projects.contains("AP")) {
      result.add("AT");
      result.add("AP");
    }

    if (projects.contains("EAkte_Maven") || projects.contains("EAkte")) {
      result.add("EAkte_Maven");
      result.add("EAkte");
    }

    if (projects.contains("Avantis_Maven") || projects.contains("Avantis")) {
      result.add("Avantis_Maven");
      result.add("Avantis");
    }

    if (projects.contains("FF_Maven") || projects.contains("FF")) {
      result.add("FF_Maven");
      result.add("FF");
    }

    if (projects.contains("FwkKern_Maven") || projects.contains("FwkKern")) {
      result.add("FwkKern_Maven");
      result.add("FwkKern");
    }

    if (projects.contains("HS_Maven") || projects.contains("HS")) {
      result.add("HS_Maven");
      result.add("HS");
    }

    if (projects.contains("Lib_Maven") || projects.contains("Lib")) {
      result.add("Lib_Maven");
      result.add("Lib");
    }

    if (projects.contains("SE_Maven") || projects.contains("SE")) {
      result.add("SE_Maven");
      result.add("SE");
    }

    if (projects.contains("TDT_Maven") || projects.contains("TDT")) {
      result.add("TDT_Maven");
      result.add("TDT");
    }

    if (projects.contains("TestFwk_Maven") || projects.contains("TestFwk")) {
      result.add("TestFwk_Maven");
      result.add("TestFwk");
    }

    return result;
  }

  @Extension
  public static final class DescriptorImpl extends SCMDescriptor<SynergySCM> {

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

    public DescriptorImpl() {
      super(SynergySCM.class, null);
      load();
    }

    @Override
    public boolean configure(StaplerRequest req, JSONObject formData) throws FormException {
      req.bindJSON(this, formData);
      save();
      return true;
    }

    /**
     * Checks if ccm executable exists.
     *
     * @param value check executable
     * @return Validation
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

    /**
     * @param ccmExe the ccmExe to set
     */
    public void setCcmExe(String ccmExe) {
      this.ccmExe = ccmExe;
    }

    /**
     * @param ccmUiLog the ccmUiLog to set
     */
    public void setCcmUiLog(String ccmUiLog) {
      this.ccmUiLog = ccmUiLog;
    }

    /**
     * @param ccmEngLog the ccmEngLog to set
     */
    public void setCcmEngLog(String ccmEngLog) {
      this.ccmEngLog = ccmEngLog;
    }

    /**
     * @param pathName the pathName to set
     */
    public void setPathName(String pathName) {
      this.pathName = pathName;
    }
  }

  public static final String CCM_SESSION_MAP_FILE_NAME = "ccmSessionMap.properties";

  /**
   * The Synergy project name. This is the raw project name : if the project
   * name contains variable, they are not substituted here.
   */
  private String project;

  /**
   * The Synergy database containing the project.
   */
  private String database;

  /**
   * The Synergy projet release. This is used to create baseline.
   */
  private String release;

  /**
   * The Synergy project update releases. This is used to check updates.
   */
  private String updateReleases;

  /**
   * List of folders used to update project. This is used to check updates.
   */
  private String updateFolders;

  /**
   * try to checkout project if there is no project
   */
  private boolean checkoutProjectIfNotExists = false;

  /**
   * The Synergy project purpose. This is used to create baseline.
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
   * ThemaxLengthQuery
   */
  private String maxQueryLength = "0";

  /**
   * Remote client connection flag.
   */
  private boolean remoteClient;

  /**
   * Detect conflict flag.
   */
  private boolean detectConflict;

  private boolean reconcile;

  /**
   * Deep detect changes flag. Deep detect will check wich project is modified
   * by a task.
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

  private boolean buildEmptyChangeLog = true;

  /**
   * Maintain a workarea for the project. The default value "null" is matched to
   * "true"
   */
  private Boolean maintainWorkarea;

  /**
   * The CCM_HOME location on UNIX systems. This is used to locate ccm
   * executable and set the remote UNIX environment.
   */
  private String ccmHome;

  private String insignificantChangePatterns;

  /**
   * Liefert den Wert von insignificantChangePatterns vom Typ String zurÃÂ¼ck
   *
   * @return insignificantChangePatterns vom Typ String
   */
  public String getInsignificantChangePatterns() {
    return insignificantChangePatterns;
  }

  /**
   * Setzt den Wert von insignificantChangePatterns
   *
   * @param p_insignificantChangePatterns vom Typ String
   */
  public void setInsignificantChangePatterns(String p_insignificantChangePatterns) {
    insignificantChangePatterns = p_insignificantChangePatterns;
  }

  /**
   *
   * @param project project
   * @param database database
   * @param release release
   * @param updateReleases updateReleases
   * @param purpose purpose
   * @param username username
   * @param password password
   * @param engine engine
   * @param oldProject oldProject
   * @param baseline baseline
   * @param oldBaseline oldBaseline
   * @param ccmHome ccmHome
   * @param remoteClient remoteClient
   * @param detectConflict detectConflict
   * @param replaceSubprojects replaceSubprojects
   * @param checkForUpdateWarnings checkForUpdateWarnings
   * @param leaveSessionOpen leaveSessionOpen
   * @param maintainWorkarea maintainWorkarea
   * @param checkTaskModifiedObjects checkTaskModifiedObjects
   * @param maxQueryLength maxQueryLength
   * @param insignificantChangePatterns insignificantChangePatterns
   * @param updateFolders updateFolders
   * @param buildEmptyChangeLog buildEmptyChangeLog
   * @param checkoutProjectIfNotExists checkoutProjectIfNotExists
   */
  @DataBoundConstructor
  public SynergySCM(String project, String database, String release, String updateReleases, String purpose, String username,
          String password, String engine,
          String oldProject, String baseline, String oldBaseline, String ccmHome, boolean remoteClient, boolean detectConflict,
          boolean replaceSubprojects, boolean checkForUpdateWarnings, boolean leaveSessionOpen, Boolean maintainWorkarea,
          boolean checkTaskModifiedObjects, String maxQueryLength, String insignificantChangePatterns, String updateFolders, boolean checkoutProjectIfNotExists, boolean buildEmptyChangeLog) {

    this.project = project;
    this.database = database;
    this.release = release;
    if (StringUtils.isEmpty(updateReleases)) {
      this.updateReleases = release;
    } else {
      this.updateReleases = updateReleases;
    }
    this.updateFolders = updateFolders;
    this.checkoutProjectIfNotExists = checkoutProjectIfNotExists;
    this.buildEmptyChangeLog = buildEmptyChangeLog;
    this.purpose = purpose;
    this.username = username;
    this.password = fixEmpty(password) != null ? Secret.fromString(password) : null;
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
    this.maxQueryLength = StringUtils.defaultIfEmpty(maxQueryLength, "300");
    this.insignificantChangePatterns = insignificantChangePatterns;
  }

  String optionalCheckoutFilePath;

  @DataBoundSetter
  public void setOptionalCheckoutFilePath(String optionalCheckoutFilePath) {
    this.optionalCheckoutFilePath = optionalCheckoutFilePath;
  }

  @Override
  public void checkout(@Nonnull Run<?, ?> build, @Nonnull Launcher launcher, @Nonnull FilePath path, @Nonnull TaskListener listener, @CheckForNull File changeLogFile, @CheckForNull SCMRevisionState scmrs)
          throws IOException, InterruptedException {
    try {
      // Start Synergy.
      setCommands(SessionUtils.openSession(path, this, listener, launcher));

      // Compute dynamic names (replace variable by their values).
      String projectName = computeDynamicValue(build, project);
      listener.getLogger().println("project: " + projectName);
      String oldProjectName = computeDynamicValue(build, oldProject);
      String baselineName = computeDynamicValue(build, baseline);
      String oldBaselineName = computeDynamicValue(build, oldBaseline);

      // Check project state.
      if (project != null && project.length() != 0) {
        // Work on a Synergy project.
        CheckoutResult result = checkoutProject(build, path, changeLogFile, projectName, oldProjectName, listener);
        if (result != null) {
          writeChangeLog(changeLogFile, result.getLogs());
          if (result.getConflicts() != null && !result.getConflicts().isEmpty()) {
            listener.getLogger().println("Error(no project provided) : conflicts detected for project " + projectName);
            throw new AbortException("Error(no project provided) : conflicts detected for project " + projectName);
          }
        }
      } else if (baseline != null && baseline.length() != 0) {
        // Work on a Synergy baseline.
        checkoutBaseline(build, path, changeLogFile, baselineName, oldBaselineName);
      } else if (release != null && release.length() != 0) {
        // Work on a Synergy project grouping.
        CheckoutResult result = checkoutProjectGrouping(build, path, changeLogFile, release, purpose, false);
        if (result != null) {
          writeChangeLog(changeLogFile, result.getLogs());
          if (result.getConflicts() != null && !result.getConflicts().isEmpty()) {
            listener.getLogger().println("Error(no release provided) : conflicts detected for project " + projectName);
            throw new AbortException("Error(no project provided) : conflicts detected for project " + projectName);
          }
        } else {
          throw new AbortException();
        }
      } else {
        listener.getLogger().println("Error : neither project nor baseline nor release is specified");
        throw new AbortException("Error : neither project nor baseline nor release is specified");
      }

      List<String> l_tasks = new ArrayList<>();

      QueryCommand taskQueryCommand = new QueryCommand("is_task_in_rp_of('" + projectName + "') or is_task_in_folder_of(is_folder_in_rp_of('" + projectName + "'))"
              + "and not (is_task_in_baseline_of(is_baseline_in_pg_of(is_project_grouping_of('" + projectName + "'))) or is_dirty_task_in_baseline_of(is_baseline_in_pg_of(is_project_grouping_of('" + projectName + "'))))",
              Arrays.asList(new String[]{"task_number"}));
      getCommands().executeSynergyCommand(path, taskQueryCommand);
      for (final Map<String, String> t : taskQueryCommand.getQueryResult()) {
        l_tasks.add(t.get("task_number"));
      }

      // Since you have access to your build, you can even add build parameters to your build during checkout. 
      // This is useful if you want to pass some information (like a version number) acquired during the polling process to a build job.
      // add a line like this to checkout()
      build.addAction(new TaskOnTopOfBaselineAction(new StringParameterValue(PARAM_TASK_ON_TOP_OF_BASELINE, StringUtils.join(l_tasks, ";"))));

    } catch (SynergyException e) {
      throw new AbortException();
    } finally {
      // Stop Synergy.
      try {
        SessionUtils.closeSession(path, getCommands(), this.isLeaveSessionOpen());
        setCommands(null);
      } catch (SynergyException e) {
        setCommands(null);
        throw new AbortException();
      }
    }

  }
  public static final String PARAM_TASK_ON_TOP_OF_BASELINE = "taskOnTopOfBaseline";

  /**
   * "Checkout" a project grouping
   *
   * @param path	Hudson workarea path
   * @param changeLogFile	Hudson changelog file
   * @param release	The project grouping release
   * @param purpose	The project grouping purpose
   * @param p_afterReconcile boolean checkout after reconcile to prevent endless
   * loop
   * @throws IOException
   * @throws InterruptedException
   * @throws SynergyException
   */
  private CheckoutResult
          checkoutProjectGrouping(Run<?, ?> build, FilePath path, File changeLogFile, String release, String purpose, boolean p_afterReconcile)
          throws IOException,
          InterruptedException, SynergyException {
    // Find project grouping.
    FindProjectGroupingCommand findCommand = new FindProjectGroupingCommand(release, purpose);
    getCommands().executeSynergyCommand(path, findCommand);
    List<String> projectGroupings = findCommand.getProjectGroupings();
    if (projectGroupings.size() != 1) {
      getCommands().getTaskListener().error("Error : multiple or no project grouping found");
      return null;
    }
    String projectGrouping = projectGroupings.get(0);

    // Update members.
    UpdateCommand updateCommand = new UpdateCommand(UpdateCommand.PROJECT_GROUPING, projectGrouping, isReplaceSubprojects());
    getCommands().executeSynergyCommand(path, updateCommand);
    Map<String, List<List<String>>> updates = updateCommand.getUpdates();

    // Generate changelog
    Collection<SynergyLogEntry> logs = generateChangeLog(updates, updateCommand.getRemovedTasks(), projectGrouping, changeLogFile, path);

    // Check update warnings.
    if (isCheckForUpdateWarnings() && updateCommand.isUpdateWarningsExists()) {
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
        if (projectConflicts != null) {
          conflicts.addAll(projectConflicts);
        }
      }
    }

    // TODO: prüfen ob sinnvoll ....
    if (reconcile && updateCommand.isUpdateWarningsExists() && !p_afterReconcile) {
      // Find the project to detect conflicts into.
      FindProjectInProjectGrouping findProjectCommand = new FindProjectInProjectGrouping(projectGrouping);
      getCommands().executeSynergyCommand(path, findProjectCommand);
      List<String> projects = findProjectCommand.getProjects();

      for (String l_project : projects) {

        ReconcileCommand l_reconcileCommand = new ReconcileCommand(l_project, PARAMS.UWA);
        getCommands().executeSynergyCommand(path, l_reconcileCommand);
      }
      // retry checkout after reconcile
      return checkoutProjectGrouping(build, path, changeLogFile, release, purpose, true);

    }

    return new CheckoutResult(conflicts, logs);
  }

  /**
   * "Checkout" a baseline
   *
   * @param path	Hudson workarea path
   * @param changeLogFile	Hudson changelog file
   * @param baselineName	The name of the baseline to checkout
   * @param oldBaselineName	The name of the old baseline (for differential
   * delivery)
   *
   * @throws IOException
   * @throws InterruptedException
   * @throws SynergyException
   */
  private void checkoutBaseline(Run<?, ?> build, FilePath path, File changeLogFile, String baselineName, String oldBaselineName)
          throws IOException, InterruptedException, SynergyException {
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
    Collection<SynergyLogEntry> allEntries = new ArrayList<SynergyLogEntry>();
    for (Map.Entry<String, String> project : projectsMapping.entrySet()) {
      CheckoutResult result = checkoutStaticProject(build, path, changeLogFile, project.getKey(), project.getValue());
      Collection<SynergyLogEntry> entries = result.getLogs();
      if (entries != null) {
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
   * @param path	Hudson workarea path
   * @param changeLogFile	Hudson changelog file
   * @param projectName	The name of the project to checkout
   * @param oldProjectName	The name of the old project (for differential
   * delivery)
   *
   * @throws IOException
   * @throws InterruptedException
   * @throws SynergyException
   */
  private CheckoutResult checkoutStaticProject(Run<?, ?> build, FilePath path, File changeLogFile, String projectName, String oldProjectName)
          throws IOException, InterruptedException, SynergyException {
    // Compute workare path
    String desiredWorkArea = getCleanWorkareaPath(path);

    if (oldProjectName != null && oldProjectName.length() != 0) {
      // Compute difference.
      CompareProjectCommand compareCommand = new CompareProjectCommand(projectName, oldProjectName);
      getCommands().executeSynergyCommand(path, compareCommand);
      List<String> result = compareCommand.getDifferences();
      ArrayList<List<String>> l_newResult = new ArrayList<List<String>>();
      for (String string : result) {
        l_newResult.add(Arrays.asList(string, string));
      }

      Map<String, List<List<String>>> names = new HashMap<String, List<List<String>>>();
      names.put(projectName, l_newResult);
      Set<String> emptySet = Collections.emptySet();

      Collection<SynergyLogEntry> entries = generateChangeLog(names, emptySet, projectName, changeLogFile, path);
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
   * @param path Hudson workarea path
   * @param changeLogFile	Hudson changelog file
   * @param projectName	The name of the project to checkout
   * @param oldProjectName	The name of the old project (for differential
   * delivery)
   *
   * @throws IOException
   * @throws InterruptedException
   * @throws SynergyException
   */
  private CheckoutResult checkoutProject(Run<?, ?> build, FilePath path, File changeLogFile, String projectName, String oldProjectName, TaskListener listener)
          throws IOException, InterruptedException, SynergyException {

    // check project exists
    if (checkoutProjectIfNotExists && !isExistingProject(projectName, path)) {

      if (purpose != null && StringUtils.isNotEmpty(purpose)) {

        FindProjectWithReleaseAndState findProject = new FindProjectWithReleaseAndState(release, "prep", projectName);
        getCommands().executeSynergyCommand(path, findProject);
        List<String> projects = findProject.getProjects();
        if (projects.isEmpty()) {
          // find ProcessRule
          // FindProcessRulesByReleaseAndPurpose findProcessRulesByPurpose = new FindProcessRulesByReleaseAndPurpose(release, purpose);
          // List<String> processRules = findProcessRulesByPurpose.getProcessRules();
          // find baseline Projects
          // FindBaselineProjectByProcessRule findBaselineProjectByProcessRule = new FindBaselineProjectByProcessRule(processRules.get(0), projectName);
          // projects = findBaselineProjectByProcessRule.getProjects();
        }

        if (!"Collaborative Development".equals(purpose)) {
          SetRoleCommand setRoleCommand = new SetRoleCommand(SetRoleCommand.BUILD_MANAGER);
          getCommands().executeSynergyCommand(path, setRoleCommand);
        }
        // get old value of property
        SetSessionPropertyCommand l_setSessionPropertyCommand = new SetSessionPropertyCommand("project_subdir_template", "");
        getCommands().executeSynergyCommand(path, l_setSessionPropertyCommand);
        String l_oldValue = l_setSessionPropertyCommand.getValue();
        // set value
        l_setSessionPropertyCommand = new SetSessionPropertyCommand("project_subdir_template", "\"\"");
        getCommands().executeSynergyCommand(path, l_setSessionPropertyCommand);
        CopyProjectCommand copy = new CopyProjectCommand(purpose, release, StringUtils.chop(StringUtils.removeEnd(path.getRemote(), StringUtils.substringBefore(projectName, "~"))), StringUtils.substringBefore(StringUtils.substringAfter(projectName, "~"), ":"), projects.get(0));
        getCommands().executeSynergyCommand(path, copy);
        // reset value
        l_setSessionPropertyCommand = new SetSessionPropertyCommand("project_subdir_template", l_oldValue);
        getCommands().executeSynergyCommand(path, l_setSessionPropertyCommand);

        // update new checked out project
        UpdateCommand updateCommand = new UpdateCommand(UpdateCommand.PROJECT, projectName, isReplaceSubprojects());
        getCommands().executeSynergyCommand(path, updateCommand);

      } else {
        listener.error("No purpose for project specified!");
      }
    }

    if (isStaticProject(projectName, path)) {
      // Clear workarea.
      path.deleteContents();

      return checkoutStaticProject(build, path, changeLogFile, projectName, oldProjectName);
    } else {
      return checkoutDynamicProject(build, path, changeLogFile, projectName, false);
    }
  }

  private CheckoutResult checkoutDynamicProject(Run<?, ?> build, FilePath path, File changeLogFile, String projectName, boolean p_afterReconcile)
          throws IOException,
          InterruptedException, SynergyException {
    // Configure workarea.
    // Assume a null value means TRUE
    // (as it was the default behavior before the addition of this parameter)
    if (shouldMaintainWorkarea()) {
      setAbsoluteWorkarea(path, projectName);
    }

    // Update members.
    UpdateCommand updateCommand = new UpdateCommand(UpdateCommand.PROJECT, projectName, isReplaceSubprojects());
    try {
      getCommands().executeSynergyCommand(path, updateCommand);
    } catch (SynergyException e) {
      // ccm update failed. Let's try a sync before we give up completely.
      SyncCommand syncCommand = new SyncCommand(projectName, true);
      getCommands().executeSynergyCommand(path, syncCommand);

      // Now update again. If this doesn't work, just die.
      getCommands().executeSynergyCommand(path, updateCommand);
    }
    Map<String, List<List<String>>> updates = updateCommand.getUpdates();

    // Generate changelog
    String pgName = updateCommand.getPgName();

    Collection<SynergyLogEntry> logs = generateChangeLog(updates, updateCommand.getRemovedTasks(), projectName, changeLogFile, path, pgName);

    // Check update warnings.
    if (isCheckForUpdateWarnings() && updateCommand.isUpdateWarningsExists()) {
      return new CheckoutResult(updateCommand.getConflicts(), logs);
    }

    // Check conflicts.
    List<Conflict> conflicts = null;
    if (detectConflict) {
      ProjectConflicts conflictsCommand = new ProjectConflicts(projectName);
      getCommands().executeSynergyCommand(path, conflictsCommand);
      conflicts = conflictsCommand.getConflicts();
    }

    if (reconcile && updateCommand.isUpdateWarningsExists() && !p_afterReconcile) {
      ReconcileCommand l_reconcileCommand = new ReconcileCommand(projectName, PARAMS.UWA);
      getCommands().executeSynergyCommand(path, l_reconcileCommand);
      return checkoutDynamicProject(build, path, changeLogFile, projectName, true);
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
  private void copyEntries(FilePath path, Collection<SynergyLogEntry> entries) throws IOException, InterruptedException, SynergyException {
    // Iterate over the tasks.
    for (SynergyLogEntry entry : entries) {
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
   * Replace an expression in the form ${name} in the given String by the value
   * of the matching environment variable.
   */
  private String computeDynamicValue(Run<?, ?> build, String parameterizedValue)
          throws IllegalStateException, InterruptedException, IOException {
    if (parameterizedValue != null && parameterizedValue.indexOf("${") != -1) {
      int start = parameterizedValue.indexOf("${");
      int end = parameterizedValue.indexOf("}", start);
      String parameter = parameterizedValue.substring(start + 2, end);
      String value = (String) build.getEnvironment(TaskListener.NULL).get(parameter);
      if (value == null) {
        throw new IllegalStateException(parameter);
      }
      return parameterizedValue.substring(0, start) + value
              + (parameterizedValue.length() > end + 1 ? parameterizedValue.substring(end + 1) : "");
    } else {
      return parameterizedValue;
    }
  }

  /**
   * Check if project exists
   *
   * @return true if the project exists
   */
  private boolean isExistingProject(String project, FilePath workspace) throws IOException, InterruptedException {
    // Get project state.
    // FIXME: dynamic Value
    GetProjectStateCommand command = new GetProjectStateCommand(project);
    try {
      getCommands().executeSynergyCommand(workspace, command, false);
    } catch (SynergyException ex) {
      return false;
    }
    return true;
  }

  /**
   * Check the project state.
   *
   * @return true if the project is a static project
   */
  private boolean isStaticProject(String project, FilePath workspace) throws IOException, InterruptedException, SynergyException {
    // Get project state.
    // FIXME: dynamic Value
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
   * @param project	The Synergy project name
   * @param relative	Should the workarea be relative or asbolute
   * @param launcher	The Hudson launcher to use to launch commands
   * @param workspace	The Hudon project workspace path
   * @param ccmAddr	The current Synergy sesison address
   * @param listener	The Hudson build listener
   * @return True if the workarea was configured sucessfully
   * @throws IOException
   * @throws InterruptedException
   * @throws SynergyException
   */
  private void configureWorkarea(String project, boolean relative, FilePath workspace)
          throws IOException, InterruptedException, SynergyException {
    // Check maintain workarea.
    GetProjectAttributeCommand getProjectAttributeCommand
            = new GetProjectAttributeCommand(project, GetProjectAttributeCommand.MAINTAIN_WORKAREA);
    getCommands().executeSynergyCommand(workspace, getProjectAttributeCommand);
    String maintainWorkArea = getProjectAttributeCommand.getValue();
    boolean changeMaintain = false;

    // Check relative/absolute wo.
    if (relative) {
      getProjectAttributeCommand = new GetProjectAttributeCommand(project, GetProjectAttributeCommand.RELATIVE);
      getCommands().executeSynergyCommand(workspace, getProjectAttributeCommand);
      String relativeWorkArea = getProjectAttributeCommand.getValue();

      if (!"TRUE".equals(relativeWorkArea)) {
        // If asked for relative workarea, and workarea is not relative, set relative workarea.
        SetProjectAttributeCommand setProjectAttributeCommand
                = new SetProjectAttributeCommand(project, GetProjectAttributeCommand.RELATIVE, "TRUE");
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
      SetProjectAttributeCommand setProjectAttributeCommand
              = new SetProjectAttributeCommand(project, GetProjectAttributeCommand.WORKAREA_PATH, desiredWorkArea);
      getCommands().executeSynergyCommand(workspace, setProjectAttributeCommand);
    }

    if (!"TRUE".equals(maintainWorkArea)) {
      // If workarea is not maintain, maintain it.
      SetProjectAttributeCommand setProjectAttributeCommand
              = new SetProjectAttributeCommand(project, GetProjectAttributeCommand.MAINTAIN_WORKAREA, "TRUE");
      getCommands().executeSynergyCommand(workspace, setProjectAttributeCommand);
      changeMaintain = true;
    }

  }

  /**
   * Configure the Synergy workarea for the main project and the subprojects.
   *
   * @param path	The Hudson projet workspace path
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
      if (e.getStatus() != 1 && e.getStatus() != 6) {
        System.out.println("ERROR: " + command + " EXITCODE :" + e.getStatus());
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
   *
   * @param build instance of build
   * @return File of revision
   */
  public static File getRevisionFile(Run<?, ?> build) {
    return new File(build.getRootDir(), "revision.txt");
  }

  /**
   * Reads the revision file of the specified build.
   *
   * @return map from {@link SvnInfo#url Subversion URL} to its revision.
   */
  static Map<String, Long> parseRevisionFile(Run<?, ?> build) throws IOException {
    Map<String, Long> revisions = new HashMap<String, Long>(); // module ->
    // revision
    { // read the revision file of the last build
      File file = getRevisionFile(build);
      if (!file.exists()) // nothing to compare against
      {
        return revisions;
      }

      BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(file), build.getCharset()));
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
   * @param names	Names of the elements that have changed
   * @param projects	Name of the Synergy project being build and that may
   * contain changes
   * @param changeLogFile	File to write the changelog into
   * @param workarea	The Workarea path
   * @return
   * @throws IOException
   * @throws InterruptedException
   * @throws SynergyException
   */
  private Collection<SynergyLogEntry> generateChangeLog(Map<String, List<List<String>>> names, Set<String> removedTasks, String projectName, File changeLogFile, FilePath workarea)
          throws IOException, InterruptedException, SynergyException {
    return generateChangeLog(names, removedTasks, projectName, changeLogFile, workarea, null);
  }

  /**
   * Generate the changelog.
   *
   * @param names	Names of the elements that have changed
   * @param projects	Name of the Synergy project being build and that may
   * contain changes
   * @param changeLogFile	File to write the changelog into
   * @param workarea	The Workarea path
   * @param pgName	Optional name of the project grouping to which the project
   * belongs
   * @return
   * @throws IOException
   * @throws InterruptedException
   * @throws SynergyException
   */
  private Collection<SynergyLogEntry> generateChangeLog(Map<String, List<List<String>>> names, Set<String> removedtasks, String projectName, File changeLogFile, FilePath workarea,
          String pgName) throws IOException, InterruptedException, SynergyException {
    // Find information about the element.
    Map<String, SynergyLogEntry> logs = new HashMap<String, SynergyLogEntry>();

    if (names != null) {
      // bis der andere Service funktioniert .... 
      // SynergyQueryService queryService = new DummyQueryService();

      // Locate Synergy Query Server
      /*try {
      Registry registry = LocateRegistry.getRegistry(engine.startsWith("http") ? new URL(engine).getHost() : engine);
      SynergyQueryService queryService = (SynergyQueryService) registry.lookup("SynergyQueryService");
      } catch(NotBoundException e) {
      // ignore and continue without project path information
      }*/
      // Get project grouping object
      QueryCommand queryCommand
              = new QueryCommand("is_project_grouping_of('" + projectName + "')", Arrays.asList(new String[]{"objectname", "release",
        "member_status", "status"}));
      getCommands().executeSynergyCommand(workarea, queryCommand);
      if (queryCommand.getQueryResult().isEmpty()) {
        return logs.values();
      }

      String memberStatus = queryCommand.getQueryResult().get(0).get("member_status");
      String projectGrouping = queryCommand.getQueryResult().get(0).get("objectname");

      if (release.trim().length() == 0) {
        release = queryCommand.getQueryResult().get(0).get("release");
      }

      // Get Baseline
      String l_baseline = null;
      String matching = null;
      queryCommand = new QueryCommand("is_baseline_in_pg_of('" + projectGrouping + "')", null);
      getCommands().executeSynergyCommand(workarea, queryCommand);
      if (queryCommand.getQueryResult().size() == 1) {
        l_baseline = queryCommand.getQueryResult().get(0).get("objectname");
      } else {
        queryCommand = new QueryCommand("cvtype='process_rule' and release='" + release + "' and member_status='" + memberStatus + "'",
                Arrays.asList(new String[]{"baseline_version_matching"}));
        getCommands().executeSynergyCommand(workarea, queryCommand);
        if (queryCommand.getQueryResult().size() == 1) {
          matching = queryCommand.getQueryResult().get(0).get("baseline_version_matching");
        }
      }

      // Find all tasks and objects in project grouping and not in baseline
      String taskquery = "(is_saved_task_in_pg_of('" + projectGrouping + "') or is_added_task_in_pg_of('" + projectGrouping + "'))";
      if (l_baseline != null) {
        taskquery += " and not (is_task_in_baseline_of('" + l_baseline + "') or is_dirty_task_in_baseline_of('" + l_baseline + "'))";
      } else if (matching != null) {
        taskquery += " and not is_task_in_folder_of(cvtype='folder' and description='" + matching + "')";
      }
      taskquery = "status != 'task_automatic' and (" + taskquery + ")";
      String objectquery = "cvtype != 'jar' and is_associated_cv_of(" + taskquery + ")";
      queryCommand = new QueryCommand(objectquery, Arrays.asList(new String[]{"objectname", "task"}));
      getCommands().executeSynergyCommand(workarea, queryCommand);
      Map<String, String> objects = new HashMap<String, String>();
      for (Map<String, String> object : queryCommand.getQueryResult()) {
        objects.put(object.get("objectname"), object.get("task"));
      }
      queryCommand
              = new QueryCommand(taskquery, Arrays.asList(new String[]{"displayname", "task_synopsis", "resolver", "completion_date", "release"}));
      getCommands().executeSynergyCommand(workarea, queryCommand);
      Map<String, Map<String, String>> tasks = new HashMap<String, Map<String, String>>();
      for (Map<String, String> task : queryCommand.getQueryResult()) {
        tasks.put(task.get("displayname"), task);
      }

      for (Entry<String, List<List<String>>> entry : names.entrySet()) {
        for (List<String> name : entry.getValue()) {
          String l_newName = name.get(0);
          if (!isOfTypeJar(l_newName)) {
            SynergyLogEntry log;
            Path l_path = new Path();
            l_path.setId(l_newName);
            // geändertes Object "name" nicht in der Object-Manege - dann kommt es aus der Baseline oder die Task wurde aus dem Projekt entfernt ..... 
            if (objects.containsKey(l_newName) || isObjectInBaseline(l_newName, l_baseline, workarea, objects, tasks)) {
              l_path.setValue(l_newName);
              if (l_newName.equals(name.get(1))) {
                l_path.setAction("A");
              } else {
                l_path.setAction("E");
              }
              List<String> taskIds = objects.get(l_newName) != null ? Arrays.asList(objects.get(l_newName).split(",")) : null;
              if (taskIds != null && !taskIds.isEmpty()) {
                for (String taskId : taskIds) {
                  log = logs.get(taskId);
                  if (log == null) {
                    // only add changes associated to tasks in pg
                    Map<String, String> task = tasks.get(taskId);
                    if (task != null) {
                      log = new SynergyLogEntry();
                      log.setMsg(task.get("task_synopsis"));
                      log.setUser(task.get("resolver"));
                      log.setTaskId(task.get("displayname"));
                      log.setDate(task.get("completion_date"));
                      log.setAction("A");
                      // Erweiterung Task-Release
                      log.setVersion(task.get("release"));
                      logs.put(taskId, log);
                    }
                  }
                  if (log != null) {
                    log.addPath(l_path);
                  }
                }
              }
            }
          }
        }
      }

      // removed Tasks
      if (!removedtasks.isEmpty()) {

        TaskInfoCommand taskInfoCommand = new TaskInfoCommand(new ArrayList(removedtasks));

        getCommands().executeSynergyCommand(workarea, taskInfoCommand);
        // Alle tasks wurden entfernt ..... 
        for (TaskCompleted task : taskInfoCommand.getInformations()) {

          String taskId = task.getId();
          SynergyLogEntry log = logs.get(taskId);

          log = new SynergyLogEntry();
          log.setMsg(task.getSynopsis());
          log.setUser(task.getResolver());
          log.setTaskId(task.getId());
          log.setDate(task.getDateCompleted().toString());
          log.setAction("D");
          // Erweiterung Task-Release
          log.setVersion(task.getRelease());
          logs.put(taskId, log);
        }
      }
    }

    return logs.values();

  }

  private boolean isOfTypeJar(String p_4partName) {
    SynergyPublisher.SynergyObject synergyObject = new SynergyPublisher.SynergyObject(p_4partName);

    return "jar".equals(synergyObject.type);
  }

  private boolean isObjectInBaseline(String l_newName, String l_baseline, FilePath l_path, Map<String, String> objects, Map<String, Map<String, String>> tasks) throws IOException, InterruptedException, SynergyException {
    // prüfen ob Object in der Baseline

    String l_query = "type = 'task' and has_associated_object('" + l_newName + "') "
            + " and (is_task_in_baseline_of('" + l_baseline + "') or is_dirty_task_in_baseline_of('" + l_baseline + "'))";
    QueryCommand queryCommand = new QueryCommand(l_query, Arrays.asList(new String[]{"displayname", "task_synopsis", "resolver", "completion_date", "release"}));
    getCommands().executeSynergyCommand(l_path, queryCommand);

    if (!queryCommand.getQueryResult().isEmpty()) {
      for (Map<String, String> entry : queryCommand.getQueryResult()) {
        if (objects.get(l_newName) == null) {
          objects.put(l_newName, entry.get("displayname"));
        } else {
          objects.put(l_newName, objects.get(l_newName) + "," + entry.get("displayname"));
        }
        tasks.put(entry.get("displayname"), entry);
      }
      return true;
    }
    return false;
  }

  /**
   * Write the changelog file.
   *
   * @param changeLogFile	The changelog file
   * @param logs	The logs
   */
  private void writeChangeLog(File changeLogFile, Collection<SynergyLogEntry> logs) {
    if (logs != null) {
      PrintWriter writer = null;
      try {
        writer = new PrintWriter(changeLogFile, "UTF-8");
        writer.println("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
        writer.println("<log>");

        for (SynergyLogEntry log : logs) {
          writer.println(String.format("\t<logentry revision=\"%s\">", log.getTaskId()));
          writer.println(String.format("\t\t<task>%s</task>", log.getTaskId()));
          writer.println(String.format("\t\t<author>%s</author>", log.getUser()));
          writer.println(String.format("\t\t<date>%s</date>", log.getDate()));
          writer.println(String.format("\t\t<version>%s</version>", log.getVersion()));
          writer.println(String.format("\t\t<msg><![CDATA[%s]]></msg>", log.getMsg()));
          writer.println(String.format("\t\t<action>%s</action>", (log.getAction())));

          writer.println("\t\t<paths>");
          for (SynergyLogEntry.Path path : log.getPaths()) {
            writer.println(String.format("\t\t\t<path action=\"%s\">%s</path>", path.getAction(), path.getValue()));
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
    return (DescriptorImpl) super.getDescriptor();
  }

  @Override
  public boolean requiresWorkspaceForPolling() {
    return true;
  }

  private static final class SynergyRevisionState extends SCMRevisionState {

    final Calendar date;

    SynergyRevisionState(Calendar date) {
      this.date = date;
    }
  }

  @Override
  public @CheckForNull
  SCMRevisionState calcRevisionsFromBuild(@Nonnull Run<?, ?> run, @Nullable FilePath workspace, @Nonnull Launcher launcher, @Nonnull TaskListener listener) throws IOException,
          InterruptedException {
    // Fixme: use not timestamp of started build
    // max of timestamp an completiondate of changes
    if (run instanceof AbstractBuild) {

      AbstractBuild build = (AbstractBuild) run;
      ChangeLogSet<? extends ChangeLogSet.Entry> l_changeSet = build.getChangeSet();
      long l_lastTimestamp = run.getTimestamp().getTimeInMillis();
      for (ChangeLogSet.Entry l_entry : l_changeSet) {
        long l_timestamp = l_entry.getTimestamp();
        if (l_lastTimestamp < l_timestamp) {
          l_lastTimestamp = l_timestamp;
        }
      }
      // return new SynergyRevisionState(new Calendar(l_lastTimestamp));
    }

    return new SynergyRevisionState(run.getTimestamp());
  }

  @Override
  public PollingResult compareRemoteRevisionWith(@Nonnull Job<?, ?> project, @Nullable Launcher launcher, @Nullable FilePath path, @Nonnull TaskListener listener, @Nonnull SCMRevisionState baseline) throws IOException, InterruptedException {

    // Check updateRelease or folders for polling.
    if (StringUtils.isEmpty(updateReleases) && StringUtils.isEmpty(updateFolders)) {
      listener.getLogger().println("The updateReleases attribute / or updateFolders attribute is not set. It is required for change pooling.");
      return PollingResult.NO_CHANGES;
    }

    // This little check ensures changes are not found while building, as
    // this with many concurrent builds and polling leads to issues where
    // we saw a build was long in the queue, and when started, the polling found
    // changed a did schedule the same build with the same changes as last time
    // between the last one started and finished.
    // Basically this disables polling while the job has a build in the queue or
    // the project is actually building
    if (project.isInQueue() || project.isBuilding()) {
      listener.getLogger().println("A build already in queue or buildung - cancelling poll");
      return PollingResult.NO_CHANGES;
    }

    if (baseline == SCMRevisionState.NONE
            // appears that other instances of None occur - its not a singleton.
            // so do a (fugly) class check.
            || baseline.getClass() != SynergyRevisionState.class) {
      listener.getLogger().println("No previous build State - start a new build");
      return PollingResult.BUILD_NOW;
    }

    // Configure commands.
    setCommands(null);
    try {
      // Start Synergy.
      setCommands(SessionUtils.openSession(path, this, listener, launcher));

      // check for changed folders since last SCMRevisionState.
      if (StringUtils.isNotEmpty(updateFolders)) {
        CheckFolderModifiedSinceDateCommand checkFolderModifiedSinceDateCommand = new CheckFolderModifiedSinceDateCommand(((SynergyRevisionState) baseline).date, updateFolders);
        getCommands().executeSynergyCommand(path, checkFolderModifiedSinceDateCommand);
        if (checkFolderModifiedSinceDateCommand.isModified()) {
          return PollingResult.SIGNIFICANT;
        }
      }
      // Find completed tasks since last SCMRevisionState.
      if (StringUtils.isNotEmpty(updateReleases)) {
        FindCompletedSinceDateCommand findCommand = new FindCompletedSinceDateCommand(((SynergyRevisionState) baseline).date, updateReleases);
        getCommands().executeSynergyCommand(path, findCommand);
        List<String> result = findCommand.getTasks();

        if (checkTaskModifiedObjects) {
          return checkTaskModifiedObjects(result, path, listener, launcher);
        } else {
          return result != null && !result.isEmpty() ? PollingResult.SIGNIFICANT : PollingResult.NO_CHANGES;
        }
      }
      return PollingResult.NO_CHANGES;
    } catch (SynergyException e) {
      return PollingResult.NO_CHANGES;
    } finally {
      // Stop Synergy
      try {
        SessionUtils.closeSession(path, getCommands(), this.isLeaveSessionOpen());
        setCommands(null);
      } catch (SynergyException e) {
        setCommands(null);
        return PollingResult.NO_CHANGES;
      }
    }
  }

  /**
   * Check if the specified taks modify the current project.
   *
   * @param tasks	A list of task number
   * @param path	The current project path.
   * @param p_listener to log
   * @param p_launcher the launcher needed to resolve environment variables
   * @return	If a task in the list modify the current project and what kind of
   * change
   * @throws SynergyException
   * @throws InterruptedException
   * @throws IOException
   */
  private PollingResult checkTaskModifiedObjects(List<String> tasks, FilePath path, TaskListener p_listener, Launcher p_launcher) throws SynergyException,
          InterruptedException,
          IOException {
    if (tasks == null || tasks.isEmpty()) {
      return PollingResult.NO_CHANGES;
    }

    // Find if the task updated some files
    TaskShowObjectsCommand showObjectsCommand = new TaskShowObjectsCommand(tasks);
    getCommands().executeSynergyCommand(path, showObjectsCommand);
    List<String> modifiedObjects = showObjectsCommand.getObjects();

    // Get project use of those files
    GetDelimiterCommand getDelim = new GetDelimiterCommand();
    getCommands().executeSynergyCommand(path, getDelim);
    String delimiter = getDelim.getDelimiter();

    String l_projectName = Util.replaceMacro(this.project, EnvVars.getRemote(p_launcher.getChannel()));
    Set<String> projects = new HashSet<String>();
    projects.add(l_projectName);

    // Subprojekte bestimmen, falls Ergebnismenge nicht leer
    if (!modifiedObjects.isEmpty()) {

      RecursiveProjectQueryCommand recursiveProjectCommand = new RecursiveProjectQueryCommand(l_projectName);
      getCommands().executeSynergyCommand(path, recursiveProjectCommand);
      List<String> l_subProjects = recursiveProjectCommand.getSubProjects();
      projects.addAll(l_subProjects);

      // add additional projectname mapping
      projects.addAll(getOtherProjectMappings(projects, delimiter));
      p_listener.getLogger().println("used projects: "+ projects);
      
      // performanceoptimierung wegen zusammenfassung der Anfragen
      // Sublisten erzeugen
      List<List<String>> l_optimizedQueryObjects = QueryUtils.createOptimizedSubLists(new HashSet<String>(modifiedObjects), maxQueryLength);

      PollingResult l_result = PollingResult.NO_CHANGES;
      for (List<String> modifiedObject : l_optimizedQueryObjects) {
        // hier eine unterscheidung auf wichitige und unwichtige objekte
        // standard-ignore-liste verwenden oder konfigurieren
        FindUseWithoutVersionCommand findUseCommand = new FindUseWithoutVersionCommand(modifiedObject, projects, delimiter);
        getCommands().executeSynergyCommand(path, findUseCommand);
        if (!findUseCommand.getPath().isEmpty()) {
          l_result = isOneObjectSignificant(findUseCommand.getPath(), p_listener) ? PollingResult.SIGNIFICANT : new PollingResult(
                  Change.INSIGNIFICANT);
        }
        // Abbrechen sobald eine signifikante Aenderung gefunden wurde
        if (l_result.equals(PollingResult.SIGNIFICANT)) {
          break;
        }
      }
      p_listener.getLogger().println("Result checking significant changes: " + l_result.change);
      return l_result;
    }
    return PollingResult.NO_CHANGES;
  }

  /**
   * @param p_map Map with objects
   * @param p_listener to log
   * @return true if any significant change
   */
  protected boolean isOneObjectSignificant(Map<String, String> p_map, TaskListener p_listener) {
    if (StringUtils.isEmpty(insignificantChangePatterns) && !p_map.isEmpty()) {
      return true;
    }
    for (Entry<String, String> l_string : p_map.entrySet()) {
      boolean l_return = false;
      // pruefe das der Entry auf keines der Pattern passt
      for (String pattern : StringUtils.split(insignificantChangePatterns, ";")) {
        if (StringUtils.contains(l_string.getKey(), pattern)) {
          p_listener.getLogger().println("File interpreted as non significant change: " + l_string.getValue());
          l_return = true;
          break;
        }
      }
      if (!l_return) {
        return true;
      }
    }

    return false;
  }

  /**
   * Returns the raw project name. If the project name contains variable, they
   * are not substituted here.
   *
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

  public String getUpdateReleases() {
    return updateReleases;
  }

  public void setUpdateFolders(String updateFolders) {
    this.updateFolders = updateFolders;
  }

  public void setCheckoutProjectIfNotExists(boolean checkoutProjectIfNotExists) {
    this.checkoutProjectIfNotExists = checkoutProjectIfNotExists;
  }

  public String getUpdateFolders() {
    return updateFolders;
  }

  public boolean isCheckoutProjectIfNotExists() {
    return checkoutProjectIfNotExists;
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

  public boolean isCheckForUpdateWarnings() {
    return checkForUpdateWarnings;
  }

  public void setCheckForUpdateWarnings(boolean checkForUpdateWarnings) {
    this.checkForUpdateWarnings = checkForUpdateWarnings;
  }

  public boolean isBuildEmptyChangelog() {
    return buildEmptyChangeLog;
  }

  public void setBuildEmptyChangelog(boolean buildEmptyChangeLog) {
    this.buildEmptyChangeLog = buildEmptyChangeLog;
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
    return maintainWorkarea == null || maintainWorkarea.booleanValue();
  }

  public boolean isCheckTaskModifiedObjects() {
    return checkTaskModifiedObjects;
  }

  public void setCheckTaskModifiedObjects(boolean checkTaskModifiedObjects) {
    this.checkTaskModifiedObjects = checkTaskModifiedObjects;
  }

  public String getMaxQueryLength() {
    return maxQueryLength;
  }

  public void setMaxQueryLength(String maxQueryLength) {
    this.maxQueryLength = maxQueryLength;
  }

  private void setCommands(Commands commands) {
    if (this.commands == null) {
      this.commands = new ThreadLocal<Commands>();
    }
    this.commands.set(commands);
    if (commands == null) {
      this.commands.remove();
    }
  }

  private Commands getCommands() {
    if (this.commands == null) {
      this.commands = new ThreadLocal<Commands>();
    }
    return commands.get();
  }

  private static class DummyQueryService implements SynergyQueryService {

    public DummyQueryService() {
    }

    @Override
    public Map<String, String> getProjectMembers(String project, boolean subprojects) throws RemoteException {
      return new HashMap<>();
    }
  }
}
