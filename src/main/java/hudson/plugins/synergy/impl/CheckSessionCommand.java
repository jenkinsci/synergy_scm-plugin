package hudson.plugins.synergy.impl;

import hudson.FilePath;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class CheckSessionCommand extends Command {

  public static final String SESSION_NOT_FOUND = "SESSION_NOT_FOUND";

  private String allSessions;

  /**
   * Checks if a session allready exists. return's the CCM addr if the session allready exists
   *
   */
  public CheckSessionCommand() {
  }

  @Override
  public String[] buildCommand(String ccmAddr) {
    String[] commands = new String[]{ccmAddr, "status"};

    return commands;
  }

  @Override
  public void parseResult(String result) {
    allSessions = result;
  }

  /**
   * Return ccm session
   * @param ccmSessionMapFile FilePath
   * @return String
   * @throws IOException in case of problems reading propertiesfile
   * @throws InterruptedException in case of problems reading propertiesfile
   */
  public String getCcmAddr(FilePath ccmSessionMapFile) throws IOException, InterruptedException {
    if (!ccmSessionMapFile.exists()) {
      return SESSION_NOT_FOUND;
    }

    InputStream is = null;
    String ccmAddr = null;
    try {
      is = ccmSessionMapFile.read();
      Properties properties = new Properties();
      properties.load(is);

      ccmAddr = properties.getProperty("ccmSession");
    } finally {
      if (is != null) {
        is.close();
      }
    }

    if (ccmAddr == null || allSessions == null || !allSessions.contains(ccmAddr)) {
      return SESSION_NOT_FOUND;
    }
    
    return ccmAddr;
  }

  @Override
  public boolean isStatusOK(int status, String result) {
    return status == 0;
  }
}
