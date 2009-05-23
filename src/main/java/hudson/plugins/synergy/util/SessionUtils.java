package hudson.plugins.synergy.util;

import java.io.IOException;

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
	 * @param synergySCM	The SCM configuration.
	 * @param listener		The build listener
	 * @param launcher		The command launcher
	 * @return				A Synergy command launcher
	 */
	private static Commands configureCommands(SynergySCM synergySCM, TaskListener listener, Launcher launcher) {
		Commands commands = new Commands();
		commands.setCcmExe(synergySCM.getDescriptor().getCcmExe());
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
		
		FilePath ccmSessionMapFile = new FilePath(path, SynergySCM.CCM_SESSION_MAP_FILE_NAME);

		CheckSessionCommand checkSessionCommand = new CheckSessionCommand();
		commands.executeSynergyCommand(path, checkSessionCommand);
		String ccmAddr = checkSessionCommand.getCcmAddr(ccmSessionMapFile);
		if (CheckSessionCommand.SESSION_NOT_FOUND.equals(ccmAddr)) {			
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
		}
		commands.setCcmAddr(ccmAddr);
		
		return commands;
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
			StopCommand stopCommand = new StopCommand();
			commands.executeSynergyCommand(path, stopCommand);
		}
	}
}