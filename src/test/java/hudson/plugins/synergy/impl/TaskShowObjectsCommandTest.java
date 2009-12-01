package hudson.plugins.synergy.impl;

import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

public class TaskShowObjectsCommandTest {

    private TaskShowObjectsCommand command;

    @Before
    public void setUp()
    {
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
	String toParse =    " 1) DeliverSM.java~3:java:pmaf#2         integrate kovacicm\n" +
			    " 2) Makefile~3:makefile:pmaf#12          integrate hrdyt\n" +
			    " 3) mplus-core-common.spec~2:spec:pmaf#1 integrate hrdyt\n" +
			    " 4) mplus-core-mm7.spec~2:spec:pmaf#1    integrate hrdyt\n" +
			    "10) mplus-core-smtp.spec~2:spec:pmaf#1   integrate hrdyt\n";

	command.parseResult(toParse);
	assertEquals(5, command.getObjects().size());
	assertEquals("DeliverSM.java~3:java:pmaf#2", command.getObjects().get(0));
	assertEquals("mplus-core-smtp.spec~2:spec:pmaf#1", command.getObjects().get(4));
    }
}