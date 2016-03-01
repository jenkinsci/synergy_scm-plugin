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
import hudson.plugins.synergy.impl.QueryCommand;
import hudson.plugins.synergy.impl.SetRoleCommand;
import hudson.plugins.synergy.impl.SynergyException;
import hudson.plugins.synergy.util.SessionUtils;
import hudson.scm.SCM;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Notifier;
import hudson.tasks.Publisher;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

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
	 * the source folder
	 */
	private String sourceFolder;

	/**
	 * the target folder
	 */
	private String targetFolder;

	@DataBoundConstructor
	public SynergyFolderPublisher(Boolean onlyOnSuccess, String sourceFolder, String targetFolder) {
		this.onlyOnSuccess = onlyOnSuccess;
		this.sourceFolder = sourceFolder;
		this.targetFolder = targetFolder;
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
			String release = synergySCM.getRelease();
			FilePath path = build.getWorkspace();

			Commands commands = null;
			
			try {
				// Start Synergy.
				commands = SessionUtils.openSession(path, synergySCM, listener, launcher);
				
				// Become build manager.
				SetRoleCommand setRoleCommand = new SetRoleCommand(SetRoleCommand.BUILD_MANAGER);
				commands.executeSynergyCommand(path, setRoleCommand);

				// find folder from description
				if(sourceFolder.length() > 0 && !Pattern.compile("(\\w+#)*\\d+$").matcher(sourceFolder).matches()) {
					sourceFolder = sourceFolder.replace("%release", release);
					QueryCommand folderQueryCommand = new QueryCommand("cvtype='folder' and description='" + sourceFolder + "'", Arrays.asList(new String[] {"displayname"}));
					commands.executeSynergyCommand(path, folderQueryCommand);
					if(folderQueryCommand.getQueryResult().size() != 1) {
						listener.getLogger().println("Folder Description '" + sourceFolder + "' ist nicht eindeutig.");
						return false;
					}
					sourceFolder = folderQueryCommand.getQueryResult().get(0).get("displayname");
				}
				
				if(targetFolder.length() > 0 && !Pattern.compile("(\\w+#)*\\d+$").matcher(targetFolder).matches()) {
					targetFolder = targetFolder.replace("%release", release);
					QueryCommand folderQueryCommand = new QueryCommand("cvtype='folder' and description='" + targetFolder + "'", Arrays.asList(new String[] {"displayname"}));
					commands.executeSynergyCommand(path, folderQueryCommand);
					if(folderQueryCommand.getQueryResult().size() != 1) {
						listener.getLogger().println("Folder Description '" + targetFolder + "' ist nicht eindeutig.");
						return false;
					}
					targetFolder = folderQueryCommand.getQueryResult().get(0).get("displayname");
				}
				
				// Copy tasks.
				CopyFolderCommand copyFolderCommand = new CopyFolderCommand(getSourceFolder(), getTargetFolder());
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

	public void setOnlyOnSuccess(boolean onlyOnSuccess) {
		this.onlyOnSuccess = onlyOnSuccess;
	}

	public boolean isOnlyOnSuccess() {
		return onlyOnSuccess;
	}
	
	public void setSourceFolder(String sourceFolder){
		this.sourceFolder = sourceFolder;
	}
	
	public String getSourceFolder(){
		return sourceFolder;
	}
	
	public void setTargetFolder(String targetFolder){
		this.targetFolder = targetFolder;
	}
	
	public String getTargetFolder(){
		return targetFolder;
	}
}