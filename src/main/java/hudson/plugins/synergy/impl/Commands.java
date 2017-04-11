package hudson.plugins.synergy.impl;

import hudson.FilePath;
import hudson.Launcher;
import hudson.model.TaskListener;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang.StringUtils;

public class Commands implements Serializable {

  private static final long serialVersionUID = 1L;
  
  /**
   * Path to ccm executable.
   */
  private String ccmExe;

  /**
   * Path to UI Log.
   */
  private String ccmUiLog;

  /**
   * Path to Engine log.
   */
  private String ccmEngLog;

  /**
   * Path to CCM_HOME.
   */
  private String ccmHome;

  /**
   * Launcher.
   */
  private transient Launcher launcher;

  /**
   * Address.
   */
  private String ccmAddr;

  /**
   * Build listener.
   */
  private TaskListener buildListener;

  /**
   * get for TaskListener
   * @return TaskListener
   */
  public TaskListener getTaskListener() {
    return buildListener;
  }

  /**
   * set for TaskListener
   * @param buildListener TaskListener
   */
  public void setTaskListener(TaskListener buildListener) {
    this.buildListener = buildListener;
  }

  
  /**
   * CCM adress
   * @return String
   */
  public String getCcmAddr() {
    return ccmAddr;
  }

  public void setCcmAddr(String ccmAddr) {
    this.ccmAddr = ccmAddr;
  }

  public Launcher getLauncher() {
    return launcher;
  }

  public void setLauncher(Launcher launcher) {
    this.launcher = launcher;
  }

  public String getCcmEngLog() {
    return ccmEngLog;
  }

  public void setCcmEngLog(String ccmEngLog) {
    this.ccmEngLog = ccmEngLog;
  }

  public String getCcmUiLog() {
    return ccmUiLog;
  }

  public void setCcmUiLog(String ccmUiLog) {
    this.ccmUiLog = ccmUiLog;
  }

  public String getCcmExe() {
    return ccmExe;
  }

  public void setCcmExe(String ccmExe) {
    this.ccmExe = ccmExe;
  }

  public String getHome() {
    return ccmHome;
  }

  public void setCcmHome(String ccmHome) {
    this.ccmHome = ccmHome;
  }

  /**
   * Builds a compare project command.
   * @param newProject new project
   * @param oldProject old project
   * @return String[] String[]
   */
  public String[] buildCompareProjectCommand(String newProject, String oldProject) {
    String[] query = new String[]{
      getCcmExe(), "query", "\"type!='project' and type!='dir' and is_member_of('" + newProject + "') and not is_member_of('" + oldProject + "')\"",
      "-u",
      "-f",
      "%%objectname"
    };
    return query;
  }

  /**
   * Executes a Synergy command. Logs environment if command fails.
   *
   * @param path	Current directory
   * @param command	Command and arguments
   * @throws IOException IOException
   * @throws InterruptedException InterruptedException
   * @throws SynergyException SynergyException
   */
  public void executeSynergyCommand(FilePath path, Command command) throws IOException, InterruptedException, SynergyException {
      executeSynergyCommand(path, command, true);
  }
  
  /**
   * Executes a Synergy command.
   *
   * @param path	Current directory
   * @param command	Command and arguments
   * @param p_logFailureEnvironment true means logging environment in case of failure
   * @throws IOException IOException
   * @throws InterruptedException InterruptedException
   * @throws SynergyException SynergyException
   */
  public void executeSynergyCommand(FilePath path, Command command, boolean p_logFailureEnvironment) throws IOException, InterruptedException, SynergyException {
    Map<String, String> system = System.getenv();
    List<String> param = new ArrayList<String>();
    if (!launcher.isUnix() || ccmHome == null || ccmHome.length() == 0) {
      for (Map.Entry<String, String> entry : system.entrySet()) {
        String s = entry.getKey() + "=" + entry.getValue();
        param.add(s);
      }
    }
    if (ccmAddr != null) {
      param.add("CCM_ADDR=" + ccmAddr);
    }
    if (ccmUiLog != null) {
      param.add("CCM_UILOG=" + ccmUiLog);
    }
    if (ccmEngLog != null) {
      param.add("CCM_ENGLOG=" + ccmEngLog);
    }

    if (launcher.isUnix() && ccmHome != null && ccmHome.length() != 0) {
      param.add("CCM_HOME=" + ccmHome);
      param.add("PATH=$CCM_HOME/bin:$PATH");
    }

    String[] env = param.toArray(new String[param.size()]);

    ByteArrayOutputStream out = new ByteArrayOutputStream();
    String[] commands = command.buildCommand(ccmExe);
    boolean[] mask = command.buildMask();
    if (launcher.isUnix()) {
      // Print Synergy command otherwise does not get printed
      // by launcher
      printCommandLine(commands, null, mask); 
    }
    int result = launcher.launch().cmds(commands).masks(mask).envs(env).stdout(out).pwd(path).join();
    String output = out.toString("ISO-8859-1");

    if (!command.isStatusOK(result, output)) {
      buildListener.getLogger().println("ccm command failed");
      buildListener.getLogger().println(output);
      if (p_logFailureEnvironment) {
        buildListener.getLogger().println("Command: The environment was :");
        for (String s : param) {
            buildListener.getLogger().println(s);
        }
      }
      throw new SynergyException(result);
    } else if (command instanceof QueryCommand) {
      buildListener.getLogger().println("...");
    } else {
      buildListener.getLogger().println(output);
    }

    output = StringUtils.stripToNull(output);
    if (output != null) {
      // TODO better way to handle this : distinguish mono an multi line result 
      // and use a BufferedReader to read the lines.  
      int oldLength = output.length();
      do {
        oldLength = output.length();
        output = StringUtils.chomp(output);
      } while (output.length() < oldLength);
      if (output.endsWith("\r\n")) {
        // DOS endline.
        output = output.substring(0, output.length() - 2);
      } else if (output.endsWith("\n")) {
        // UNIX endline.
        output = output.substring(0, output.length() - 1);
      }
    }
    output = StringUtils.stripToNull(output);
    command.parseResult(output);
  }

  /**
   * Executes a Synergy command. Logs environment if command fails.
   *
   * @param path	Current directory
   * @param command	Command and arguments
   * @throws IOException IOException
   * @throws InterruptedException InterruptedException
   * @throws SynergyException SynergyException
   */
  public void executeSynergyCommand(FilePath path, StreamCommand command) throws IOException, InterruptedException, SynergyException {
      executeSynergyCommand(path, command, true);
  }
  
  /**
   * Executes a Synergy command.
   *
   * @param path	Current directory
   * @param command	Command and arguments
   * @param p_logFailureEnvironment true means logging environment in case of failure
   * @throws IOException IOException
   * @throws InterruptedException InterruptedException
   * @throws SynergyException SynergyException
   */
  public void executeSynergyCommand(FilePath path, StreamCommand command, boolean p_logFailureEnvironment) throws IOException, InterruptedException, SynergyException {
    Map<String, String> system = System.getenv();
    List<String> param = new ArrayList<String>();
    if (!launcher.isUnix() || ccmHome == null || ccmHome.length() != 0) {
      for (Map.Entry<String, String> entry : system.entrySet()) {
        String s = entry.getKey() + "=" + entry.getValue();
        param.add(s);
      }
    }
    if (ccmAddr != null) {
      param.add("CCM_ADDR=" + ccmAddr);
    }
    if (ccmUiLog != null) {
      param.add("CCM_UILOG=" + ccmUiLog);
    }
    if (ccmEngLog != null) {
      param.add("CCM_ENGLOG=" + ccmEngLog);
    }

    if (launcher.isUnix() && ccmHome != null && ccmHome.length() != 0) {
      param.add("CCM_HOME=" + ccmHome);
      param.add("PATH=$CCM_HOME/bin:$PATH");
    }
    String[] env = param.toArray(new String[param.size()]);

    OutputStream out = command.buildResultOutputer();
    int result;
    try {
      String[] commands = command.buildCommand(ccmExe);
      boolean[] mask = command.buildMask();
      if (launcher.isUnix()) {
        // Print Synergy command otherwise does not get printed
        // by launcher
        printCommandLine(commands, null, mask);
      }
      result = launcher.launch().cmds(commands).masks(mask).envs(env).stdout(out).pwd(path).join();
    } finally {
      out.close();
    }

    if (result != 0 && result != 1) {
      buildListener.getLogger().println("ccm command failed");
      if (p_logFailureEnvironment) {
        buildListener.getLogger().println("StreamCommand : The environment was :");
        for (String s : param) {
            buildListener.getLogger().println(s);
        }
      }
      throw new SynergyException(result);
    }
  }

  /**
   * Prints out the command line to the listener so that users know what we are doing.
   * @param cmd cmd
   * @param workDir dir
   * @param mask mask
   */
  protected final void printCommandLine(String[] cmd, FilePath workDir, boolean[] mask) {
    StringBuilder buf = new StringBuilder();
    int arg_index = 0;

    if (workDir != null) {
      buf.append('[');
      buf.append(workDir.getRemote().replaceFirst("^.+[/\\\\]", ""));
      buf.append("] ");
    }
    buf.append('$');
    for (String c : cmd) {
      String c_masked;
      buf.append(' ');
      /* determine if argument should be masked 
			 * TODO: consider displaying the password when debug mode is enabled
       */
      if (mask[arg_index] == true) {
        c_masked = "******";
      } else {
        c_masked = c;
      }

      if (c.indexOf(' ') >= 0) {
        if (c.indexOf('"') >= 0) {
          buf.append('\'').append(c_masked).append('\'');
        } else {
          buf.append('"').append(c_masked).append('"');
        }
      } else {
        buf.append(c_masked);
      }
      arg_index++;
    }
    buildListener.getLogger().println(buf.toString());
  }

}
