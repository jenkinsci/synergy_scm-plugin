/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package hudson.plugins.synergy;

import hudson.FilePath;
import hudson.Launcher;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.plugins.synergy.SynergyBuildStep.SynergyBuildStepDescriptor;
import hudson.plugins.synergy.impl.Commands;
import hudson.plugins.synergy.impl.GenericCommand;
import hudson.plugins.synergy.impl.SetRoleCommand;
import hudson.plugins.synergy.impl.SynergyException;
import hudson.plugins.synergy.util.SessionUtils;
import java.util.ArrayList;
import java.util.List;
import javax.inject.Inject;
import jenkins.model.Jenkins;
import org.jenkinsci.plugins.workflow.steps.AbstractSynchronousNonBlockingStepExecution;
import org.jenkinsci.plugins.workflow.steps.StepContextParameter;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
/**
 *
 * @author u48jfe
 */
class SynergyBuildStepExecution extends AbstractSynchronousNonBlockingStepExecution<List<List<String>>> {
  
  private static final long serialVersionUID = 1L;

  @StepContextParameter
  private transient TaskListener listener;

  @StepContextParameter
  private transient FilePath ws;

  @StepContextParameter
  private transient Run build;

  @StepContextParameter
  private transient Launcher launcher;

  @Inject
  private transient SynergyBuildStep step;

  @Override
  @SuppressFBWarnings("DE_MIGHT_IGNORE")
  protected List<List<String>> run() throws Exception {

    List<List<String>> l_return = new ArrayList<>();
    Commands commands = null;

    try {

      Jenkins jenkinsInstance = Jenkins.getInstance();
      if (jenkinsInstance != null) {
        //default to global config values if not set in step, but allow step to override all global settings
        SynergySCM.DescriptorImpl globalSynergyDescriptor = jenkinsInstance.getDescriptorByType(SynergySCM.DescriptorImpl.class);
        SynergyBuildStepDescriptor globalSynergyStepDescriptor = jenkinsInstance.getDescriptorByType(SynergyBuildStepDescriptor.class);
        String ccmExe = step.getCcmExe() != null ? step.getCcmExe() : globalSynergyDescriptor.getCcmExe();
        String ccmHome = step.getCcmHome() != null ? step.getCcmHome() : globalSynergyStepDescriptor.getCcmHome();
        String ccmUiLog = step.getCcmUiLog() != null ? step.getCcmUiLog() : globalSynergyDescriptor.getCcmUiLog();
        String ccmEngLog = step.getCcmEngLog() != null ? step.getCcmEngLog() : globalSynergyDescriptor.getCcmEngLog();
        String database = step.getDatabase() != null ? step.getDatabase() : globalSynergyStepDescriptor.getDatabase();
        String username = step.getUsername() != null ? step.getUsername() : globalSynergyStepDescriptor.getUsername();
        String password = step.getPassword() != null ? step.getPassword() : globalSynergyStepDescriptor.getPassword();
        String pathName = step.getPathName() != null ? step.getPathName() : globalSynergyDescriptor.getPathName();
        String engine = step.getEngine() != null ? step.getEngine() : globalSynergyStepDescriptor.getEngine();

        // Start Synergy.
        commands = SessionUtils.openSession(ws, listener, launcher, ccmExe, ccmHome, ccmUiLog, ccmEngLog, false, database, username, password, false, pathName, engine);

        // Become build manager.
        SetRoleCommand setRoleCommand = new SetRoleCommand(SetRoleCommand.BUILD_MANAGER);
        commands.executeSynergyCommand(ws, setRoleCommand);

        // Copy tasks.
        for (String[] l_nextStep : step.getArgs()) {
          GenericCommand genericCommand = new GenericCommand(l_nextStep);
          commands.executeSynergyCommand(ws, genericCommand);
          l_return.add(genericCommand.getresult());
        }
      }
      return l_return;

    } finally {
      // Stop Synergy.
      try {
        if (commands != null) {
          SessionUtils.closeSession(ws, commands, false);
        }
      } catch (SynergyException e) {
        // do nothing
      }
    }

  }

}
