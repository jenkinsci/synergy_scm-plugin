package hudon.plugins.synergy.test.log;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringWriter;

import junit.framework.TestCase;

/**
 * Generic log test case.
 */
public abstract class AbstractLogTest extends TestCase {
	/**
	 * Returns the content of the specified log file.
	 * @param path	The log file path
	 * @return		The content of the log file as a String
	 * @throws IOException
	 */
	public String readLog(String path) throws IOException {
		InputStream in = getClass().getClassLoader().getResourceAsStream(path);
		if (in==null) {
			throw new IllegalArgumentException("resource not found");
		}
		InputStreamReader reader = new InputStreamReader(in);
		StringWriter writer = new StringWriter();
		char[] cbuf = new char[1024];
		int len = reader.read(cbuf);
		while (len!=-1){
			writer.write(cbuf, 0, len);
			len = reader.read(cbuf);
		}
		return writer.toString();
	}
}
