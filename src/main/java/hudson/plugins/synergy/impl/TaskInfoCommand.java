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
import java.util.regex.Pattern;
import java.util.regex.Matcher;


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
		Pattern re_taskid = Pattern.compile("^.+?([0-9]+).+$");
		try {
			BufferedReader reader = new BufferedReader(new StringReader(result));
			String line = reader.readLine();
			while (line!=null) {
			   // TODO: check the format of the taskid-line in releases < 7.1
			   Matcher m = re_taskid.matcher(line);
				if (m.matches()){
					task = new TaskCompleted();
					informations.add(task);
					task.setId(m.group(1));
				}else if (line.indexOf("Synopsis:")!=-1) {
					// Check whether we have already stored task synopsis
					// (in case task description also contains 'Synopsis:')
					if (task.getSynopsis() == null){
						// TODO multiline synopsis
						task.setSynopsis(line.substring(line.indexOf(':')+1).trim());
					}
				}else if (line.indexOf("Resolver:")!=-1) {
					task.setResolver(line.substring(line.indexOf(':')+1).trim());
				} else if ((line.indexOf("Status set to 'completed'")!=-1) ||
						     (line.indexOf("Actual Completion Date:")!=-1)){ 
					try {
						String dateAsString = line.substring(0, line.lastIndexOf(':'));
						Date date = synergyFormat.parse(dateAsString);
						task.setDateCompleted(date);
					} catch (ParseException e) {
						// ignore.
						// TODO: log parsing problems to hudson logfile
					  System.out.println("ParseException: '"+line+"'");
					}
			   }
				line = reader.readLine();				
			}
		} catch (IOException e) {
			// TODO: log parsing problems to hudson logfile
			// Will not happen on a StringReader.
		}
	}
	
	public List<TaskCompleted> getInformations() {
		return informations;
	}
}
