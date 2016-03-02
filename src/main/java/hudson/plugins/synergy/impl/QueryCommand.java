package hudson.plugins.synergy.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class QueryCommand extends Command {

  private final static String FS = "\034";
  private final static String RS = "\036";
  private String query;
  private String sortBy;
  private List<String> attrs;
  private List<Map<String, String>> objs = new ArrayList<Map<String, String>>();

  public QueryCommand(String query, List<String> attrs) {
    this(query, attrs, null);
  }

  public QueryCommand(String query, List<String> attrs, String sortBy) {
    this.query = query;
    this.attrs = attrs;
    if (attrs == null) {
      this.attrs = Arrays.asList(new String[]{"objectname"});
    }
    this.sortBy = sortBy;
  }

  public String[] buildCommand(String ccmExe) {
    StringBuffer format = new StringBuffer();
    for (String attr : attrs) {
      format.append("%" + attr + RS);
    }
    format.setLength(format.length() - 1);
    format.append(FS);
    if (sortBy != null) {
      return new String[]{ccmExe, "query", "-u", "-sby", sortBy, "-nf", "-f", format.toString(), query};
    } else {
      return new String[]{ccmExe, "query", "-u", "-ns", "-nf", "-f", format.toString(), query};
    }
  }

  @Override
  public void parseResult(String result) {
    if (result != null) {
      String[] rows = result.split(FS);
      for (int i = 0; i < rows.length; i++) {
        String[] columns = rows[i].trim().split(RS);
        if (columns.length == attrs.size()) {
          Map<String, String> m = new HashMap<String, String>();
          for (int j = 0; j < columns.length; j++) {
            columns[j] = columns[j].trim();
            if ("<void>".equals(columns[j])) {
              columns[j] = "";
            }
            m.put(attrs.get(j), columns[j]);
          }
          objs.add(m);
        }
      }
    }
  }

  public List<Map<String, String>> getQueryResult() {
    return objs;
  }
}
