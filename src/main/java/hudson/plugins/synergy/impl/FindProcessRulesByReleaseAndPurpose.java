package hudson.plugins.synergy.impl;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.lang.StringUtils;

/**
 * Finds the projects in a project grouping. ccm query -t project -u -f
 * %objectname "release='AT10.0.0_MAVEN' and status='prep'"
 *
 * @author jrbe
 */
public class FindProcessRulesByReleaseAndPurpose extends Command {

    /**
     * The project release.
     */
    private String release;
    private String purpose;
    

    /**
     * The founded projects.
     */
    private List<String> processRules;

    public FindProcessRulesByReleaseAndPurpose(String p_release, String p_purpose) {
        this.release = p_release;
        this.purpose = p_purpose;
        
    }

    @Override
    public String[] buildCommand(String ccmExe) {
      // ccm query "cvtype='process_rule' and release='AP11.1.0' and name='%003aIntegration Testing'"
        String[] commands = new String[]{ccmExe, "query", "-f", "%objectname", "\"cvtype='process_rule' and release='" + release + "' and name='%003a" + purpose + "'\""};
        return commands;
    }

    @Override
    public void parseResult(String result) {
        if (result != null) {
            processRules = new ArrayList<String>();
            try {
                BufferedReader reader = new BufferedReader(new StringReader(result));
                String sousProjet = reader.readLine();
                while (sousProjet != null) {
                    processRules.add(sousProjet.trim());
                    sousProjet = reader.readLine();
                }
            } catch (IOException e) {
                // Ignore on StringReader.
            }
        }
    }

    public List<String> getProcessRules() {
        return processRules;
    }
}
