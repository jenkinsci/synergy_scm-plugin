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
	 * Single task, singleline synopsis, Synergy 6.5 format
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
	 * Tests the task info log 1.
	 * single task, multiline synopsis, Synergy 6.5 format
	 */
	public void testTaskInfoCommandMulitlineSynopsisSyngery65() throws IOException {
		// Test a Synergy 6.5 task info log.
		TaskInfoCommand taskCommand = new TaskInfoCommand(new ArrayList<String>());
		String log = readLog("logs/tasks/task1_multiline_synopsis.log.txt");
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
		assertEquals("Ajout LNG_CODE vue interface\nuti", tasks.get(0).getSynopsis());
		assertEquals(cal.getTime(), tasks.get(0).getDateCompleted());
	}

	/**
	 * Tests the task info log 1.
	 * multiple tasks, one with singleline synopsis, one with multiline synopsis, Synergy 6.5 format
	 */
	public void testTaskInfoCommandMultipleTasksSyngery65() throws IOException {
		// Test a Synergy 6.5 task info log.
		TaskInfoCommand taskCommand = new TaskInfoCommand(new ArrayList<String>());
		String log = readLog("logs/tasks/task1_multiple_tasks.log.txt");
		taskCommand.parseResult(log);
		List<TaskCompleted> tasks = taskCommand.getInformations();
		
		// Parsing of the log should not be null.
		assertNotNull(tasks);
		
		// There are 1 task in this log.
		assertEquals(2,tasks.size());
		
		// Check the task elements.
		for (int i=0;i<2;i++){
			TaskCompleted task=tasks.get(i);
		Calendar cal = Calendar.getInstance();
		cal.clear();
		cal.set(Calendar.YEAR, 2010);
		cal.set(Calendar.MONTH, 2);
		cal.set(Calendar.DAY_OF_MONTH, 16);
		cal.set(Calendar.HOUR_OF_DAY, 17);
		cal.set(Calendar.MINUTE, 10);
		cal.set(Calendar.SECOND, 32);
		if (i==0){
			assertEquals("10827", task.getId());
			assertEquals("Ajout LNG_CODE vue interface uti", task.getSynopsis());
		}else if(i==1){
			assertEquals("10828", task.getId());
			assertEquals("Ajout LNG_CODE vue interface\nuti", task.getSynopsis());
		}
		assertEquals("dhet", task.getResolver());
		assertEquals(cal.getTime(), task.getDateCompleted());
		}
	}
	
	/**
	 * Tests the task info log 1.
	 * Single task, singleline synopsis, Synergy 6.5 format
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

}
