package hudson.plugins.synergy.impl;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import org.apache.commons.lang.StringUtils;

/**
 * Kommando sucht nach Tasks, die fuer eine Menge an definierten Releases und einem Zeitpunkt completed bzw. auch
 * excluded wurden, da diese Menge potentiell das Projekt ändern.
 *
 * @author u48jfe
 *
 */
public class CheckFolderModifiedSinceDateCommand extends Command {

  private Calendar calendar;
  private List<String> folders;

  private boolean modified = false;

  public CheckFolderModifiedSinceDateCommand(Calendar date, String folders) {
    this.calendar = date;
    this.folders = parse(folders);
  }

  private List<String> parse(String p_folders) {
    List<String> l_return = new ArrayList<String>();
    String[] split = StringUtils.split(p_folders, ";");
    for (String string : split) {
      l_return.add(string);
    }
    return l_return;
  }

  /**
   * 
   * ccm query -f %name;%modify_time "(folder('39527') or folder('39033')) and (modify_time &gt;= time('2016/04/19 13:51:42'))"
   * 
   * Output: 
   * 1) 39033;19.04.16 16:52
   * 2) 39527;19.04.16 16:58
   * 
   * {@inheritDoc}
   */
  @Override
  public String[] buildCommand(String ccmExe) {
    SimpleDateFormat format = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
    String dateAsString = format.format(calendar.getTime());
    String releaseString = "";
    for (String rel : folders) {
      if (releaseString.length() == 0) {
        releaseString = " and (";
      }
      if (releaseString.indexOf("folder") > 0) {
        releaseString = releaseString + " or ";
      }
      releaseString = releaseString + "folder('" + rel + "')";
    }
    releaseString = releaseString + ")";
    return new String[]{ccmExe, "query", "-f", "%name;%modify_time",
      "modify_time >= time('" + dateAsString + "') " + releaseString};
  }

  @Override
  public void parseResult(String result) {
    if (result != null) {
       List<String> myFolders = new ArrayList<String>(1);
      try {
        BufferedReader reader = new BufferedReader(new StringReader(result));
        String line = reader.readLine();
        while (line != null) {
          line = line.trim();
          if (line.length() != 0) {
            myFolders.add(line);
          }
          line = reader.readLine();
        }
        modified = !myFolders.isEmpty();
      } catch (IOException e) {
        // Should not happen with a StringReader.
      }
      
    }
  }

  public boolean isModified() {
    return modified;
  }
}
