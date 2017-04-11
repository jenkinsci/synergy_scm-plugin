/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package hudson.plugins.synergy;

import hudson.Extension;
import hudson.scm.SCM;
import jenkins.model.Jenkins;
import org.jenkinsci.plugins.workflow.steps.scm.SCMStep;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;

/**
 *
 * @author u48jfe
 */
public class SynergySCMStep extends SCMStep {

  private String project4Part;
  
  private String database;
  private String username;
  private String password;
  
  private String engine;
  private String ccmHome;
  
  private String release;
  private String updateReleases;
  private String purpose;
  private String oldProject;
  private String baseline;
  private String oldBaseline;
  private String insignificantChangePatterns;
  private String updateFolders;
  

  @DataBoundConstructor
  public SynergySCMStep(String project4Part) {
    this.project4Part = project4Part;
  }
  
  
  @Override
  protected SCM createSCM() {
   return new SynergySCM(project4Part, database, release, updateReleases, purpose, username, password, engine, oldProject, baseline, oldBaseline, ccmHome, false, false, false, false, false, Boolean.TRUE, true, "300", insignificantChangePatterns, updateFolders, true, true);    
  }

  /**
   * @param database the database to set
   */
  @DataBoundSetter
  public void setDatabase(String database) {
    this.database = database;
  }

  /**
   * @param username the username to set
   */
  @DataBoundSetter
  public void setUsername(String username) {
    this.username = username;
  }

  /**
   * @param password the password to set
   */
  @DataBoundSetter
  public void setPassword(String password) {
    this.password = password;
  }

  /**
   * @param engine the engine to set
   */
  @DataBoundSetter
  public void setEngine(String engine) {
    this.engine = engine;
  }

  /**
   * @param ccmHome the ccmHome to set
   */
  @DataBoundSetter
  public void setCcmHome(String ccmHome) {
    this.ccmHome = ccmHome;
  }

  /**
   * @param release the release to set
   */
  @DataBoundSetter
  public void setRelease(String release) {
    this.release = release;
  }

  /**
   * @param updateReleases the updateReleases to set
   */
  @DataBoundSetter
  public void setUpdateReleases(String updateReleases) {
    this.updateReleases = updateReleases;
  }

  /**
   * @param purpose the purpose to set
   */
  @DataBoundSetter
  public void setPurpose(String purpose) {
    this.purpose = purpose;
  }

  /**
   * @param oldProject the oldProject to set
   */
  @DataBoundSetter
  public void setOldProject(String oldProject) {
    this.oldProject = oldProject;
  }

  /**
   * @param baseline the baseline to set
   */
  @DataBoundSetter
  public void setBaseline(String baseline) {
    this.baseline = baseline;
  }

  /**
   * @param oldBaseline the oldBaseline to set
   */
  @DataBoundSetter
  public void setOldBaseline(String oldBaseline) {
    this.oldBaseline = oldBaseline;
  }

  /**
   * @param insignificantChangePatterns the insignificantChangePatterns to set
   */
  @DataBoundSetter
  public void setInsignificantChangePatterns(String insignificantChangePatterns) {
    this.insignificantChangePatterns = insignificantChangePatterns;
  }

  /**
   * @param updateFolders the updateFolders to set
   */
  @DataBoundSetter
  public void setUpdateFolders(String updateFolders) {
    this.updateFolders = updateFolders;
  }
 
  
  
  @Extension
    public static final class SynergySCMStepDescriptorImpl extends SCMStepDescriptor {

        

        @Override
        public String getFunctionName() {
            return "synergy";
        }

        @Override
        public String getDisplayName() {
            return "synergy";
        }

    }
  
}
