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
public class FindCompletedSinceDateCommand extends Command {

  private Calendar calendar;
  private List<String> release;

  private List<String> tasks;

  public FindCompletedSinceDateCommand(Calendar date, String release) {
    this.calendar = date;
    this.release = parse(release);
  }

  private List<String> parse(String p_releases) {
    List<String> l_return = new ArrayList<String>();
    String[] split = StringUtils.split(p_releases, ";");
    for (String string : split) {
      l_return.add(string);
    }
    return l_return;
  }

  /**
   * ccm query -t task -u -f %objectname "(excluded_time &gt;= time('2014/10/07 13:51:42') or completion_date &gt;=
   * time('2014/10/07 13:51:42')) and (release='BT9.0.0' or release='BT8.0_COPY' or release='BT8.1_COPY' or
   * release='BT8.2_COPY' or release='BT9.0_COPY')" {@inheritDoc}
   */
  @Override
  public String[] buildCommand(String ccmExe) {
    SimpleDateFormat format = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
    String dateAsString = format.format(calendar.getTime());
    String releaseString = "";
    for (String rel : release) {
      if (releaseString.length() == 0) {
        releaseString = " and (";
      }
      if (releaseString.indexOf("release") > 0) {
        releaseString = releaseString + " or ";
      }
      releaseString = releaseString + "release='" + rel + "'";
    }
    releaseString = releaseString + ")";
    return new String[]{ccmExe, "query", "-t", "task", "-u", "-f", "%objectname",
      "(excluded_time >= time('" + dateAsString + "') or completion_date >= time('" + dateAsString + "'))" + releaseString};
  }

  @Override
  public void parseResult(String result) {
    if (result != null) {
      tasks = new ArrayList<String>(1);
      try {
        BufferedReader reader = new BufferedReader(new StringReader(result));
        String line = reader.readLine();
        while (line != null) {
          line = line.trim();
          if (line.length() != 0) {
            tasks.add(line);
          }
          line = reader.readLine();
        }
      } catch (IOException e) {
        // Should not happen with a StringReader.
      }
    }
  }

  public List<String> getTasks() {
    return tasks;
  }
}
