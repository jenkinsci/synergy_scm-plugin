package hudson.plugins.synergy.impl;

import hudson.plugins.synergy.SynergyChangeLogSet.LogEntry;

import java.util.Collection;

/**
 * The result of a checkout,
 * made up of a set of logs and conflicts
 */
public class CheckoutResult {
	private Collection<LogEntry> logs;
	private Collection<Conflict> conflicts;
	
	public CheckoutResult(Collection<Conflict> conflicts, Collection<LogEntry> logs) {
		this.conflicts = conflicts;
		this.logs = logs;
	}
	public Collection<LogEntry> getLogs() {
		return logs;
	}
	public Collection<Conflict> getConflicts() {
		return conflicts;
	}
}
