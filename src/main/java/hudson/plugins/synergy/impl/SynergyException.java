package hudson.plugins.synergy.impl;

public class SynergyException extends Exception {
	private int status;
	
	public SynergyException(int status) {
		this.status = status;
	}

	public int getStatus() {
		return status;
	}

}
