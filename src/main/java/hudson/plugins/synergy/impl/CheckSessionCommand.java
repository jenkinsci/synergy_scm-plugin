package hudson.plugins.synergy.impl;

import hudson.FilePath;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class CheckSessionCommand extends Command {

	public static final String SESSION_NOT_FOUND = "SESSION_NOT_FOUND";

	private String allSessions;

	/**
	 * Checks if a session allready exists. return's the CCM addr if the session
	 * allready exists
	 * 
	 * @param filePath
	 */
	public CheckSessionCommand() {
	}

	@Override
	public String[] buildCommand(String ccmAddr) {
		String[] commands = new String[] { ccmAddr, "status" };

		return commands;
	}

	@Override
	public void parseResult(String result) {
		allSessions = result;
	}

	public String getCcmAddr(FilePath ccmSessionMapFile) throws IOException, InterruptedException {
		if (!ccmSessionMapFile.exists())
			return SESSION_NOT_FOUND;

		InputStream is = null;
		String ccmAddr = null;
		try {
			is = ccmSessionMapFile.read();
			Properties properties = new Properties();
			properties.load(is);

			ccmAddr = properties.getProperty("ccmSession");
		} finally {
			if (is != null)
				is.close();
		}

		if (allSessions.indexOf(ccmAddr) < 0)
			return SESSION_NOT_FOUND;

		if (ccmAddr == null)
			return SESSION_NOT_FOUND;

		return ccmAddr;
	}

	@Override
	public boolean isStatusOK(int status) {
		return status == 0;
	}
}
