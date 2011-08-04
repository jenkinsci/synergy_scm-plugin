package hudson.plugins.synergy;

import hudson.model.AbstractBuild;
import hudson.model.User;
import hudson.scm.ChangeLogSet;
import hudson.scm.EditType;

import java.io.IOException;
import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.kohsuke.stapler.export.Exported;
import org.kohsuke.stapler.export.ExportedBean;

public final class SynergyChangeLogSet extends ChangeLogSet<SynergyChangeLogSet.LogEntry> {

	private final List<LogEntry> logs;

	/**
	 * @GuardedBy this
	 */
	private Map<String, Long> revisionMap;

	public SynergyChangeLogSet(AbstractBuild build, List<LogEntry> logs) {
		super(build);
		
		// we want recent changes first
		// TODO
		/*
		Collections.sort(logs, new Comparator<LogEntry>() {
			public int compare(LogEntry a, LogEntry b) {
				return b.getTaskId().compareTo(a.get)
			}
		});
		*/
		this.logs = Collections.unmodifiableList(logs);
		for (LogEntry log : logs)
			log.setParent(this);
	}

	public boolean isEmptySet() {
		return logs.isEmpty();
	}

	public List<LogEntry> getLogs() {
		return logs;
	}

	public Iterator<LogEntry> iterator() {
		return logs.iterator();
	}

	public synchronized Map<String, Long> getRevisionMap() throws IOException {
		if (revisionMap == null)
			revisionMap = SynergySCM.parseRevisionFile(build);
		return revisionMap;
	}

	@Exported
	public List<RevisionInfo> getRevisions() throws IOException {
		List<RevisionInfo> r = new ArrayList<RevisionInfo>();
		for (Map.Entry<String, Long> e : getRevisionMap().entrySet())
			r.add(new RevisionInfo(e.getKey(), e.getValue()));
		return r;
	}

	@ExportedBean(defaultVisibility = 999)
	public static final class RevisionInfo {
		@Exported
		public final String module;
		@Exported
		public final long revision;

		public RevisionInfo(String module, long revision) {
			this.module = module;
			this.revision = revision;
		}
	}

	/**
	 * One commit.
	 * <p>
	 * Setter methods are public only so that the objects can be constructed
	 * from Digester. So please consider this object read-only.
	 */
	public static class LogEntry extends ChangeLogSet.Entry {
		private String taskId;
		private int version;
		private User author;
		private String date;
		private String msg;
		private List<Path> paths = new ArrayList<Path>();

		/**
		 * Gets the {@link SynergyChangeLogSet} to which this change set
		 * belongs.
		 */
		public SynergyChangeLogSet getParent() {
			return (SynergyChangeLogSet) super.getParent();
		}

		/**
		 * Gets the id of the task.		
		 */
		@Exported
		public String getTaskId() {
			return taskId;
		}
		@Exported
		public void setTaskId(String id) {
			this.taskId = id;
		}

		@Override
		public User getAuthor() {
			if (author == null)
				return User.getUnknown();
			return author;
		}

		@Override
		public Collection<String> getAffectedPaths() {
			return new AbstractList<String>() {
				public String get(int index) {
					return paths.get(index).value;
				}

				public int size() {
					return paths.size();
				}
			};
		}

		public void setUser(String author) {
			this.author = User.get(author);
		}

		@Exported
		public String getUser() {
			return author==null ? null : author.getId();
		}

		@Exported
		public String getDate() {
			return date;
		}

		public void setDate(String date) {
			this.date = date;
		}

		@Override
		@Exported
		public String getMsg() {
			return msg;
		}

		public void setMsg(String msg) {
			this.msg = msg;
		}

		public void addPath(Path p) {
			p.entry = this;
			paths.add(p);
		}

		/**
		 * Gets the files that are changed in this commit.
		 * 
		 * @return can be empty but never null.
		 */
		@Exported
		public List<Path> getPaths() {
			return paths;
		}

		public void setParent(SynergyChangeLogSet parent) {
			super.setParent(parent);
		}
		@Exported
		public int getVersion() {
			return version;
		}
		@Exported
		public void setVersion(int version) {
			this.version = version;
		}
	}

	/**
	 * A file in a commit.
	 * <p>
	 * Setter methods are public only so that the objects can be constructed
	 * from Digester. So please consider this object read-only.
	 */
	@ExportedBean(defaultVisibility = 999)
	public static class Path {
		private LogEntry entry;
		private char action;
		private String id;
		public String getId() {
			return id;
		}

		public void setId(String id) {
			this.id = id;
		}

		private String value;

		/**
		 * Gets the {@link LogEntry} of which this path is a member.
		 */
		public LogEntry getLogEntry() {
			return entry;
		}

		/**
		 * Sets the {@link LogEntry} of which this path is a member.
		 */
		public void setLogEntry(LogEntry entry) {
			this.entry = entry;
		}

		public void setAction(String action) {
			this.action = action.charAt(0);
		}

		/**
		 * Path in the repository. Such as <tt>/test/trunk/foo.c</tt>
		 */
		@Exported(name = "file")
		public String getValue() {
			return value;
		}

		public void setValue(String value) {
			this.value = value;
		}

		@Exported
		public EditType getEditType() {
			if (action == 'A')
				return EditType.ADD;
			if (action == 'D')
				return EditType.DELETE;
			return EditType.EDIT;
		}

	}

}
