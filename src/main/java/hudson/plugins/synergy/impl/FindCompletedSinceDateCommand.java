package hudson.plugins.synergy.impl;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class FindCompletedSinceDateCommand extends Command {
	private Calendar calendar;
	private String release;
	
	private List<String> tasks;
	
	public FindCompletedSinceDateCommand(Calendar date, String release) {
		this.calendar = date;
		this.release = release;
	}
	
	@Override
	public String[] buildCommand(String ccmExe) {
		SimpleDateFormat format = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
		String dateAsString = format.format(calendar.getTime());
		return new String[]{ccmExe, "query", "-t", "task", "-u", "-f", "%objectname", "completion_date >= time('" + dateAsString + "') and release='" + release + "'"};
	}
	@Override
	public void parseResult(String result) {
		tasks = new ArrayList<String>(1);
		try {
			BufferedReader reader = new BufferedReader(new StringReader(result));
			String line = reader.readLine();
			while (line!=null) {
				line = line.trim();
				if (line.length()!=0) {
					tasks.add(line);
				}
				line = reader.readLine();
			}
		} catch (IOException e) {
			// Should not happen with a StringReader.
		}
	}
	
	public List<String> getTasks() {
		return tasks;
	}
}
