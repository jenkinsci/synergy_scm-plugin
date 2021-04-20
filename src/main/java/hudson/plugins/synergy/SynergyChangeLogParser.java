package hudson.plugins.synergy;

import hudson.model.AbstractBuild;
import hudson.plugins.synergy.SynergyChangeLogSet.LogEntry;
import hudson.plugins.synergy.SynergyChangeLogSet.Path;
import hudson.scm.ChangeLogParser;
import hudson.util.IOException2;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import org.apache.commons.digester3.Digester;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;

public class SynergyChangeLogParser extends ChangeLogParser {
	@Override
	public SynergyChangeLogSet parse(AbstractBuild build, File changelogFile)
			throws IOException, SAXException {

		Digester digester = new Digester();

		digester.setXIncludeAware(false);

		if (!Boolean.getBoolean(SynergyChangeLogParser.class.getName() + ".UNSAFE")) {
			try {
				digester.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
				digester.setFeature("http://xml.org/sax/features/external-general-entities", false);
				digester.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
				digester.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
			}
			catch ( ParserConfigurationException ex) {
				throw new SAXException("Failed to securely configure CVS changelog parser", ex);
			}
		}

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
