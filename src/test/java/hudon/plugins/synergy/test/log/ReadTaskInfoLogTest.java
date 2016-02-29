package hudon.plugins.synergy.test.log;

import hudson.plugins.synergy.impl.TaskCompleted;
import hudson.plugins.synergy.impl.TaskInfoCommand;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class ReadTaskInfoLogTest extends AbstractLogTest {
	/**
	 * Tests the task info log 1.
	 */
	public void testTaskInfoCommandSyngery65() throws IOException {
		// Test a Synergy 6.5 task info log.
		TaskInfoCommand taskCommand = new TaskInfoCommand(new ArrayList<String>());
		String log = readLog("logs/tasks/task1.log.txt");
		taskCommand.parseResult(log);
		List<TaskCompleted> tasks = taskCommand.getInformations();
		
		// Parsing of the log should not be null.
		assertNotNull(tasks);
		
		// There are 1 task in this log.
		assertEquals(1,tasks.size());
		
		// Check the task elements.
		Calendar cal = Calendar.getInstance();
		cal.clear();
		cal.set(Calendar.YEAR, 2010);
		cal.set(Calendar.MONTH, 2);
		cal.set(Calendar.DAY_OF_MONTH, 16);
		cal.set(Calendar.HOUR_OF_DAY, 17);
		cal.set(Calendar.MINUTE, 10);
		cal.set(Calendar.SECOND, 32);
		assertEquals("10827", tasks.get(0).getId());
		assertEquals("dhet", tasks.get(0).getResolver());
		assertEquals("Ajout LNG_CODE vue interface uti", tasks.get(0).getSynopsis());
		assertEquals(cal.getTime(), tasks.get(0).getDateCompleted());
	}
	/**
	 * Tests the task info log 2.
	 */
	public void testTaskInfoCommandSynergy71() throws IOException {
		// Test a Synergy 7.1 task info log.
		TaskInfoCommand taskCommand = new TaskInfoCommand(new ArrayList<String>());
		String log = readLog("logs/tasks/task2.log.txt");
		taskCommand.parseResult(log);
		List<TaskCompleted> tasks = taskCommand.getInformations();
		
		// Parsing of the log should not be null.
		assertNotNull(tasks);
		
		// There are 1 task in this log.
		assertEquals(1,tasks.size());
	
		// Sat Apr 17 07:55:32 CEST 2010
		// Check the task elements.
		Calendar cal = Calendar.getInstance();
		cal.clear();
		cal.set(Calendar.YEAR, 2010);
		cal.set(Calendar.MONTH, 2);
		cal.set(Calendar.DAY_OF_MONTH, 17);
		cal.set(Calendar.HOUR_OF_DAY, 7);
		cal.set(Calendar.MINUTE, 55);

		assertEquals("462", tasks.get(0).getId());
		assertEquals("BacAn2d", tasks.get(0).getResolver());
		assertEquals("JavaDoc Foobar (hehehe) gnarf?", tasks.get(0).getSynopsis());
		assertEquals(cal.getTime(), tasks.get(0).getDateCompleted());
	}

        /**
	 * Tests the task info log 3.
	 */
	public void testTaskInfoCommandSynergy71DefaultLocale() throws IOException {
		// Test a Synergy 7.1 task info log.
		TaskInfoCommand taskCommand = new TaskInfoCommand(new ArrayList<String>());
		String log = readLog("logs/tasks/task3.log.txt");
		taskCommand.parseResult(log);
		List<TaskCompleted> tasks = taskCommand.getInformations();

		// Parsing of the log should not be null.
		assertNotNull(tasks);

		// There are 1 task in this log.
		assertEquals(1,tasks.size());

		// 17.01.11 14:22
		// Check the task elements.
		Calendar cal = Calendar.getInstance();
		cal.clear();
		cal.set(Calendar.YEAR, 2011);
		cal.set(Calendar.MONTH, 0);
		cal.set(Calendar.DAY_OF_MONTH, 17);
		cal.set(Calendar.HOUR_OF_DAY, 14);
		cal.set(Calendar.MINUTE, 22);

		assertEquals("402797", tasks.get(0).getId());
		assertEquals("u48jfe", tasks.get(0).getResolver());
		assertEquals("PT107479: Log-Anpassung in Uniserv MailClient", tasks.get(0).getSynopsis());
		assertEquals(cal.getTime(), tasks.get(0).getDateCompleted());
}

}
