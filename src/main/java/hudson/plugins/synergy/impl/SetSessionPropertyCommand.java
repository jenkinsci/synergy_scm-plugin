package hudson.plugins.synergy.impl;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import org.apache.commons.lang.StringUtils;

public class SetSessionPropertyCommand extends Command {

    String key = null;
    String valueToSet = "";
    String value = null;

    public SetSessionPropertyCommand(String p_key, String p_value) {
        this.key = p_key;
        this.valueToSet = p_value;
    }

    /**
     * Builds a set role command.
     */
    @Override
    public String[] buildCommand(String ccmExe) {
        String[] commands;
        if (StringUtils.isEmpty(valueToSet)) {
            commands = new String[]{ccmExe, "set", key};
        } else {
            commands = new String[]{ccmExe, "set", key, valueToSet};
        }
        return commands;
    }

    @Override
    public void parseResult(String result) {
        // Wert auslesen zum späteren zurücksetzen
        // leer muss auf "" gemappt werden

        String line;
        if (StringUtils.isEmpty(result)) {
            value = "\"\"";
        } else {
            try {
                BufferedReader reader = new BufferedReader(new StringReader(result));
                while ((line = reader.readLine()) != null) {
                    value = line;
                    break;
                }
                reader.close();

            } catch (IOException e) {
                // TODO: log parsing problems to hudson logfile
                // Will not happen on a StringReader.
            }
        }
    }

    public String getValue() {
        return value;
    }
}
