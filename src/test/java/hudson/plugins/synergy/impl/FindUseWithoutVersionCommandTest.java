package hudson.plugins.synergy.impl;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;

import org.junit.Test;

import static org.junit.Assert.*;

public class FindUseWithoutVersionCommandTest {

  /**
   * 
   */
  private static final String _I = File.separator;



  @Test
  public void testParseResult() {
    String result =
        "InMemoryDataStore.java~2 integrate rmaboj java myproj pmaf#1 pmaf#493\n" +
            "myproj" + _I + "api" + _I + "src" + _I + "main" + _I + "java" + _I + "com" + _I
            + "sample" + _I + "InMemoryDataStore.java~2@myproj~rmaboj\n" +
            "myproj" + _I + "api" + _I + "src" + _I + "main" + _I + "java" + _I + "com" + _I
            + "sample" + _I + "InMemoryDataStore.java~2@myproj~croftl\n";

    FindUseWithoutVersionCommand command = new
        FindUseWithoutVersionCommand(Arrays.asList("InMemoryDataStore.java~2"), Collections.singleton("myproj~abcd"), "~");

    command.parseResult(result);
    assertNotNull(command.getPath());
    Map<String, String> l_path = command.getPath();
    assertTrue(command.getPath().containsKey("InMemoryDataStore.java"));
    assertEquals("myproj" + _I + "api" + _I + "src" + _I + "main" + _I + "java" + _I + "com" + _I
        + "sample" + _I + "InMemoryDataStore.java", command.getPath().get("InMemoryDataStore.java"));
  }



  @Test
  public void testParseResultMultiLine() {
    String result =
        "GdaErscheinungsform.dbx~16 integrate u48hrn dbx BT9.0.0 HBT#1 MPH#503860,MPH#506099\n"
            +
            "  Projects:\n"
            +
            "    SE_Maven" + _I + "se-test" + _I + "se-db" + _I + "daten" + _I + "de" + _I + "bgnet"
            + _I + "se" + _I + "partner" + _I + "bss" + _I + "dt" + _I
            + "GdaErscheinungsform.dbx~16@SE_Maven~BT9.0.0 - u48ssk:project:HBT#1\n"
            +
            "    SE_Maven" + _I + "se-test" + _I + "se-db" + _I + "daten" + _I + "de" + _I + "bgnet"
            + _I + "se" + _I + "partner" + _I + "bss" + _I + "dt" + _I
            + "GdaErscheinungsform.dbx~16@SE_Maven~BT9.0.0.8.0:project:HBT#1\n"
            +
            "    SE_Maven" + _I + "se-test" + _I + "se-db" + _I + "daten" + _I + "de" + _I + "bgnet"
            + _I + "se" + _I + "partner" + _I + "bss" + _I + "dt" + _I
            + "GdaErscheinungsform.dbx~16@SE_Maven~u480vko_BT9.0.0:project:HBT#1\n"
            +
            "";

    FindUseWithoutVersionCommand command = new
        FindUseWithoutVersionCommand(Arrays.asList("GdaErscheinungsform.dbx~16"), Collections.singleton("SE_Maven~u480vko_BT9.0.0"), "~");

    command.parseResult(result);
    assertNotNull(command.getPath());
    Map<String, String> l_path = command.getPath();
    assertTrue(command.getPath().containsKey("GdaErscheinungsform.dbx"));
    assertEquals("SE_Maven" + _I + "se-test" + _I + "se-db" + _I + "daten" + _I + "de" + _I + "bgnet"
        + _I + "se" + _I + "partner" + _I + "bss" + _I + "dt" + _I + "GdaErscheinungsform.dbx",
        command.getPath().get("GdaErscheinungsform.dbx"));
  }

}