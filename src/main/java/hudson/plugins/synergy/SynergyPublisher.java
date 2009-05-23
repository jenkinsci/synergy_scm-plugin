package hudson.plugins.synergy;

import hudson.FilePath;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import hudson.model.Descriptor;
import hudson.model.Result;
import hudson.plugins.synergy.impl.Commands;
import hudson.plugins.synergy.impl.CreateProjectBaselineCommand;
import hudson.plugins.synergy.impl.PublishBaselineCommand;
import hudson.plugins.synergy.impl.SynergyException;
import hudson.plugins.synergy.util.SessionUtils;
import hudson.scm.SCM;
import hudson.tasks.Publisher;

import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import net.sf.json.JSONObject;

import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.StaplerRequest;

public class SynergyPublisher extends Publisher {
	public static class DescriptorImpl extends Descriptor<Publisher> {
		public static DescriptorImpl DESCRIPTOR = new DescriptorImpl();

		private DescriptorImpl() {
			super(SynergyPublisher.class);
		}

		@Override
		public String getDisplayName() {
			return "Create a Synergy Baseline";
		}

		@Override
		public Publisher newInstance(StaplerRequest req, JSONObject formData) throws FormException {
			return new SynergyPublisher(
				req.getParameter("synergyPublisher.time"),
				Boolean.parseBoolean(req.getParameter("synergyPublisher.publish"))
			);
		}

	}

	/**
	 * The moment the baseline should be created (after/before/sucess)
	 */
	private String time;

	/**
	 * Should the baseline be published
	 */
	private boolean publish;
	
	@DataBoundConstructor
	public SynergyPublisher(String time, boolean publish) {
		this.time = time;
		this.publish = publish;
	}

	public Descriptor<Publisher> getDescriptor() {
		return DescriptorImpl.DESCRIPTOR;
	}

	@Override
	public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener) throws InterruptedException, IOException {
		// Check SCM used.
		SCM scm = build.getProject().getScm();
		if (!(scm instanceof SynergySCM)) {
			listener.getLogger().println("No baseline publishing for non Synergy project");
			return false;
		}

		// Check what needs to be done.
		boolean createBaseline = false;
		boolean publishBaseline = false;
		boolean buildSucess = Result.SUCCESS.equals(build.getResult());

		String createBaselineAtPoint = getTime();

		if ("after".equals(createBaselineAtPoint)) {
			// Always create baseline after build.
			createBaseline = true;
		} else if ("success".equals(createBaselineAtPoint) && buildSucess) {
			// Create baseline only after sucessful build.
			createBaseline = true;
		}
		if (publish && buildSucess) {
			// Publish baseline if build is sucessful.
			publishBaseline = true;
		}

		// Check if we need to go on.
		if (createBaseline || publishBaseline) {
			// Get Synergy parameters.
			SynergySCM synergySCM = (SynergySCM) scm;
			String project = synergySCM.getProject();
			String purpose = synergySCM.getPurpose();
			String release = synergySCM.getRelease();

			FilePath path = build.getProject().getWorkspace();

			Commands commands = null;			

			try {
				// Open Session.
				commands = SessionUtils.openSession(path, synergySCM, listener, launcher);
				
				Date date = build.getTimestamp().getTime();
				DateFormat format = new SimpleDateFormat("yyyyMMdd-hhmm");
				String name = build.getProject().getName() + "-" + format.format(date);

				// Create baseline.
				if (createBaseline) {
					CreateProjectBaselineCommand createCommand = new CreateProjectBaselineCommand(name, project, release, purpose);
					commands.executeSynergyCommand(path, createCommand);
				}

				// Publish baseline.
				if (publishBaseline) {
					PublishBaselineCommand publishCommand = new PublishBaselineCommand(name);
					commands.executeSynergyCommand(path, publishCommand);
				}

			} catch (SynergyException e) {
				return false;
			} finally {
				// Stop Synergy.
				try {
					SessionUtils.closeSession(path, synergySCM, commands);
				} catch (SynergyException e) {
					return false;
				}
			}
		}
		return true;
	}

	public void setPublish(boolean publish) {
		this.publish = publish;
	}

	public boolean isPublish() {
		return publish;
	}

	public String getTime() {
		return time;
	}

	public void setTime(String time) {
		this.time = time;
	}
}