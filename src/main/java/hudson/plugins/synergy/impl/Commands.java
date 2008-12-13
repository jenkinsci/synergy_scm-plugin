package hudson.plugins.synergy.impl;

import hudson.FilePath;
import hudson.Launcher;
import hudson.Proc;
import hudson.model.BuildListener;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class Commands implements Serializable {
	/**
	 * Path to ccm executable.
	 */
	private String ccmExe;
	
	/**
	 * Path to UI Log.
	 */
	private String ccmUiLog;
	
	/**
	 * Path to Engine log.
	 */
	private String ccmEngLog;
	
	/**
	 * Launcher.
	 */
	private Launcher launcher;
	
	/**
	 * Address.
	 */
	private String ccmAddr;
	
	/**
	 * Build listener.
	 */
	private BuildListener buildListener;
	
	public BuildListener getBuildListener() {
		return buildListener;
	}


	public void setBuildListener(BuildListener buildListener) {
		this.buildListener = buildListener;
	}


	public String getCcmAddr() {
		return ccmAddr;
	}


	public void setCcmAddr(String ccmAddr) {
		this.ccmAddr = ccmAddr;
	}


	public Launcher getLauncher() {
		return launcher;
	}


	public void setLauncher(Launcher launcher) {
		this.launcher = launcher;
	}


	public String getCcmEngLog() {
		return ccmEngLog;
	}


	public void setCcmEngLog(String ccmEngLog) {
		this.ccmEngLog = ccmEngLog;
	}


	public String getCcmUiLog() {
		return ccmUiLog;
	}


	public void setCcmUiLog(String ccmUiLog) {
		this.ccmUiLog = ccmUiLog;
	}


	public String getCcmExe() {
		return ccmExe;
	}
		

	public void setCcmExe(String ccmExe) {
		this.ccmExe = ccmExe;
	}
	
	/**
	 * Builds a compare project command.
	 */
	public String[] buildCompareProjectCommand(String newProject, String oldProject) {
		String[] query = new String[] {
				getCcmExe(), "query", "\"type!='project' and type!='dir' and is_member_of('" + newProject+ "') and not is_member_of('" + oldProject+ "')\"", 
				"-u",
				"-f",
				"%%objectname"				
		};
		return query;
	}
	
	/**
	 * Executes a Synergy command.
	 * @param path				Current directory
	 * @param command			Command and arguments
	 * @throws IOException
	 * @throws InterruptedException
	 */
	public void executeSynergyCommand(FilePath path, Command command) throws IOException, InterruptedException, SynergyException {
		Map<String, String> system =System.getenv();
		List<String> param = new ArrayList<String>();
		for (Map.Entry<String, String> entry : system.entrySet()) {
			String s = entry.getKey() + "=" + entry.getValue();			
			param.add(s);
		}
		if (ccmAddr!=null) {
			param.add("CCM_ADDR=" + ccmAddr);
		}
		if (ccmUiLog!=null) {
			param.add("CCM_UILOG=" + ccmUiLog);
		}
		if (ccmEngLog!=null) {
			param.add("CCM_ENGLOG=" + ccmEngLog);
		}
		
		String[] env = param.toArray(new String[param.size()]);
		
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		String[] commands = command.buildCommand(ccmExe);
		boolean[] mask = command.buildMask();
		Proc proc = launcher.launch(commands, mask, env, null, out, path);
		int result = proc.join();	
		String output = out.toString();
		
		if (result!=0 && result!=1) {
			buildListener.getLogger().println("ccm command failed");
			buildListener.getLogger().println(output);
			buildListener.getLogger().println("The environment was :");
			for (String s : param) {
				buildListener.getLogger().println(s);
			}
			throw new SynergyException(result);
		} else {
			buildListener.getLogger().println(output);
		}
		
		
		
		if (output!=null && output.endsWith("\r\n")) {
			output = output.substring(0, output.length()-2);
		}
		command.parseResult(output);
	}
	
	/**
	 * Executes a Synergy command.
	 * @param path				Current directory
	 * @param command			Command and arguments
	 * @throws IOException
	 * @throws InterruptedException
	 */
	public void executeSynergyCommand(FilePath path, StreamCommand command) throws IOException, InterruptedException, SynergyException {
		Map<String, String> system =System.getenv();
		List<String> param = new ArrayList<String>();
		for (Map.Entry<String, String> entry : system.entrySet()) {
			String s = entry.getKey() + "=" + entry.getValue();			
			param.add(s);
		}
		if (ccmAddr!=null) {
			param.add("CCM_ADDR=" + ccmAddr);
		}
		if (ccmUiLog!=null) {
			param.add("CCM_UILOG=" + ccmUiLog);
		}
		if (ccmEngLog!=null) {
			param.add("CCM_ENGLOG=" + ccmEngLog);
		}
		
		String[] env = param.toArray(new String[param.size()]);
		
		OutputStream out = command.buildResultOutputer();
		int result;
		try {
			String[] commands = command.buildCommand(ccmExe);
			boolean[] mask = command.buildMask();
			Proc proc = launcher.launch(commands, mask, env, null, out, path);
			result = proc.join();
		} finally {
			out.close();
		}
				
		if (result!=0 && result!=1) {
			buildListener.getLogger().println("ccm command failed");		
			buildListener.getLogger().println("The environment was :");
			for (String s : param) {
				buildListener.getLogger().println(s);
			}
			throw new SynergyException(result);
		} 
	}
}
