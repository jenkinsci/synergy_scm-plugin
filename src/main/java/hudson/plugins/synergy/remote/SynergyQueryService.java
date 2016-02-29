package hudson.plugins.synergy.remote;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.sql.SQLException;
import java.util.Map;

public interface SynergyQueryService extends Remote {
	public Map<String, String> getProjectMembers(String project, boolean subprojects) throws RemoteException;

}
