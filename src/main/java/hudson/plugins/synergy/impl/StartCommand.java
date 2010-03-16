package hudson.plugins.synergy.impl;

import hudson.FilePath;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.StringTokenizer;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

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
	
	private transient int passwordIndex = -1;
	private boolean isWebmodeSession;
	private boolean isRunningOnUnix = false;

	/**
	 * Builds a start session command. 
     * A start session command returns the CCM_ADDR to use for the following commands.
	 * 
	 * @param database		The database path
	 * @param login			The user login
	 * @param password		The user password
	 * @param remoteClient	Use remote client flag
	 * @param pathName      The path name
	 */
	public StartCommand(String database, String engine, String login, String password, boolean remoteClient, String pathName) {
		this.database = database;
		this.engine = engine;
		this.login = login;
		this.password = password;
		this.remoteClient = remoteClient;
		this.pathName = pathName;

		isWebmodeSession = false;
		Pattern re_webmode = Pattern.compile("^https?://..*$");
		if (re_webmode.matcher(engine).matches()){
			isWebmodeSession = true;
		}

		// TODO: implement a better way to detect unix runtime environment
		if (System.getProperty("file.separator").equals("/")){
			isRunningOnUnix = true;
		}
	}

	@Override
	public String[] buildCommand(String ccmAddr) {
		// Creates an array of required parameters.
		String[] commands = new String[] { ccmAddr, "start", "-d", database, "-nogui", "-m", "-q" };               	        

		List<String> list = new ArrayList<String>(Arrays.asList(commands));
		
		// Add "-h engine" or "-s engine" if engine is set.
		if (engine!=null && engine.length()!=0) {
			if (isWebmodeSession){
				list.add("-s");
			}else{
				list.add("-h");
			}
			list.add(engine);
		}
		
		// Add "-n login" if login is set
		// Unix commandline client does not support the "-n" option
		if (login!=null && login.length()!=0 && !isRunningOnUnix) {
			list.add("-n");
			list.add(login);
		}
		
		// Add "-pw password" if password is set
		if (password!=null && password.length()!=0) {
			list.add("-pw");
			list.add(password);
			passwordIndex = list.size()-1;
		}

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

   /* 
    * This Method marks the position for masking the password
    * (see commands array in this.buildCommand())
    */
	@Override
	public boolean[] buildMask() {
		boolean[] result = super.buildMask();
		if (passwordIndex!=-1) {
			result[passwordIndex] = true;
		}
		return result;
	}

	@Override
	public void parseResult(String result) {
		StringTokenizer tokenizer = new StringTokenizer(result,"\t\n\r\f");
		while (tokenizer.hasMoreTokens()) {
			String token = tokenizer.nextToken();
			if (!token.startsWith("Warning")) {
				ccmAddr = token;
				break;
			}
		}
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
