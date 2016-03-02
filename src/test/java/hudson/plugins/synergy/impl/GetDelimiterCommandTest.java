package hudson.plugins.synergy.impl;

import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Test;

public class GetDelimiterCommandTest {

  private GetDelimiterCommand command;

  public GetDelimiterCommandTest() {

  }

  @Before
  public void setUp() {
    command = new GetDelimiterCommand();
  }

  @Test
  public void testParseResultWithNewline() {
    String testInput = "~\r\n";
    command.parseResult(testInput);
    String result = command.getDelimiter();
    assertEquals("~", result);
  }

}
