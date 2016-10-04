/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package hudson.plugins.synergy;

import hudson.Extension;
import java.util.List;
import javax.annotation.Nonnull;
import net.sf.json.JSONObject;
import org.jenkinsci.plugins.workflow.steps.AbstractStepDescriptorImpl;
import org.jenkinsci.plugins.workflow.steps.AbstractStepImpl;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.StaplerRequest;

/**
 *
 * @author u48jfe
 */
public class SynergyBuildStep extends AbstractStepImpl {

  private String database;
  private String username;
  private String password;
  private String pathName;
  private String engine;
  private String ccmHome;
  private String ccmExe;
  private String ccmUiLog;
  private String ccmEngLog;

  @Nonnull
  private List<String[]> args;

  public List<String[]> getArgs() {
    return args;
  }

  /**
   * Konstruktor
   *
   * @param args for command lien
   *
   */
  @DataBoundConstructor
  public SynergyBuildStep(List<String[]> args) {
    this.args = args;
  }

  /**
   * @return the database
   */
  public String getDatabase() {
    return database;
  }

  /**
   * @return the username
   */
  public String getUsername() {
    return username;
  }

  /**
   * @return the password
   */
  public String getPassword() {
    return password;
  }

  /**
   * @return the pathName
   */
  public String getPathName() {
    return pathName;
  }

  /**
   * @return the engine
   */
  public String getEngine() {
    return engine;
  }

  /**
   * @return the ccmHome
   */
  public String getCcmHome() {
    return ccmHome;
  }

  /**
   * @return the ccmExe
   */
  public String getCcmExe() {
    return ccmExe;
  }

  /**
   * @return the ccmUiLog
   */
  public String getCcmUiLog() {
    return ccmUiLog;
  }

  /**
   * @return the ccmEngLog
   */
  public String getCcmEngLog() {
    return ccmEngLog;
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
   * @param pathName the pathName to set
   */
  @DataBoundSetter
  public void setPathName(String pathName) {
    this.pathName = pathName;
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
   * @param ccmExe the ccmExe to set
   */
  @DataBoundSetter
  public void setCcmExe(String ccmExe) {
    this.ccmExe = ccmExe;
  }

  /**
   * @param ccmUiLog the ccmUiLog to set
   */
  @DataBoundSetter
  public void setCcmUiLog(String ccmUiLog) {
    this.ccmUiLog = ccmUiLog;
  }

  /**
   * @param ccmEngLog the ccmEngLog to set
   */
  @DataBoundSetter
  public void setCcmEngLog(String ccmEngLog) {
    this.ccmEngLog = ccmEngLog;
  }

  @Extension
  public static class SynergyBuildStepDescriptor extends AbstractStepDescriptorImpl {

    private String ccmHome;
    private String database;
    private String engine;
    private String username;
    private String password;

    public SynergyBuildStepDescriptor() {
      super(SynergyBuildStepExecution.class);
      load();
    }

    @Override
    public boolean configure(StaplerRequest req, JSONObject formData) throws FormException {
      req.bindJSON(this, formData);
      save();
      return true;
    }

    @Override
    public String getFunctionName() {
      return "synergyStep";
    }

    @Nonnull
    @Override
    public String getDisplayName() {
      return "generic synergy step";
    }

    /**
     * @return the ccmHome
     */
    public String getCcmHome() {
      return ccmHome;
    }

    /**
     * @return the database
     */
    public String getDatabase() {
      return database;
    }

    /**
     * @return the engine
     */
    public String getEngine() {
      return engine;
    }

    /**
     * @return the username
     */
    public String getUsername() {
      return username;
    }

    /**
     * @return the password
     */
    public String getPassword() {
      return password;
    }

    /**
     * @param ccmHome the ccmHome to set
     */
    public void setCcmHome(String ccmHome) {
      this.ccmHome = ccmHome;
    }

    /**
     * @param database the database to set
     */
    public void setDatabase(String database) {
      this.database = database;
    }

    /**
     * @param engine the engine to set
     */
    public void setEngine(String engine) {
      this.engine = engine;
    }

    /**
     * @param username the username to set
     */
    public void setUsername(String username) {
      this.username = username;
    }

    /**
     * @param password the password to set
     */
    public void setPassword(String password) {
      this.password = password;
    }
  }
}
