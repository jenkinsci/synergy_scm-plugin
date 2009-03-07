package hudson.plugins.synergy.impl;

public class Conflict {
	private String objectname;
	private String task;
	private String message;
	private String type;
	
	public Conflict(String objectname, String task, String type, String message) {
		this.objectname = objectname;
		this.task = task;
		this.type = type;
		this.message = message;
	}
	
	public String getMessage() {
		return message;
	}
	public String getObjectname() {
		return objectname;
	}
	public String getTask() {
		return task;
	}
	public String getType() {
		return type;
	}
}
