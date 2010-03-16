package hudson.plugins.synergy.util;

import java.io.IOException;

import java.util.StringTokenizer;
import java.io.File;

import hudson.FilePath;
import hudson.Launcher;
import hudson.model.TaskListener;
import hudson.plugins.synergy.SynergySCM;
import hudson.plugins.synergy.impl.CheckSessionCommand;
import hudson.plugins.synergy.impl.Commands;
import hudson.plugins.synergy.impl.StartCommand;
import hudson.plugins.synergy.impl.StopCommand;
import hudson.plugins.synergy.impl.SynergyException;

/**
 * Utility class to open and close a session.
 */
public class SessionUtils {
	/**
	 * Create and configure a Synergy command launcher.
	 *
	 * If ccmHome is not defined in a unix environment, the value of the CCM_HOME environment
	 * variable is used instead.
	 * 
	 * @param synergySCM	The SCM configuration.
	 * @param listener		The build listener
	 * @param launcher		The command launcher
	 * @return				A Synergy command launcher
	 */
	private static Commands configureCommands(SynergySCM synergySCM, TaskListener listener, Launcher launcher) {
		Commands commands = new Commands();
		String ccmExe = synergySCM.getDescriptor().getCcmExe();
		String ccmHome = synergySCM.getCcmHome();
		if (launcher.isUnix() && ccmHome!=null && ccmHome.length()!=0){
			if (((ccmHome == null) || (ccmHome.length() <= 0)) && ccmExe.startsWith("/")){
				commands.setCcmHome(System.getenv("CCM_HOME"));
				commands.setCcmExe(ccmExe);
			}else if ((ccmHome == null) || (ccmHome.length() <= 0)){
				StringTokenizer tokenizer = new StringTokenizer(System.getenv("PATH"),":");
				while (tokenizer.hasMoreTokens()) {
					String path = tokenizer.nextToken();
					if(new File(path, ccmExe).canExecute()){
						ccmHome=System.getenv("CCM_HOME");
						ccmExe=path+"/"+ccmExe;
						break;
					}
				}
				commands.setCcmHome(ccmHome);
				commands.setCcmExe(ccmExe);
			}else{
				commands.setCcmHome(ccmHome);
				commands.setCcmExe(ccmHome + "/bin/" + ccmExe);
			}
		} else {
			commands.setCcmExe(ccmExe);
		}
//		commands.setCcmExe(synergySCM.getDescriptor().getCcmExe());
		commands.setCcmUiLog(synergySCM.getDescriptor().getCcmUiLog());
		commands.setCcmEngLog(synergySCM.getDescriptor().getCcmEngLog());

		commands.setTaskListener(listener);
		commands.setLauncher(launcher);
		
		return commands;
	}
	
	/**
	 * Open or resuse a Synergy session.
	 * @param path						The workarea path
	 * @param synergySCM				The Synergy SCM configuration
	 * @param listener					The build listener
	 * @param launcher					The command launcher
	 * @return							A Synergy command launcher
	 * @throws IOException
	 * @throws InterruptedException
	 * @throws SynergyException
	 */
	public static Commands openSession(FilePath path, SynergySCM synergySCM, TaskListener listener, Launcher launcher) throws IOException, InterruptedException, SynergyException {
		Commands commands = configureCommands(synergySCM, listener, launcher);
		String ccmAddr = CheckSessionCommand.SESSION_NOT_FOUND;
		FilePath ccmSessionMapFile = new FilePath(path, SynergySCM.CCM_SESSION_MAP_FILE_NAME);
		if (synergySCM.isLeaveSessionOpen()) {
			CheckSessionCommand checkSessionCommand = new CheckSessionCommand();
			commands.executeSynergyCommand(path, checkSessionCommand);
			ccmAddr = checkSessionCommand.getCcmAddr(ccmSessionMapFile);
		}
		if (CheckSessionCommand.SESSION_NOT_FOUND.equals(ccmAddr)) {			
			ccmAddr = startSession(path, synergySCM, commands, ccmSessionMapFile);
		}		
		commands.setCcmAddr(ccmAddr);
		
		return commands;
	}

	private static String startSession(FilePath path, SynergySCM synergySCM, Commands commands, FilePath ccmSessionMapFile) throws IOException, InterruptedException, SynergyException {
		String ccmAddr;
		// Get Synergy parameters.
		String database = synergySCM.getDatabase();
		String username = synergySCM.getUsername();
		String password = synergySCM.getPassword();
		boolean remoteClient = synergySCM.isRemoteClient();
		String pathName = synergySCM.getDescriptor().getPathName();
		String engine = synergySCM.getEngine();
		
		// Start Synergy.		
		StartCommand startCommand = new StartCommand(database, engine, username, password, remoteClient, pathName);
		commands.executeSynergyCommand(path, startCommand);
		ccmAddr = startCommand.getCcmAddr();
		startCommand.addCcmAddrToSessionMapFile(ccmSessionMapFile);
		return ccmAddr;
	}
	
	/**
	 * Close the Synergy session, if configured too.
	 * @param path						The workarea path.
	 * @param synergySCM				The Synergy SCM configuration
	 * @param commands					The Synergy command launcher
	 * @throws IOException
	 * @throws InterruptedException
	 * @throws SynergyException
	 */
	public static void closeSession(FilePath path, SynergySCM synergySCM, Commands commands) throws IOException, InterruptedException, SynergyException {		
		if (!synergySCM.isLeaveSessionOpen()) {
			if (commands!=null) {
				StopCommand stopCommand = new StopCommand();
				commands.executeSynergyCommand(path, stopCommand);
			}
		}
	}
}
