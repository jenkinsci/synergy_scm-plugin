/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package hudson.plugins.synergy;

import hudson.FilePath;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.Action;
import hudson.model.BuildListener;
import hudson.model.Describable;
import hudson.model.Descriptor;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.plugins.synergy.impl.Commands;
import hudson.plugins.synergy.impl.CopyFolderCommand;
import hudson.plugins.synergy.impl.SetRoleCommand;
import hudson.plugins.synergy.impl.SynergyException;
import hudson.plugins.synergy.util.SessionUtils;
import hudson.scm.SCM;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.BuildStepMonitor;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import jenkins.tasks.SimpleBuildStep;
import org.kohsuke.stapler.DataBoundConstructor;

/**
 *
 * @author u48jfe
 */
public class CopyFolderBuildStep implements SimpleBuildStep, Describable<CopyFolderBuildStep> {

  /**
   * the source folder
   */
  private String sourceFolder;

  /**
   * the target folder
   */
  private String targetFolder;

  /**
   * Konstruktor
   *
   * @param sourceFolder source folder
   * @param targetFolder target folder
   */
  @DataBoundConstructor
  public CopyFolderBuildStep(String sourceFolder, String targetFolder) {
    this.sourceFolder = sourceFolder;
    this.targetFolder = targetFolder;
  }

  @Override
  public boolean prebuild(AbstractBuild<?, ?> build, BuildListener listener) {
    return true;
  }

  @Override
  public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener) throws InterruptedException, IOException {
    SCM scm = build.getParent().getScm();
    if (!(scm instanceof SynergySCM)) {
      listener.getLogger().println("No add task to folder for non Synergy project");
      return false;
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

      // Copy tasks.
      CopyFolderCommand copyFolderCommand = new CopyFolderCommand(getSourceFolder(), getTargetFolder());
      commands.executeSynergyCommand(path, copyFolderCommand);

    } catch (SynergyException e) {
      return false;
    } finally {
      // Stop Synergy.
      try {
        if (commands != null) {
          SessionUtils.closeSession(path, commands, synergySCM.isLeaveSessionOpen());
        }
      } catch (SynergyException e) {
        // do nothing
      }
    }
    return true;
  }

  @Override
  @Deprecated
  public Action getProjectAction(AbstractProject<?, ?> project) {
    return null;
  }

  @Override
  public Collection<? extends Action> getProjectActions(AbstractProject<?, ?> project) {
    return Collections.emptyList();
  }

  @Override
  public BuildStepMonitor getRequiredMonitorService() {
    return BuildStepMonitor.NONE;
  }

  @Override
  public Descriptor<CopyFolderBuildStep> getDescriptor() {
    return new DescriptorImpl();
  }

  /**
   * @return the sourceFolder
   */
  public String getSourceFolder() {
    return sourceFolder;
  }

  /**
   * @param sourceFolder the sourceFolder to set
   */
  public void setSourceFolder(String sourceFolder) {
    this.sourceFolder = sourceFolder;
  }

  /**
   * @return the targetFolder
   */
  public String getTargetFolder() {
    return targetFolder;
  }

  /**
   * @param targetFolder the targetFolder to set
   */
  public void setTargetFolder(String targetFolder) {
    this.targetFolder = targetFolder;
  }

  @Override
  public void perform(Run<?, ?> run, FilePath path, Launcher launcher, TaskListener listener) throws InterruptedException, IOException {
    
    if (run instanceof AbstractBuild) {
      AbstractBuild<?, ?> build = (AbstractBuild<?, ?>) run;
    SCM scm = build.getParent().getScm();
    if (!(scm instanceof SynergySCM)) {
      listener.getLogger().println("No add task to folder for non Synergy project");
    }

    // Get Synergy parameters.
    SynergySCM synergySCM = (SynergySCM) scm;

    Commands commands = null;

    try {
      // Start Synergy.
      commands = SessionUtils.openSession(path, synergySCM, listener, launcher);

      // Become build manager.
      SetRoleCommand setRoleCommand = new SetRoleCommand(SetRoleCommand.BUILD_MANAGER);
      commands.executeSynergyCommand(path, setRoleCommand);

      // Copy tasks.
      CopyFolderCommand copyFolderCommand = new CopyFolderCommand(getSourceFolder(), getTargetFolder());
      commands.executeSynergyCommand(path, copyFolderCommand);

    } catch (SynergyException e) {
      
    } finally {
      // Stop Synergy.
      try {
        if (commands != null) {
          SessionUtils.closeSession(path, commands, synergySCM.isLeaveSessionOpen());
        }
      } catch (SynergyException e) {
        // do nothing
      }
    }
    }
    
  }

  
  public static final class DescriptorImpl extends BuildStepDescriptor<CopyFolderBuildStep> {

    @Override
    public boolean isApplicable(Class<? extends AbstractProject> jobType) {
      return true;
    }

    @Override
    public String getDisplayName() {
      return "Synergy Copy Folder BuildStep";
    }

  }
}
