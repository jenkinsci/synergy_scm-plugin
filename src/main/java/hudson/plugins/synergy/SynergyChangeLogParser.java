package hudson.plugins.synergy;

import hudson.model.AbstractBuild;
import hudson.plugins.synergy.SynergyChangeLogSet.LogEntry;
import hudson.plugins.synergy.SynergyChangeLogSet.Path;
import hudson.scm.ChangeLogParser;
import hudson.util.Digester2;
import hudson.util.IOException2;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import org.apache.commons.digester.Digester;
import org.xml.sax.SAXException;

public class SynergyChangeLogParser extends ChangeLogParser {
	@Override
	public SynergyChangeLogSet parse(AbstractBuild build, File changelogFile)
			throws IOException, SAXException {

		Digester digester = new Digester2();
		ArrayList<LogEntry> r = new ArrayList<LogEntry>();
		digester.push(r);

		digester.addObjectCreate("*/logentry", LogEntry.class);
		digester.addSetProperties("*/logentry");
		digester.addBeanPropertySetter("*/logentry/task", "taskId");
		digester.addBeanPropertySetter("*/logentry/author", "user");
		digester.addBeanPropertySetter("*/logentry/date");
		digester.addBeanPropertySetter("*/logentry/msg");
		digester.addSetNext("*/logentry", "add");

		digester.addObjectCreate("*/logentry/paths/path", Path.class);
		digester.addSetProperties("*/logentry/paths/path");
		digester.addBeanPropertySetter("*/logentry/paths/path", "action");
		digester.addBeanPropertySetter("*/logentry/paths/path", "value");
		digester.addSetNext("*/logentry/paths/path", "addPath");

		try {
			digester.parse(changelogFile);
		} catch (IOException e) {
			throw new IOException2("Failed to parse " + changelogFile, e);
		} catch (SAXException e) {
			throw new IOException2("Failed to parse " + changelogFile, e);
		}

		SynergyChangeLogSet set = new SynergyChangeLogSet(build, r);
		return set;
	}
}
