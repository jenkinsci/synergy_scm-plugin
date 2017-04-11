package hudson.plugins.synergy.impl;

import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Test;

public class TaskShowObjectsCommandTest {

  private TaskShowObjectsCommand command;

  @Before
  public void setUp() {
    command = new TaskShowObjectsCommand(null);
  }

  @Test
  public void testParseSingleResult() {
    String toParse = "1) DeliverSM.java~3:java:pmaf#2 integrate kovacicm\n";

    command.parseResult(toParse);
    assertEquals(1, command.getObjects().size());
    assertEquals("DeliverSM.java~3:java:pmaf#2", command.getObjects().get(0));
  }

  @Test
  public void testParseMultipleResult() {
    String toParse = " 1) DeliverSM.java~3:java:pmaf#2         integrate kovacicm\n"
        + " 2) Makefile~3:makefile:pmaf#12          integrate hrdyt\n"
        + " 3) mplus-core-common.spec~2:spec:pmaf#1 integrate hrdyt\n"
        + " 4) mplus-core-mm7.spec~2:spec:pmaf#1    integrate hrdyt\n"
        + "10) mplus-core-smtp.spec~2:spec:pmaf#1   integrate hrdyt\n";

    command.parseResult(toParse);
    assertEquals(5, command.getObjects().size());
    assertEquals("DeliverSM.java~3:java:pmaf#2", command.getObjects().get(0));
    assertEquals("mplus-core-smtp.spec~2:spec:pmaf#1", command.getObjects().get(4));
  }

  @Test
  public void testParseNameWithWhitespace() {
    String toParse = "1) CADI MDC Workbooks 2011-10-13~1:dir:slc#1 integrate sarobson\n";

    command.parseResult(toParse);
    assertEquals(1, command.getObjects().size());
    assertEquals("CADI MDC Workbooks 2011-10-13~1:dir:slc#1", command.getObjects().get(0));
  }
}
