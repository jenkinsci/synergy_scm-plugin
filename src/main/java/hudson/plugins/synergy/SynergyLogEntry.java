/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package hudson.plugins.synergy;

import hudson.model.User;
import hudson.scm.ChangeLogSet;
import hudson.scm.EditType;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import org.kohsuke.stapler.export.Exported;
import org.kohsuke.stapler.export.ExportedBean;

/**
 * One commit.
 * <p>
 * Setter methods are public only so that the objects can be constructed from Digester. So please consider this object
 * read-only.
 */
public class SynergyLogEntry extends ChangeLogSet.Entry {

  private String taskId;
  private String version;
  private User author;
  private String date;
  private String msg;
  private List<Path> paths = new ArrayList<Path>();
  private String action;

  /**
   * Gets the {@link SynergyChangeLogSet} to which this change set belongs.
   * @return SynergyChangeLogSet
   */
  @Override
  public SynergyChangeLogSet getParent() {
    return (SynergyChangeLogSet) super.getParent();
  }

  @Override
  public long getTimestamp() {
    try {
      // Fixme: richtiges format ....?
      String l_dateAsString = getDate();
      // z.B. 09.06.16 15:10
      SimpleDateFormat l_format = new SimpleDateFormat("dd.MM.yy hh:mm");
      Date l_parsedDate = l_format.parse(l_dateAsString);
      return l_parsedDate.getTime();
    } catch (ParseException l_ex) {
      // do nothing
    }
    return super.getTimestamp();
  }

  /**
   * Gets the id of the task.
   * @return  TaskId
   */
  @Exported
  public String getTaskId() {
    return taskId;
  }

  @Exported
  public void setTaskId(String id) {
    this.taskId = id;
  }

  @Override
  public User getAuthor() {
    if (author == null) {
      return User.getUnknown();
    }
    return author;
  }

  @Override
  public Collection<String> getAffectedPaths() {
    return new AbstractList<String>() {
      @Override
      public String get(int index) {
        return paths.get(index).value;
      }

      @Override
      public int size() {
        return paths.size();
      }
    };
  }

  public void setUser(String author) {
    if (author == null) {
      this.author = User.getUnknown();
    }
    this.author = User.get(author);
  }

  @Exported
  public String getUser() {
    return author == null ? User.getUnknown().getId() : author.getId();
  }

  @Exported
  public String getDate() {
    return date;
  }

  public void setDate(String date) {
    this.date = date;
  }

  @Override
  @Exported
  public String getMsg() {
    return msg;
  }

  public void setMsg(String msg) {
    this.msg = msg;
  }

  public void addPath(Path p) {
    p.entry = this;
    paths.add(p);
  }

  /**
   * Gets the files that are changed in this commit.
   *
   * @return can be empty but never null.
   */
  @Exported
  public List<Path> getPaths() {
    return paths;
  }

  public void setParent(SynergyChangeLogSet parent) {
    super.setParent(parent);
  }

  @Exported
  public String getVersion() {
    return version;
  }

  @Exported
  public void setVersion(String version) {
    this.version = version;
  }
  
  @Exported
  public EditType getEditType() {
    if (getAction().startsWith("A")) {
        return EditType.ADD;
      }
      if (getAction().startsWith("D")) {
        return EditType.DELETE;
      }
      return EditType.EDIT;
  }

  /**
   * @return the action
   */
  @Exported
  public String getAction() {
    return action != null ? action : "E";
  }
  
  

  /**
   * @param action the action to set
   */
  public void setAction(String action) {
    this.action = action;
  }
  
  @Exported
  public boolean isAddedTask() {
    return EditType.ADD.equals(getEditType());
  }
  
  @Exported
  public boolean isRemovedTask() {
    return EditType.DELETE.equals(getEditType());
  }
  
  
  /**
   * A file in a commit.
   * <p>
   * Setter methods are public only so that the objects can be constructed from Digester. So please consider this object
   * read-only.
   */
  @ExportedBean(defaultVisibility = 999)
  public static class Path {

    private SynergyLogEntry entry;
    private String action;
    private String id;

    public String getId() {
      return id;
    }

    public void setId(String id) {
      this.id = id;
    }

    private String value;

    /**
     * Gets the {@link SynergyLogEntry} of which this path is a member.
     * @return  SynergyLogEntry
     */
    public SynergyLogEntry getLogEntry() {
      return entry;
    }

    /**
     * Sets the {@link SynergyLogEntry} of which this path is a member.
     * @param entry Log Entry
     */
    public void setLogEntry(SynergyLogEntry entry) {
      this.entry = entry;
    }

    /**
     * Path in the repository. Such as <tt>/test/trunk/foo.c</tt>
     * @return path to file
     */
    @Exported(name = "file")
    public String getValue() {
      return value;
    }

    public void setValue(String value) {
      this.value = value;
    }

    @Exported
    public EditType getEditType() {
      if (getAction().startsWith("A")) {
        return EditType.ADD;
      }
      if (getAction().startsWith("D")) {
        return EditType.DELETE;
      }
      return EditType.EDIT;
    }

    /**
     * @return the action
     */
    @Exported
    public String getAction() {
      return action != null ? action : "E";
    }

    /**
     * @param action the action to set
     */
    public void setAction(String action) {
      this.action = action;
    }

  }
}
