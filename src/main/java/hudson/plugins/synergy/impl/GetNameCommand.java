package hudson.plugins.synergy.impl;

/**
 * Get the name of an object.
 */
public class GetNameCommand extends Command {
	private String id;
	private String name;
	
	public GetNameCommand(String id) {
		this.id = id;
	}
	
	@Override
	public String[] buildCommand(String ccmExe) {
		return new String[] { ccmExe, "attr", "-s", "name", id };
	}
	@Override
	public void parseResult(String result) {
		name = result;
	}
	
	public String getName() {
		return name;
	}
}
