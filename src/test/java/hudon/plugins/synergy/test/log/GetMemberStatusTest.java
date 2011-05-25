package hudon.plugins.synergy.test.log;

import java.io.IOException;

import hudson.plugins.synergy.impl.GetMemberStatusCommand;

public class GetMemberStatusTest extends AbstractLogTest {
	
	public void testGetMemberStatusSynergy65() throws IOException{
		GetMemberStatusCommand command=new GetMemberStatusCommand("Integration Testing");
		String content=readLog("logs/projectPurpose/projectPurpose.log1.txt");
		command.parseResult(content);
		String memberStatus=command.getMemberStatus();
		assertEquals("integrate", memberStatus);
	}
	
	public void testGetMemberStatusWithAdditionalNewlineSynergy65() throws IOException{
		GetMemberStatusCommand command=new GetMemberStatusCommand("Integration Testing");
		String content=readLog("logs/projectPurpose/projectPurpose.log1.txt");
		command.parseResult("\r\n"+content);
		String memberStatus=command.getMemberStatus();
		assertEquals("integrate", memberStatus);
	}
}
