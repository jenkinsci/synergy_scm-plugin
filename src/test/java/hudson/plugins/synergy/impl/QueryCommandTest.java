package hudson.plugins.synergy.impl;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import static org.junit.Assert.*;
import org.junit.Test;

public class QueryCommandTest {

  private final static String FS = "\034";
  private final static String RS = "\036";

  @Test
  public void testSimple() {
    QueryCommand cmd = new QueryCommand("", Arrays.asList(new String[]{"objectname", "version"}));
    cmd.parseResult("InMemoryDataStore.java~2:java:MPH#1" + RS + "2" + FS + "\nInMemoryDataStore.java~3:java:MPH#1" + RS + "3" + FS + "\n");
    List<Map<String, String>> res = cmd.getQueryResult();
    if (res.size() != 2) {
      fail("should return 2 elements");
    }
    if (!res.get(0).get("objectname").equals("InMemoryDataStore.java~2:java:MPH#1")) {
      fail("should return InMemoryDataStore.java~2:java:MPH#1");
    }
    if (!res.get(0).get("version").equals("2")) {
      fail("should return 2");
    }
    if (!res.get(1).get("objectname").equals("InMemoryDataStore.java~3:java:MPH#1")) {
      fail("should return InMemoryDataStore.java~3:java:MPH#1");
    }
    if (!res.get(1).get("version").equals("3")) {
      fail("should return 3");
    }
  }

  @Test
  public void testMultiLine() {
    QueryCommand cmd = new QueryCommand("", Arrays.asList(new String[]{"task_number", "task_description"}));
    cmd.parseResult("1234" + RS + "Zeile 1\nZeile2" + FS + "\n");
    List<Map<String, String>> res = cmd.getQueryResult();
    if (res.size() != 1) {
      fail("should return 1 element");
    }
    if (!res.get(0).get("task_number").equals("1234")) {
      fail("should return 1234");
    }
    if (!res.get(0).get("task_description").equals("Zeile 1\nZeile2")) {
      fail("should return Zeile 1\nZeile2");
    }
  }
}
