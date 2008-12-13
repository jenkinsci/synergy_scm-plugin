package hudson.plugins.synergy.impl;

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
	
	/**
	 * Builds a start session command.
	 * A start session command returns the CCM_ADDR to use for the following commands.
	 * 
	 * @param database		The database path
	 * @param login			The user login
	 * @param password		The user password
	 * @return				The start command. The last part of the command is the logon password.
	 */
	public StartCommand(String database, String engine, String login, String password) {
		this.database = database;
		this.engine = engine;
		this.login = login;
		this.password = password;
	}

	
	@Override
	public String[] buildCommand(String ccmAddr) {
		String[] commands = new String[]{ccmAddr, "start", "-d", database, "-h", engine, "-n", login, "-nogui", "-m", "-q", "-pw", password};
		return commands;
	}
	
	@Override
	public boolean[] buildMask() {
		boolean[] result = super.buildMask();
		result[result.length-1] = true;
		return result;
	}
	
	@Override
	public void parseResult(String result) {
		ccmAddr = result;
	}
	
	public String getCcmAddr() {
		return ccmAddr;
	}
	
	
}
