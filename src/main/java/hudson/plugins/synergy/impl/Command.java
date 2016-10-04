package hudson.plugins.synergy.impl;

import java.util.Arrays;

/**
 * Synergy text command returning some text.
 */
public abstract class Command {

  /**
   * Return the Synergy command line to execute as an array. The line must start
   * with the ccmExe location, which is given as the first method parameter.
   *
   * @param ccmExe path to executable
   * @return String[] command as command line
   */
  public abstract String[] buildCommand(String ccmExe);

  /**
   * Parse the command result.
   *
   * @param result input from command line
   */
  public abstract void parseResult(String result);

  /**
   * Build a mask of values in the command line that should not be logged.
   *
   * @return boolean[]
   */
  public boolean[] buildMask() {
    return new boolean[buildCommand(null).length];
  }

  /**
   * Return true if the given return status is ok for the command. The default
   * is to return true if the status is 0 or 1 with empty output.
   *
   * Testing only on 0 is not enough for "ccm query" commands as they return 1
   * for no result. Testing 0 or 1 is not enough for "ccm query" commands as
   * they return 1 for failure.
   *
   * @param status	The ccm process return code
   * @param output	The ccm process output
   * @return boolean
   */
  public boolean isStatusOK(int status, String output) {
    return status == 0 || ((status == 1 || status == 6) && (output == null || output.length() == 0));
  }

  @Override
  public String toString() {
    return Arrays.toString(buildCommand("ccm.executable"));
  }

}
