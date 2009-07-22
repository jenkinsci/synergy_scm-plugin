package hudson.plugins.synergy.impl;

import hudson.FilePath;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

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
	 * @return 				The start command. The last part of the command is the logon password.
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
		String[] commands = new String[] { ccmAddr, "start", "-d", database, "-h", engine, "-n", login, "-nogui", "-m", "-q", "-pw", password };
		List<String> list = new ArrayList<String>(Arrays.asList(commands));

		// Add "-rc" parameter if required at the end of the array.
		if (remoteClient) {
			list.add("-rc");
		}
		
		// Add "-u pathname" if pathname is set.
		if (pathName!=null && pathName.length()!=0) {
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
	public boolean isStatusOK(int status, String result) {
		return status == 0;
	}

	public void addCcmAddrToSessionMapFile(FilePath ccmSessionMapFile) throws IOException, InterruptedException {
		InputStream is = null;
		OutputStream os = null;
		try {
			Properties properties = new Properties();
			if (ccmSessionMapFile.exists()) {
				is = ccmSessionMapFile.read();
				properties.load(is);
			}

			properties.put("ccmSession", ccmAddr);

			os = ccmSessionMapFile.write();
			properties.store(os, null);
		} finally {
			if (is != null)
				is.close();
			if (os != null)
				os.close();
		}

	}
}
