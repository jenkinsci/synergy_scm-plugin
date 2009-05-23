package hudson.plugins.synergy;

import hudson.Plugin;
import hudson.scm.SCMS;
import hudson.tasks.BuildStep;

/**
 * Entry point of the Synergy plugin.
 * 
 * @author Jean-Noel RIBETTE
 */
public class SynergyPluginImpl extends Plugin {
	/**
	 * Starts the Synergy plugin.
	 * 
	 * This method registers the followings Hudson extensions :
	 * <ul>
	 * 	<li>Synergy SCM</li>
	 *  <li>Synergy Maven reporter (used to publish a baseline if the build is sucessful for a Maven project) 
	 *  <li>Synergy publisher (used to publish a baseline if the build is sucessful for a Maven project)
	 * </ul>
	 */
    public void start() throws Exception {
        SCMS.SCMS.add(SynergySCM.DescriptorImpl.DESCRIPTOR); 
        BuildStep.PUBLISHERS.add(SynergyPublisher.DescriptorImpl.DESCRIPTOR);
        BuildStep.PUBLISHERS.add(SynergyFolderPublisher.DescriptorImpl.DESCRIPTOR);
    }
}
