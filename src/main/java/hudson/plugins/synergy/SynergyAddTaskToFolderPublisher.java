package hudson.plugins.synergy;

import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.BuildListener;
import hudson.model.ParameterValue;
import hudson.model.Result;
import hudson.model.StringParameterValue;
import hudson.plugins.synergy.impl.AddTasksToFolderCommand;
import hudson.plugins.synergy.impl.Commands;
import hudson.plugins.synergy.impl.GetFolderIDCommand;
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
import org.apache.commons.lang.StringUtils;
import org.kohsuke.stapler.DataBoundConstructor;

public class SynergyAddTaskToFolderPublisher extends Notifier {

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
   * @return the folderDescription
   */
  public String getFolderDescription() {
    return folderDescription;
  }

  /**
   * @param folderDescription the folderDescription to set
   */
  public void setFolderDescription(String folderDescription) {
    this.folderDescription = folderDescription;
  }

  @Extension
  public static class DescriptorImpl extends BuildStepDescriptor<Publisher> {

    public DescriptorImpl() {
      super(SynergyAddTaskToFolderPublisher.class);
    }

    @Override
    public String getDisplayName() {
      return "Synergy add task to folder";
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

  /**
   * description of folder for query
   */
  private String folderDescription;

  @DataBoundConstructor
  public SynergyAddTaskToFolderPublisher(String whenToAdd, Boolean simulateOnly, String folderDescription) {
    this.whenToAdd = whenToAdd;
    this.simulateOnly = simulateOnly;
    this.folderDescription = folderDescription;
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
      listener.getLogger().println(buildResult + " is better or equal than " + resultWhenToAdd);

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
        GetFolderIDCommand getFolderIDCommand = new GetFolderIDCommand(synergySCM.getRelease(), getFolderDescription());
        commands.executeSynergyCommand(path, getFolderIDCommand);
        String folderID = getFolderIDCommand.getFolderID();

        //Ermittle zu beruecksichtigende Tasks
        // String l_projectName = Util.replaceMacro(synergySCM.getProject(), EnvVars.getRemote(launcher.getChannel()));
        // TaskCompletedInFolderCommand taskCompletedInFolderCommand
        //         = new TaskCompletedInFolderCommand(l_projectName, folderID);
        // commands.executeSynergyCommand(path, taskCompletedInFolderCommand);
        // List<String> tasks = taskCompletedInFolderCommand.getInformations();
        List<String> tasks = new ArrayList<String>();
        List<TaskOnTopOfBaselineAction> actions = build.getActions(TaskOnTopOfBaselineAction.class);
        for (TaskOnTopOfBaselineAction action : actions) {
          if (action.getParameter().getName().equals(SynergySCM.PARAM_TASK_ON_TOP_OF_BASELINE)) {
            ParameterValue parameter = action.getParameter();
            tasks = new ArrayList(Arrays.asList(StringUtils.split(((StringParameterValue) parameter).value, ";")));
          }
        }

        // alle tasks die schon im Folder drin sind
        List<String> l_folderTasks = new ArrayList<>();
        QueryCommand taskQueryCommand = new QueryCommand("is_task_in_folder_of('" + folderID + "')",
                Arrays.asList(new String[]{"task_number"}));
        commands.executeSynergyCommand(path, taskQueryCommand);
        for (final Map<String, String> t : taskQueryCommand.getQueryResult()) {
          l_folderTasks.add(t.get("task_number"));
        }

        // nur Tasks die noch nicht drin waren müssen noch hinzugefügt werden
        tasks.removeAll(l_folderTasks);

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
      listener.getLogger().println(buildResult + " is worse than " + resultWhenToAdd);
    }
    return true;
  }

  public void setwhenToAdd(String whenToAdd) {
    this.whenToAdd = whenToAdd;
  }

  public String getWhenToAdd() {
    return whenToAdd;
  }

}
