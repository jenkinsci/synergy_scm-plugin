package hudson.plugins.synergy.impl;

import java.util.Date;
import java.util.List;

public class TaskCompleted {
	private String id;
	
	private String synopsis;
	
	private String resolver;
	
	private Date dateCompleted;
	
	public TaskCompleted(){
		super();
	}
	public TaskCompleted(String id, String synopsis, String resolver, Date dateCompleted){
		super();
		this.id = id;
		this.synopsis =synopsis;
		this.resolver=resolver;
		this.dateCompleted= dateCompleted;
		
	}
	public boolean equals (TaskCompleted task){
		super.equals(task);
		if (task.id==this.id){
			return true;
		}
		return false;
	}
	public boolean searchTask(List<TaskCompleted> tasks){
		for (TaskCompleted taskSearch: tasks){
			if (taskSearch.equals(this)){
				return true;
			}
		}
		return false;
	}
	public String getId() {
		return id;
	}
	public void setId(String id) {
		this.id = id;
	}
	public String getSynopsis() {
		return synopsis;
	}
	public void setSynopsis(String synopsis) {
		this.synopsis = synopsis;
	}
	public String getResolver() {
		return resolver;
	}
	public void setResolver(String resolver) {
		this.resolver = resolver;
	}
	public Date getDateCompleted() {
		return dateCompleted;
	}
	public void setDateCompleted(Date dateCompleted) {
		this.dateCompleted = dateCompleted;
	}
}
