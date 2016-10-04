package hudson.plugins.synergy;

import hudson.model.Run;
import hudson.plugins.synergy.SynergyLogEntry.Path;
import hudson.scm.ChangeLogParser;
import hudson.scm.ChangeLogSet;
import hudson.scm.RepositoryBrowser;
import hudson.util.Digester2;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import org.apache.commons.digester.Digester;
import org.xml.sax.SAXException;

public class SynergyChangeLogParser extends ChangeLogParser {

  @Override
  public ChangeLogSet<SynergyLogEntry> parse(Run build, RepositoryBrowser<?> browser, File changelogFile)
      throws IOException, SAXException {

    Digester digester = new Digester2();
    ArrayList<SynergyLogEntry> r = new ArrayList<SynergyLogEntry>();
    digester.push(r);

    digester.addObjectCreate("*/logentry", SynergyLogEntry.class);
    digester.addSetProperties("*/logentry");
    digester.addBeanPropertySetter("*/logentry/task", "taskId");
    digester.addBeanPropertySetter("*/logentry/author", "user");
    digester.addBeanPropertySetter("*/logentry/date");
    digester.addBeanPropertySetter("*/logentry/msg");
    digester.addBeanPropertySetter("*/logentry/version", "version");
    digester.addBeanPropertySetter("*/logentry/action", "action");
    digester.addSetNext("*/logentry", "add");

    digester.addObjectCreate("*/logentry/paths/path", Path.class);
    digester.addSetProperties("*/logentry/paths/path");
    digester.addBeanPropertySetter("*/logentry/paths/path", "value");
    digester.addSetNext("*/logentry/paths/path", "addPath");

    try {
      digester.parse(changelogFile);
    } catch (IOException e) {
      throw new IOException("Failed to parse " + changelogFile, e);
    } catch (SAXException e) {
      throw new IOException("Failed to parse " + changelogFile, e);
    }

    SynergyChangeLogSet set = new SynergyChangeLogSet(build, r);
    return set;
  }
}
