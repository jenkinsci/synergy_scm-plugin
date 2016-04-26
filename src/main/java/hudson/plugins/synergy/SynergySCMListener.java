/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package hudson.plugins.synergy;

import hudson.Extension;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import hudson.model.Cause;
import hudson.model.Result;
import hudson.model.listeners.SCMListener;
import hudson.scm.ChangeLogSet;
import hudson.scm.SCM;
import hudson.triggers.SCMTrigger;
import hudson.triggers.TimerTrigger.TimerTriggerCause;
import java.util.List;

/**
 *
 * @author u48jfe
 */
@Extension
public class SynergySCMListener extends SCMListener {

    @Override
    public void onChangeLogParsed(AbstractBuild<?, ?> build, BuildListener listener, ChangeLogSet<?> changelog) throws Exception {
        SCM scm = build.getProject().getScm();
        if ((scm instanceof SynergySCM)) {
            SynergySCM synergySCM = (SynergySCM)scm;
            if (!synergySCM.isBuildEmptyChangelog()) {
            boolean l_automaticTriggered = false;
            // pruefe wer den Build ausgeloest hat
            List<Cause> causes = build.getCauses();
            for (Cause cause : causes) {
                if (cause instanceof SCMTrigger.SCMTriggerCause || cause instanceof TimerTriggerCause) {
                    l_automaticTriggered = true;
                    break;
                }
            }
            if (changelog.isEmptySet() && l_automaticTriggered) {
                listener.getLogger().println("No change on project and build started on SCMTrigger or TimerTrigger, nothing to do.");
                build.getExecutor().interrupt(Result.ABORTED);
            }
        }
        }
        super.onChangeLogParsed(build, listener, changelog); //To change body of generated methods, choose Tools | Templates.
    }

}
