package hudson.plugins.synergy.impl;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;


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
