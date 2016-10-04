/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package hudson.plugins.synergy;

import hudson.EnvVars;
import hudson.Extension;
import hudson.model.EnvironmentContributor;
import hudson.model.InvisibleAction;
import hudson.model.Run;
import hudson.model.StringParameterValue;
import hudson.model.TaskListener;
import java.io.IOException;
import javax.annotation.Nonnull;
import org.kohsuke.accmod.Restricted;
import org.kohsuke.accmod.restrictions.NoExternalUse;

/**
 *
 * @author u48jfe
 */
@Restricted(NoExternalUse.class)
public class TaskOnTopOfBaselineAction extends InvisibleAction {
  
  StringParameterValue value;
  
  public TaskOnTopOfBaselineAction(StringParameterValue value) {
    this.value = value;
  }
  
  public StringParameterValue getParameter() {
    return value;
  }
  
  @Extension
  public static final class TaskOnTopOfBaselineActionEnvironmentContributor extends EnvironmentContributor {
    
    @Override
    public void buildEnvironmentFor(@Nonnull Run r, @Nonnull EnvVars envs, @Nonnull TaskListener listener) throws IOException, InterruptedException {
      TaskOnTopOfBaselineAction action = r.getAction(TaskOnTopOfBaselineAction.class);
      if (action != null && action.getParameter() != null) {
        envs.put(action.getParameter().getName(), String.valueOf(action.getParameter().getValue()));
      }
    }
  }
  
}
