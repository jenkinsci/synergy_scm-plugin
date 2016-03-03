/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package hudson.plugins.synergy;

import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import hudson.model.Descriptor;
import hudson.plugins.synergy.impl.AddTasksToFolderCommand;
import hudson.plugins.synergy.impl.Commands;
import hudson.plugins.synergy.impl.GetFolderIDCommand;
import hudson.plugins.synergy.impl.SetRoleCommand;
import hudson.plugins.synergy.impl.SynergyException;
import hudson.plugins.synergy.impl.TaskCompletedInSourceFolderAndNotInTargetFolderCommand;
import hudson.plugins.synergy.util.QueryUtils;
import hudson.plugins.synergy.util.SessionUtils;
import hudson.scm.SCM;
import hudson.tasks.BuildWrapper;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import org.kohsuke.stapler.DataBoundConstructor;

/**
 *
 * @author u48jfe
 */
public class PreCheckoutBuildStep extends BuildWrapper {

  /**
   * Constructor
   *
   * @param simulateOnly
   * @param sourceFolderDescription
   * @param targetFolderDescription
   */
  @DataBoundConstructor
  public PreCheckoutBuildStep(boolean simulateOnly, String sourceFolderDescription, String targetFolderDescription) {
    this.simulateOnly = simulateOnly;
    this.sourceFolderDescription = sourceFolderDescription;
    this.targetFolderDescription = targetFolderDescription;
  }

  @Override
  public void preCheckout(AbstractBuild build, Launcher launcher, BuildListener listener) throws IOException, InterruptedException {

    SCM scm = build.getProject().getScm();
    if (!(scm instanceof SynergySCM)) {
      listener.getLogger().println("No add task to folder for non Synergy project");
      return;
    }

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

      //Hole Folder-ID der Folders
      GetFolderIDCommand getFolderIDCommand = new GetFolderIDCommand(synergySCM.getRelease(), getSourceFolderDescription());
      commands.executeSynergyCommand(path, getFolderIDCommand);
      String sourceFolderID = getFolderIDCommand.getFolderID();

      getFolderIDCommand = new GetFolderIDCommand(synergySCM.getRelease(), getTargetFolderDescription());
      commands.executeSynergyCommand(path, getFolderIDCommand);
      String targetFolderID = getFolderIDCommand.getFolderID();

      //Ermittle zu beruecksichtigende Tasks
      TaskCompletedInSourceFolderAndNotInTargetFolderCommand taskCompletedInFolderCommand
          = new TaskCompletedInSourceFolderAndNotInTargetFolderCommand(sourceFolderID, targetFolderID);
      commands.executeSynergyCommand(path, taskCompletedInFolderCommand);
      List<String> tasks = taskCompletedInFolderCommand.getInformations();

      // optimize for max querylength
      String maxQueryLength = synergySCM.getMaxQueryLength();
      List<List<String>> optimizedSubLists = QueryUtils.createOptimizedSubLists(new HashSet<String>(tasks), maxQueryLength);
      for (List<String> optimizedSubList : optimizedSubLists) {
        AddTasksToFolderCommand addTaskToFolderCommand = new AddTasksToFolderCommand(optimizedSubList, targetFolderID);
        if (isSimulateOnly()) {
          listener.getLogger().println("Command to execute: " + addTaskToFolderCommand.toString());
        } else {
          commands.executeSynergyCommand(path, addTaskToFolderCommand);
        }
      }

    } catch (SynergyException e) {
      // do nothing
    } finally {
      // Stop Synergy.
      try {
        if (commands != null) {
          SessionUtils.closeSession(path, synergySCM, commands);
        }
      } catch (SynergyException e) {
        // do nothing
      }
    }

  }

  @Override
  public Environment setUp(AbstractBuild build, Launcher launcher, BuildListener listener) throws IOException, InterruptedException {
    return new Environment() {
      // nothing to do 
    };
  }

  /**
   * simulate only
   */
  private boolean simulateOnly;

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
   * description of folder for query
   */
  private String sourceFolderDescription;

  /**
   * @return the folderDescription
   */
  public String getSourceFolderDescription() {
    return sourceFolderDescription;
  }

  /**
   * @param sourceFolderDescription the folderDescription to set
   */
  public void setSourceFolderDescription(String sourceFolderDescription) {
    this.sourceFolderDescription = sourceFolderDescription;
  }

  /**
   * description of folder for target
   */
  private String targetFolderDescription;

  /**
   * @return the destinationFolderDescription
   */
  public String getTargetFolderDescription() {
    return targetFolderDescription;
  }

  /**
   * @param targetFolderDescription the destinationFolderDescription to set
   */
  public void setTargetFolderDescription(String targetFolderDescription) {
    this.targetFolderDescription = targetFolderDescription;
  }

  @Extension
  public static final class DescriptorImpl extends Descriptor<BuildWrapper> {

    /**
     * This human readable name is used in the configuration screen.
     *
     * @return String
     */
    public String getDisplayName() {
      return "Run synergy buildstep before SCM runs";
    }
  }
}
