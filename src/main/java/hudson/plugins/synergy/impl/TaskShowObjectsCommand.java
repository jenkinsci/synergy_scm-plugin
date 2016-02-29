package hudson.plugins.synergy.impl;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang.StringUtils;

/**
 * Performs 'ccm task -show objects taskXYZ' and returns list of modified
 * objects by that task.
 * @author galetm
 */
public class TaskShowObjectsCommand extends Command {

    private final Collection<String> tasks;
    private List<String> objects = new ArrayList<String>();
    private Map<String, Collection<String>> l_map = new HashMap<String, Collection<String>>();

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

        // format
        command.add("-format");
        command.add("%objectname");

	command.addAll(tasks);



	// eg: ccm task -show objects task452~1:task:pmaf task453~1:task:pmaf
	return command.toArray(new String[command.size()]);
    }

    @Override
    public void parseResult(String result) {
        if (result != null) {
            ArrayList<String> l_objects = new ArrayList<String>();
            String taskNr = null;
	
	BufferedReader reader = new BufferedReader(new StringReader(result));
	try {
	    String line = reader.readLine();
	    while (line != null) {
		if (line.contains(") ")) {

		    int index = line.indexOf(") ");
                        if (index < 0) {
			continue;
                        }

		    String sub = line.substring(index+2);
                        int indexOf = StringUtils.indexOf(sub, " ", StringUtils.lastIndexOf(sub, "#"));
                        if (indexOf == -1) {
                            indexOf = sub.length();
		}
                        // wegen formatierung und rueck-kompatibilitaet
                        sub = StringUtils.substring(sub, 0, indexOf);
                        // sub = StringUtils.split(sub)[0];
                        l_objects.add(sub);
                    } else {
                        // newline with task
                        if (StringUtils.stripToNull(line) != null) {
                            saveObjects(taskNr, l_objects);

                            taskNr = parseTaskNr(line);
                            l_objects = new ArrayList<String>();
                        }
                    }
		line = reader.readLine();
	    }
                saveObjects(taskNr, l_objects);
	} catch (IOException e) {
	    throw new RuntimeException(e);
	}
    }
    }

    private void saveObjects(String taskNr, ArrayList<String> l_objects) {
        // save old task
        if (taskNr != null) {
            l_map.put(taskNr, l_objects);
        }
        objects.addAll(l_objects);
    }

    /**
     * Returns list of modified objects by the specified task.
     * @return list of modified objects (ex. "SampleClass.java~3:java:pmaf#2")
     */
    public List<String> getObjects() {
	return objects;
    }

    /**
     * Returns list of modified objects by the specified task.
     * @return list of modified objects (ex. "SampleClass.java~3:java:pmaf#2")
     */
    public Map<String, Collection<String>> getMap() {
        return l_map;
}

    private String parseTaskNr(String line) {
        // Task MPH#400969:
        return line.substring(line.indexOf("Task ") + 5, line.indexOf(":"));
    }
}
