package hudson.plugins.synergy.impl;

import hudson.plugins.synergy.SynergyLogEntry;
import java.util.Collection;

/**
 * The result of a checkout, made up of a set of logs and conflicts
 */
public class CheckoutResult {

  private Collection<SynergyLogEntry> logs;
  private Collection<Conflict> conflicts;

  public CheckoutResult(Collection<Conflict> conflicts, Collection<SynergyLogEntry> logs) {
    this.conflicts = conflicts;
    this.logs = logs;
  }

  public Collection<SynergyLogEntry> getLogs() {
    return logs;
  }

  public Collection<Conflict> getConflicts() {
    return conflicts;
  }
}
