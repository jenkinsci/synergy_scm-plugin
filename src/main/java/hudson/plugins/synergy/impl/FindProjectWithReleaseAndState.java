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
public class FindProjectWithReleaseAndState extends Command {

    /**
     * The project release.
     */
    private String release;
    private String status;
    private String projectName;

    /**
     * The founded projects.
     */
    private List<String> projects;

    public FindProjectWithReleaseAndState(String p_release, String p_status, String p_projectName) {
        this.release = p_release;
        this.status = p_status;
        this.projectName = StringUtils.substringBefore(p_projectName, "~");
    }

    @Override
    public String[] buildCommand(String ccmExe) {
        String[] commands = new String[]{ccmExe, "query", "-t", "project", "-u", "-f", "%objectname", "\"(release='" + release + "') and (status='" + status + "') and (name='"+projectName+"')\""};
        return commands;
    }

    @Override
    public void parseResult(String result) {
        if (result != null) {
            projects = new ArrayList<String>();
            try {
                BufferedReader reader = new BufferedReader(new StringReader(result));
                String sousProjet = reader.readLine();
                while (sousProjet != null) {
                    projects.add(sousProjet.trim());
                    sousProjet = reader.readLine();
                }
            } catch (IOException e) {
                // Ignore on StringReader.
            }
        }
    }

    public List<String> getProjects() {
        return projects;
    }
}
