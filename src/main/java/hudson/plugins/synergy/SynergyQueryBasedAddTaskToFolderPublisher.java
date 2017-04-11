package hudson.plugins.synergy;

import hudson.EnvVars;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.Util;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.BuildListener;
import hudson.model.Result;
import hudson.plugins.synergy.impl.AddTasksToFolderCommand;
import hudson.plugins.synergy.impl.Commands;
import hudson.plugins.synergy.impl.QueryCommand;
import hudson.plugins.synergy.impl.SetRoleCommand;
import hudson.plugins.synergy.impl.SynergyException;
import hudson.plugins.synergy.util.QueryUtils;
import hudson.plugins.synergy.util.SessionUtils;
import hudson.scm.SCM;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Notifier;
import hudson.tasks.Publisher;
import hudson.util.ListBoxModel;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import org.kohsuke.stapler.DataBoundConstructor;

public class SynergyQueryBasedAddTaskToFolderPublisher extends Notifier {

  private String queryStringDestinationFolder;
  private String queryStringTasks;

  /**
   * @return the simulateOnly
   */
  public boolean isSimulateOnly() {
    return simulateOnly;
  }

  /**
   * @param simulateOnly the simulateOnly to set
   */
  public void setSimulateOnly(boolean simulateOnly) {
    this.simulateOnly = simulateOnly;
  }

  /**
   * @return the queryStringDestinationFolder
   */
  public String getQueryStringDestinationFolder() {
    return queryStringDestinationFolder;
  }

  /**
   * @param queryStringDestinationFolder the queryStringDestinationFolder to set
   */
  public void setQueryStringDestinationFolder(String queryStringDestinationFolder) {
    this.queryStringDestinationFolder = queryStringDestinationFolder;
  }

  /**
   * @return the queryStringTasks
   */
  public String getQueryStringTasks() {
    return queryStringTasks;
  }

  /**
   * @param queryStringTasks the queryStringTasks to set
   */
  public void setQueryStringTasks(String queryStringTasks) {
    this.queryStringTasks = queryStringTasks;
  }


  @Extension
  public static class DescriptorImpl extends BuildStepDescriptor<Publisher> {

    public DescriptorImpl() {
      super(SynergyQueryBasedAddTaskToFolderPublisher.class);
    }

    @Override
    public String getDisplayName() {
      return "Synergy query based add task to folder";
    }

    @Override
    public boolean isApplicable(Class<? extends AbstractProject> jobType) {
      return true;
    }

    public ListBoxModel doFillWhenToAddItems() {
      ListBoxModel items = new ListBoxModel();
      items.add(Result.SUCCESS.toString(), Result.SUCCESS.toString());
      items.add(Result.UNSTABLE.toString(), Result.UNSTABLE.toString());
      items.add(Result.FAILURE.toString(), Result.FAILURE.toString());
      return items;
    }

  }

  /**
   * Should the baseline be published
   */
  private String whenToAdd;

  /**
   * simulate only
   */
  private boolean simulateOnly;

  @DataBoundConstructor
  public SynergyQueryBasedAddTaskToFolderPublisher(String whenToAdd, Boolean simulateOnly, String queryStringDestinationFolder, String queryStringTasks ) {
    this.whenToAdd = whenToAdd;
    this.simulateOnly = simulateOnly;
    this.queryStringDestinationFolder = queryStringDestinationFolder;
    this.queryStringTasks = queryStringTasks;
  }

  @Override
  public BuildStepMonitor getRequiredMonitorService() {
    return BuildStepMonitor.STEP;
  }

  @Override
  public boolean perform(AbstractBuild<?, ?> build, Launcher launcher,
          BuildListener listener) throws InterruptedException, IOException {
    // Check SCM used.
    SCM scm = build.getParent().getScm();
    if (!(scm instanceof SynergySCM)) {
      listener.getLogger().println("No add task to folder for non Synergy project");
      return false;
    }

    // Check what needs to be done.
    Result resultWhenToAdd = Result.fromString(whenToAdd);
    Result buildResult = build.getResult();
    boolean buildSucess = buildResult != null ? buildResult.isBetterOrEqualTo(resultWhenToAdd) : false;

    // Check if we need to go on.
    if (buildSucess) {
      listener.getLogger().println(build.getResult() + " is better or equal than " + resultWhenToAdd);

      // Get Synergy parameters.
      SynergySCM synergySCM = (SynergySCM) scm;
      FilePath path = build.getWorkspace();

      Commands commands = null;

      try {
        // Start Synergy.
        commands = SessionUtils.openSession(path, synergySCM, listener, launcher);

        // Become build manager.
        SetRoleCommand setRoleCommand = new SetRoleCommand(SetRoleCommand.BUILD_MANAGER);
        commands.executeSynergyCommand(path, setRoleCommand);

        //Hole Folder-ID des Zielfolders
        String folderIdQuery = getQueryStringDestinationFolder();
        QueryCommand folderQueryCommand = new QueryCommand(folderIdQuery, Arrays.asList(new String[]{FORMAT_STRING_OBJECTNAME}));
        commands.executeSynergyCommand(path, folderQueryCommand);

        List<Map<String, String>> l_resultFolder = folderQueryCommand.getQueryResult();
        if (l_resultFolder.size() != 1) {
          listener.getLogger().println("Folder query: '" + getQueryStringDestinationFolder() + "' liefert kein eindeutiges Ergebnis.");
          return false;
        }
        Map<String, String> firstResult = l_resultFolder.get(0);
        String folderID = firstResult.get(FORMAT_STRING_OBJECTNAME);
        listener.getLogger().println("Folder: "+folderID);

                String l_projectName = Util.replaceMacro(synergySCM.getProject(), EnvVars.getRemote(launcher.getChannel()));
        String taskQuery = getQueryStringTasks() + " and is_task_in_folder_of(is_folder_in_rp_of('"+l_projectName+"')) and not is_task_in_folder_of('"+folderID+"')";

        List<String> tasks = new ArrayList<String>();
        QueryCommand taskQueryCommand = new QueryCommand(taskQuery, Arrays.asList(new String[]{"task_number", "task_synopsis", "task_description", "resolver", "status", "release"}));
        commands.executeSynergyCommand(path, taskQueryCommand);
        for (final Map<String, String> t : taskQueryCommand.getQueryResult()) {
          listener.getLogger().println(t.toString());
          tasks.add(t.get("task_number"));
          listener.getLogger().println("Task: "+t.toString());
        }

        // optimize for max querylength
        String maxQueryLength = synergySCM.getMaxQueryLength();
        if (!tasks.isEmpty()) {
          List<List<String>> optimizedSubLists = QueryUtils.createOptimizedSubLists(new HashSet<String>(tasks), maxQueryLength);
          for (List<String> optimizedSubList : optimizedSubLists) {
            AddTasksToFolderCommand addTaskToFolderCommand = new AddTasksToFolderCommand(optimizedSubList, folderID);
            if (isSimulateOnly()) {
              listener.getLogger().println("Command to execute: " + addTaskToFolderCommand.toString());
            } else {
              commands.executeSynergyCommand(path, addTaskToFolderCommand);
            }
          }
        }
      } catch (SynergyException e) {
        return false;
      } finally {
        // Stop Synergy.
        try {
          if (commands != null) {
            SessionUtils.closeSession(path, commands, synergySCM.isLeaveSessionOpen());
          }
        } catch (SynergyException e) {
          return false;
        }
      }
    } else {
      listener.getLogger().println(build.getResult() + " is worse than " + resultWhenToAdd);
    }
    return true;
  }
  public static final String FORMAT_STRING_OBJECTNAME = "objectname";

  public void setwhenToAdd(String whenToAdd) {
    this.whenToAdd = whenToAdd;
  }

  public String getWhenToAdd() {
    return whenToAdd;
  }

}
