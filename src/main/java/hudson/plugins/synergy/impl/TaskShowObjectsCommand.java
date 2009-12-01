package hudson.plugins.synergy.impl;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Performs 'ccm task -show objects taskXYZ' and returns list of modified
 * objects by that task.
 * @author galetm
 */
public class TaskShowObjectsCommand extends Command {

    private final Collection<String> tasks;
    private List<String> objects = new ArrayList<String>();

    public TaskShowObjectsCommand(Collection<String> tasks) {
	this.tasks = tasks;
    }

    @Override
    public String[] buildCommand(String ccmExe) {
	List<String> command = new ArrayList<String>();
	command.add(ccmExe);
	command.add("task");
	command.add("-show");
	command.add("objects");
	command.addAll(tasks);

	// eg: ccm task -show objects task452~1:task:pmaf task453~1:task:pmaf
	return command.toArray(new String[command.size()]);
    }

    @Override
    public void parseResult(String result) {
	objects = new ArrayList<String>();
	
	BufferedReader reader = new BufferedReader(new StringReader(result));
	try {
	    String line = reader.readLine();
	    while (line != null) {
		if (line.contains(") ")) {

		    int index = line.indexOf(") ");
		    if (index < 0)
			continue;

		    String sub = line.substring(index+2);
		    sub = sub.substring(0, sub.indexOf(" "));
		    objects.add(sub);
		}
		line = reader.readLine();
	    }
	} catch (IOException e) {
	    throw new RuntimeException(e);
	}
    }

    /**
     * Returns list of modified objects by the specified task.
     * @return list of modified objects (ex. "SampleClass.java~3:java:pmaf#2")
     */
    public List<String> getObjects() {
	return objects;
    }
}
