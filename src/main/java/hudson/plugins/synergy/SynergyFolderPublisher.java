package hudson.plugins.synergy;

import hudson.FilePath;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import hudson.model.Descriptor;
import hudson.model.Result;
import hudson.plugins.synergy.impl.Commands;
import hudson.plugins.synergy.impl.CopyFolderCommand;
import hudson.plugins.synergy.impl.SynergyException;
import hudson.plugins.synergy.util.SessionUtils;
import hudson.scm.SCM;
import hudson.tasks.Publisher;

import java.io.IOException;

import net.sf.json.JSONObject;

import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.StaplerRequest;

public class SynergyFolderPublisher extends Publisher {
	public static class DescriptorImpl extends Descriptor<Publisher> {
		public static DescriptorImpl DESCRIPTOR = new DescriptorImpl();

		private DescriptorImpl() {
			super(SynergyFolderPublisher.class);
		}

		@Override
		public String getDisplayName() {
			return "Synergy Copy Folder";
		}

		@Override
		public Publisher newInstance(StaplerRequest req, JSONObject formData) throws FormException {
			return new SynergyFolderPublisher(Boolean.parseBoolean(req.getParameter("synergyPublisher.publish")), req
					.getParameter("synergy.intFolder"), req.getParameter("synergy.devFolder"));
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

	public Descriptor<Publisher> getDescriptor() {
		return DescriptorImpl.DESCRIPTOR;
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
			FilePath path = build.getProject().getWorkspace();

			Commands commands = null;
			
			try {
				// Start Synergy.
				commands = SessionUtils.openSession(path, synergySCM, listener, launcher);
			
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