package hudson.plugins.synergy;

import hudson.model.Run;
import hudson.scm.ChangeLogSet;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.kohsuke.stapler.export.Exported;
import org.kohsuke.stapler.export.ExportedBean;

public final class SynergyChangeLogSet extends ChangeLogSet<SynergyLogEntry> {

  private final List<SynergyLogEntry> logs;

  /**
   * @GuardedBy this
   */
  private Map<String, Long> revisionMap;

  public SynergyChangeLogSet(Run<?,?> build, List<SynergyLogEntry> logs) {
    super(build, null);

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
    for (SynergyLogEntry log : logs) {
      log.setParent(this);
    }
  }

  @Override
  public boolean isEmptySet() {
    return logs.isEmpty();
  }

  public List<SynergyLogEntry> getLogs() {
    return logs;
  }

  @Override
  public Iterator<SynergyLogEntry> iterator() {
    return logs.iterator();
  }

  public synchronized Map<String, Long> getRevisionMap() throws IOException {
    if (revisionMap == null) {
      revisionMap = SynergySCM.parseRevisionFile(getRun());
    }
    return revisionMap;
  }

  @Exported
  public List<RevisionInfo> getRevisions() throws IOException {
    List<RevisionInfo> r = new ArrayList<RevisionInfo>();
    for (Map.Entry<String, Long> e : getRevisionMap().entrySet()) {
      r.add(new RevisionInfo(e.getKey(), e.getValue()));
    }
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
}
