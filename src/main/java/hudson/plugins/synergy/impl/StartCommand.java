package hudson.plugins.synergy.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * A start session command.
 * A start session command returns the CCM_ADDR to use for the following commands.
 */
public class StartCommand extends Command {
	private String ccmAddr;
	private String login;
	private String database;
	private String engine;
	private String password;
	private boolean remoteClient;
	private String pathName;
	
	/**
	 * Builds a start session command.
	 * A start session command returns the CCM_ADDR to use for the following commands.
	 * 
	 * @param database		The database path
	 * @param login			The user login
	 * @param password		The user password
	 * @param remoteClient	Use remote client flag
	 * @return				The start command. The last part of the command is the logon password.
	 */
	public StartCommand(String database, String engine, String login, String password, boolean remoteClient, String pathName) {
		this.database = database;
		this.engine = engine;
		this.login = login;
		this.password = password;
		this.remoteClient = remoteClient;
		this.pathName = pathName;
	}

	
	@Override
	public String[] buildCommand(String ccmAddr) {
		// Creates an array of required parameters.
		String[] commands = new String[]{ccmAddr, "start", "-d", database, "-h", engine, "-n", login, "-nogui", "-m", "-q", "-pw", password};		
		List<String> list = new ArrayList<String>(Arrays.asList(commands));
		
		// Add "-rc" parameter if required at the end of the array.
		if (remoteClient) {			
			list.add("-rc");	
		}
		
		// Add "-u pathname" if pathname is set.
		if (pathName!=null) {
			list.add("-u");
			list.add(pathName);
		}		
		
		commands = list.toArray(new String[list.size()]);
		return commands;
	}
	
	@Override
	public boolean[] buildMask() {
		boolean[] result = super.buildMask();
		int pwdIndex = 12;
		result[pwdIndex] = true;
		return result;
	}
	
	@Override
	public void parseResult(String result) {
		ccmAddr = result;
	}
	
	public String getCcmAddr() {
		return ccmAddr;
	}
	
	@Override
	public boolean isStatusOK(int status) {
		return status==0;
	}
	
}
