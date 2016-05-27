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
import hudson.plugins.synergy.impl.GetFolderIDCommand;
import hudson.plugins.synergy.impl.SetRoleCommand;
import hudson.plugins.synergy.impl.SynergyException;
import hudson.plugins.synergy.impl.TaskCompletedInFolderCommand;
import hudson.plugins.synergy.util.QueryUtils;
import hudson.plugins.synergy.util.SessionUtils;
import hudson.scm.SCM;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Notifier;
import hudson.tasks.Publisher;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
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

  }

  /**
   * Should the baseline be published
   */
  private boolean onlyOnSuccess;

  /**
   * simulate only
   */
  private boolean simulateOnly;

  /**
   * description of folder for query
   */
  private String folderDescription;

  @DataBoundConstructor
  public SynergyAddTaskToFolderPublisher(Boolean onlyOnSuccess, Boolean simulateOnly, String folderDescription) {
    this.onlyOnSuccess = onlyOnSuccess;
    this.simulateOnly = simulateOnly;
    this.folderDescription = folderDescription;
  }

  public BuildStepMonitor getRequiredMonitorService() {
    return BuildStepMonitor.STEP;
  }

  @Override
  public boolean perform(AbstractBuild<?, ?> build, Launcher launcher,
          BuildListener listener) throws InterruptedException, IOException {
    // Check SCM used.
    SCM scm = build.getProject().getScm();
    if (!(scm instanceof SynergySCM)) {
      listener.getLogger().println("No add task to folder for non Synergy project");
      return false;
    }

    // Check what needs to be done.
    boolean buildSucess = Result.SUCCESS.equals(build.getResult());
    boolean copyFolders = true;

    if (onlyOnSuccess && !buildSucess) {
      // Copy folders if build is sucessful.
      copyFolders = false;
    }

    // Check if we need to go on.
    if (copyFolders) {

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
        String l_projectName = Util.replaceMacro(synergySCM.getProject(), EnvVars.getRemote(launcher.getChannel()));
        TaskCompletedInFolderCommand taskCompletedInFolderCommand
                = new TaskCompletedInFolderCommand(l_projectName, folderID);
        commands.executeSynergyCommand(path, taskCompletedInFolderCommand);
        List<String> tasks = taskCompletedInFolderCommand.getInformations();

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
            SessionUtils.closeSession(path, synergySCM, commands);
          }
        } catch (SynergyException e) {
          return false;
        }
      }
    }
    return true;
  }

  public void setOnlyOnSuccess(boolean onlyOnSuccess) {
    this.onlyOnSuccess = onlyOnSuccess;
  }

  public boolean isOnlyOnSuccess() {
    return onlyOnSuccess;
  }

}
