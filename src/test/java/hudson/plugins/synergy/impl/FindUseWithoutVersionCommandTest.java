package hudson.plugins.synergy.impl;

import java.util.Collections;
import org.junit.Test;
import static org.junit.Assert.*;

public class FindUseWithoutVersionCommandTest {

	@Test
	public void testParseResult() {
		String result = "InMemoryDataStore.java~2 integrate rmaboj java myproj pmaf#1 pmaf#493\n"+
			"myproj/api/src/main/java/com/sample/InMemoryDataStore.java~2@myproj~rmaboj\n" +
			"myproj/api/src/main/java/com/sample/InMemoryDataStore.java~2@myproj~croftl\n";

		FindUseWithoutVersionCommand command = new
			FindUseWithoutVersionCommand("InMemoryDataStore.java~2", Collections.singleton("myproj~abcd"), "~");

		command.parseResult(result);
		assertNotNull(command.getPath());
		assertEquals("myproj/api/src/main/java/com/sample/InMemoryDataStore.java", command.getPath());
	}

}