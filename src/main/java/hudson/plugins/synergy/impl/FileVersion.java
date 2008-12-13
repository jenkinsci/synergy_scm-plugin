package hudson.plugins.synergy.impl;

import java.util.List;

import hudson.scm.EditType;

public class FileVersion {
	final public static String EDIT = "Edit";
	final public static String DELETE = "Delete";
	final public static String ADD = "Add";
	
	private String name;

	private String version;

	private String type;

	private String instance;

	private String projectPath;

	private String resolver;

	private int task;

	private String  action;

	public FileVersion() {

	}

	public FileVersion(String name, String version, String type, String instance) {
		this.name = name;
		this.version = version;
		this.type = type;
		this.instance = instance;
	}

	public FileVersion(String name, String version, String type,
			String instance, String projectPath, String resolver, int task) {
		this.name = name;
		this.version = version;
		this.type = type;
		this.instance = instance;
		this.projectPath = projectPath;
		this.resolver = resolver;
		this.task = task;
	}

	public boolean equals(FileVersion file) {
		if (file != null) {
			super.equals(file);
			if ((this.name!=null) && ((file.name == null) || (!file.name.equals(this.name)))) {
				return false;
			}
			if ((this.version!=null) && ((file.version == null) || (!file.version.equals(this.version)))) {
				return false;
			}
			if ((this.type!=null) && ((file.type == null) || (!file.type.equals(this.type)))) {
				return false;
			}
			if ((this.instance!=null) && ((file.instance == null) || (!file.instance.equals(this.instance)))) {
				return false;
			}
			if ((this.projectPath!=null) && ((file.projectPath == null) || (!file.projectPath.equals(this.projectPath)))) {
				return false;
			}
			if ((this.resolver!=null) && ((file.resolver == null) || (!file.resolver.equals(this.resolver)))) {
				return false;
			}
			if ((this.task>0) && ((file.task == 0) || (!(file.task == this.task)))) {
				return false;
			}
			if ((this.action!=null) && ((file.action == null) || (!file.action.equals(this.action)))) {
				return false;
			}
			return true;
		}
		return false;
	}
	public boolean equalsTask(FileVersion file) {
		if (file != null) {
			super.equals(file);
			if ((this.name!=null) && ((file.name == null) || (!file.name.equals(this.name)))) {
				return false;
			}
			if ((this.version!=null) && ((file.version == null) || (!file.version.equals(this.version)))) {
				return false;
			}
			if ((this.type!=null) && ((file.type == null) || (!file.type.equals(this.type)))) {
				return false;
			}
			if ((this.instance!=null) && ((file.instance == null) || (!file.instance.equals(this.instance)))) {
				return false;
			}
			return true;
		}
		return false;
	}
	public boolean searchInList(List<FileVersion> files) {
		for (FileVersion fileSearch : files) {
			if (fileSearch.equals(this)) {
				return true;
			}
		}
		return false;
	}
	public FileVersion getSearchInList(List<FileVersion> files) {
		for (FileVersion fileSearch : files) {
			if (fileSearch.equalsTask(this)) {
				return fileSearch;
			}
		}
		return null;
	}
	public EditType fileToAdd() {
		return EditType.ADD;
	}

	public EditType fileToModify() {
		return EditType.EDIT;
	}

	public EditType fileToDelete() {
		return EditType.DELETE;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getVersion() {
		return version;
	}

	public void setVersion(String version) {
		this.version = version;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public String getInstance() {
		return instance;
	}

	public void setInstance(String instance) {
		this.instance = instance;
	}



	public String getResolver() {
		return resolver;
	}

	public void setResolver(String resolver) {
		this.resolver = resolver;
	}

	public int getTask() {
		return task;
	}

	public void setTask(int task) {
		this.task = task;
	}



	public String getAction() {
		return action;
	}

	public void setAction(String action) {
		this.action = action;
	}

	public String getProjectPath() {
		return projectPath;
	}

	public void setProjectPath(String projectPath) {
		this.projectPath = projectPath;
	}

}
