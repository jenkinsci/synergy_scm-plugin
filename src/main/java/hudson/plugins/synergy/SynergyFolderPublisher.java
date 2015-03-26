package hudson.plugins.synergy;

import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.BuildListener;
import hudson.model.Result;
import hudson.plugins.synergy.impl.Commands;
import hudson.plugins.synergy.impl.CopyFolderCommand;
import hudson.plugins.synergy.impl.SetRoleCommand;
import hudson.plugins.synergy.impl.SynergyException;
import hudson.plugins.synergy.util.SessionUtils;
import hudson.scm.SCM;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Notifier;
import hudson.tasks.Publisher;

import java.io.IOException;

import net.sf.json.JSONObject;

import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.StaplerRequest;

public class SynergyFolderPublisher extends Notifier {
	@Extension
	public static class DescriptorImpl extends BuildStepDescriptor<Publisher> {
		public DescriptorImpl() {
			super(SynergyFolderPublisher.class);
		}

		@Override
		public String getDisplayName() {
			return "Synergy Copy Folder";
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
	 * the integration folder
	 */
	private String intFolder;

	/**
	 * the development folder
	 */
	private String devFolder;

	@DataBoundConstructor
	public SynergyFolderPublisher(Boolean onlyOnSuccess, String intFolder, String devFolder) {
		this.onlyOnSuccess = onlyOnSuccess;
		this.intFolder = intFolder;
		this.devFolder = devFolder;
	}

	public BuildStepMonitor getRequiredMonitorService() {
		return BuildStepMonitor.STEP;
	}

	@Override
	public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener) throws InterruptedException, IOException {
		// Check SCM used.
		SCM scm = build.getProject().getScm();
		if (!(scm instanceof SynergySCM)) {
			listener.getLogger().println("No Folder copy for non Synergy project");
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
			
				// Copy tasks.
				CopyFolderCommand copyFolderCommand = new CopyFolderCommand(getIntFolder(), getDevFolder());
				commands.executeSynergyCommand(path, copyFolderCommand);
			} catch (SynergyException e) {
				return false;
			} finally {
				// Stop Synergy.				
				try {
					if (commands!=null) {
						SessionUtils.closeSession(path, synergySCM, commands);
					}
				} catch (SynergyException e) {
					return false;
				}
			}
		}
		return true;
	}

	public void setPublish(boolean publish) {
		this.onlyOnSuccess = publish;
	}

	public boolean isPublish() {
		return onlyOnSuccess;
	}
	
	public void setIntFolder(String intFolder){
		this.intFolder = intFolder;
	}
	
	public String getIntFolder(){
		return intFolder;
	}
	
	public void setDevFolder(String devFolder){
		this.devFolder = devFolder;
	}
	
	public String getDevFolder(){
		return devFolder;
	}
}