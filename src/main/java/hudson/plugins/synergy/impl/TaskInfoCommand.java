package hudson.plugins.synergy.impl;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Get information about tasks.
 * 
 * @author jrbe
 * 
 * TODO : work with several task at once.
 */
public class TaskInfoCommand extends Command {
	private DateFormat synergyFormat = new SimpleDateFormat("EE MMM dd hh:mm:ss yyyy", Locale.ENGLISH);
	
	private List<String> tasks;
	private List<TaskCompleted> informations;
	
	public TaskInfoCommand(List<String> tasks) {
		this.tasks = tasks;
	}
	
	@Override
	public String[] buildCommand(String ccmExe) {
		String[] commands = new String[]{ccmExe, "task", "-show", "info", tasks.get(0)};
		return commands;
	}
	
	@Override
	public void parseResult(String result) {
		informations = new ArrayList<TaskCompleted>();
		TaskCompleted task = null;		
		try {
			BufferedReader reader = new BufferedReader(new StringReader(result));
			String line = reader.readLine();
			while (line!=null) {
				if (line.startsWith("Task:")) {
					task = new TaskCompleted();
					informations.add(task);
					task.setId(line.substring(line.indexOf(':')+1).trim());
				}else if (line.indexOf("Synopsis:")!=-1) {
					// TODO multiline synopsis
					task.setSynopsis(line.substring(line.indexOf(':')+1).trim());
				} else if (line.indexOf("Resolver:")!=-1) {
					task.setResolver(line.substring(line.indexOf(':')+1).trim());
				} else if (line.indexOf("Status set to 'completed'")!=-1) try {
					String dateAsString = line.substring(0, line.lastIndexOf(':'));
					Date date = synergyFormat.parse(dateAsString);
					task.setDateCompleted(date);
				} catch (ParseException e) {
					// ignore.
				}
				line = reader.readLine();				
			}
		} catch (IOException e) {
			// Will not happen on a StringReader.
		}
	}
	
	public List<TaskCompleted> getInformations() {
		return informations;
	}
}
