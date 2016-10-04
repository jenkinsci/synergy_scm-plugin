package hudson.plugins.synergy.util;

import hudson.FilePath;
import hudson.Launcher;
import hudson.model.TaskListener;
import hudson.plugins.synergy.SynergySCM;
import hudson.plugins.synergy.impl.CheckSessionCommand;
import hudson.plugins.synergy.impl.Commands;
import hudson.plugins.synergy.impl.GetDelimiterCommand;
import hudson.plugins.synergy.impl.StartCommand;
import hudson.plugins.synergy.impl.StopCommand;
import hudson.plugins.synergy.impl.SynergyException;
import hudson.util.Secret;
import java.io.File;
import java.io.IOException;
import java.util.StringTokenizer;

/**
 * Utility class to open and close a session.
 */
public class SessionUtils {

  /**
   * Create and configure a Synergy command launcher.
   *
   * If ccmHome is not defined in a unix environment, the value of the CCM_HOME environment variable is used instead.
   *
   * @param synergySCM	The SCM configuration.
   * @param listener	The build listener
   * @param launcher	The command launcher
   * @return	A Synergy command launcher
   */
  private static Commands configureCommands(String ccmExe, String ccmHome, String ccmUiLog, String ccmEngLog, TaskListener listener, Launcher launcher) {
    Commands commands = new Commands();
    if (launcher.isUnix() && ccmHome != null && ccmHome.length() != 0) {
      if (((ccmHome.length() <= 0)) && ccmExe.startsWith("/")) {
        commands.setCcmHome(System.getenv("CCM_HOME"));
        commands.setCcmExe(ccmExe);
      } else if ((ccmHome.length() <= 0)) {
        StringTokenizer tokenizer = new StringTokenizer(System.getenv("PATH"), ":");
        while (tokenizer.hasMoreTokens()) {
          String path = tokenizer.nextToken();
          if (new File(path, ccmExe).canExecute()) {
            ccmHome = System.getenv("CCM_HOME");
            ccmExe = path + "/" + ccmExe;
            break;
          }
        }
        commands.setCcmHome(ccmHome);
        commands.setCcmExe(ccmExe);
      } else {
        commands.setCcmHome(ccmHome);
        commands.setCcmExe(ccmHome + "/bin/" + ccmExe);
      }
    } else {
      commands.setCcmExe(ccmExe);
    }

    commands.setCcmUiLog(ccmUiLog);
    commands.setCcmEngLog(ccmEngLog);

    commands.setTaskListener(listener);
    commands.setLauncher(launcher);

    return commands;
  }

  /**
   * Open or resuse a Synergy session.
   *
   * @param path	The workarea path
   * @param synergySCM	The Synergy SCM configuration
   * @param listener	The build listener
   * @param launcher	The command launcher
   * @return	A Synergy command launcher
   * @throws IOException IOException
   * @throws InterruptedException InterruptedException
   * @throws SynergyException SynergyException
   */
  public static Commands openSession(FilePath path, SynergySCM synergySCM, TaskListener listener, Launcher launcher) throws IOException, InterruptedException, SynergyException {
    Commands commands = configureCommands(synergySCM.getDescriptor().getCcmExe(), synergySCM.getCcmHome(), synergySCM.getDescriptor().getCcmUiLog(), synergySCM.getDescriptor().getCcmEngLog(), listener, launcher);
    String ccmAddr = CheckSessionCommand.SESSION_NOT_FOUND;
    FilePath ccmSessionMapFile = new FilePath(path, SynergySCM.CCM_SESSION_MAP_FILE_NAME);
    if (synergySCM.isLeaveSessionOpen()) {
      CheckSessionCommand checkSessionCommand = new CheckSessionCommand();
      commands.executeSynergyCommand(path, checkSessionCommand);
      ccmAddr = checkSessionCommand.getCcmAddr(ccmSessionMapFile);
    }
    if (CheckSessionCommand.SESSION_NOT_FOUND.equals(ccmAddr)) {
      ccmAddr = startSession(path,  commands, ccmSessionMapFile, synergySCM.getDatabase(), synergySCM.getUsername(), Secret.toString(synergySCM.getPassword()), synergySCM.isRemoteClient(), synergySCM.getDescriptor().getPathName(), synergySCM.getEngine());
    }
    commands.setCcmAddr(ccmAddr);

    // check if Session is alive
    GetDelimiterCommand l_command = new GetDelimiterCommand();
    try {
      commands.executeSynergyCommand(path, l_command);
    } catch (SynergyException l_synEx) {
      ccmAddr = startSession(path,  commands, ccmSessionMapFile, synergySCM.getDatabase(), synergySCM.getUsername(), Secret.toString(synergySCM.getPassword()), synergySCM.isRemoteClient(), synergySCM.getDescriptor().getPathName(), synergySCM.getEngine());
      commands.setCcmAddr(ccmAddr);
    }

    return commands;
  }

  /**
   * Open or resuse a Synergy session.
   *
   * @param path	The workarea path
   * @param listener	The build listener
   * @param launcher	The command launcher
   * @param ccmExe ccmExe
   * @param ccmHome ccmHome
   * @param ccmUiLog ccmUiLog
   * @param ccmEngLog ccmEngLog
   * @param leaveSessionOpen leaveSessionOpen
   * @param database database
   * @param username username
   * @param password password
   * @param remoteClient remoteClient
   * @param pathName pathName
   * @param engine engine
   * 
   * @return	A Synergy command launcher
   * @throws IOException IOException
   * @throws InterruptedException InterruptedException
   * @throws SynergyException SynergyException
   */
  public static Commands openSession(FilePath path, TaskListener listener, Launcher launcher, String ccmExe, String ccmHome, String ccmUiLog, String ccmEngLog, boolean leaveSessionOpen, String database,String  username,String  password,boolean  remoteClient,String  pathName, String engine) throws IOException, InterruptedException, SynergyException {
    Commands commands = configureCommands(ccmExe, ccmHome, ccmUiLog, ccmEngLog, listener, launcher);
    String ccmAddr = CheckSessionCommand.SESSION_NOT_FOUND;
    FilePath ccmSessionMapFile = new FilePath(path, SynergySCM.CCM_SESSION_MAP_FILE_NAME);
    if (leaveSessionOpen) {
      CheckSessionCommand checkSessionCommand = new CheckSessionCommand();
      commands.executeSynergyCommand(path, checkSessionCommand);
      ccmAddr = checkSessionCommand.getCcmAddr(ccmSessionMapFile);
    }
    if (CheckSessionCommand.SESSION_NOT_FOUND.equals(ccmAddr)) {
      ccmAddr = startSession(path, commands, ccmSessionMapFile, database, username, password, remoteClient, pathName, engine);
    }
    commands.setCcmAddr(ccmAddr);

    // check if Session is alive
    GetDelimiterCommand l_command = new GetDelimiterCommand();
    try {
      commands.executeSynergyCommand(path, l_command);
    } catch (SynergyException l_synEx) {
      ccmAddr = startSession(path, commands, ccmSessionMapFile, database, username, password, remoteClient, pathName, engine);
      commands.setCcmAddr(ccmAddr);
    }

    return commands;
  }
  
  private static String startSession(FilePath path, Commands commands, FilePath ccmSessionMapFile, String database, String username, String password, boolean remoteClient, String pathName, String engine) throws IOException, InterruptedException, SynergyException {
    String ccmAddr;

    // Start Synergy.		
    StartCommand startCommand = new StartCommand(database, engine, username, password, remoteClient, pathName, commands.getLauncher().isUnix());
    commands.executeSynergyCommand(path, startCommand);
    ccmAddr = startCommand.getCcmAddr();
    startCommand.addCcmAddrToSessionMapFile(ccmSessionMapFile);
    return ccmAddr;
  }

  /**
   * Close the Synergy session, if configured too.
   *
   * @param path	The workarea path.
   * @param commands	The Synergy command launcher
   * @param leaveSessionOpen leave session open
   * @throws IOException IOException
   * @throws InterruptedException InterruptedException
   * @throws SynergyException SynergyException
   */
  public static void closeSession(FilePath path, Commands commands, boolean leaveSessionOpen) throws IOException, InterruptedException, SynergyException {
    if (!leaveSessionOpen) {
      if (commands != null) {
        StopCommand stopCommand = new StopCommand();
        commands.executeSynergyCommand(path, stopCommand);
      }
    }
  }
}
