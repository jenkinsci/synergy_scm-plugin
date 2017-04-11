package hudson.plugins.synergy;

import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.BuildListener;
import hudson.model.Result;
import hudson.model.TaskListener;
import hudson.plugins.synergy.impl.Command;
import hudson.plugins.synergy.impl.Commands;
import hudson.plugins.synergy.impl.CreateProjectBaselineCommand;
import hudson.plugins.synergy.impl.GetProjectAttributeCommand;
import hudson.plugins.synergy.impl.PublishBaselineCommand;
import hudson.plugins.synergy.impl.QueryCommand;
import hudson.plugins.synergy.impl.SetRoleCommand;
import hudson.plugins.synergy.impl.SynergyException;
import hudson.plugins.synergy.impl.UpdateCommand;
import hudson.plugins.synergy.util.SessionUtils;
import hudson.scm.SCM;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Notifier;
import hudson.tasks.Publisher;
import hudson.util.ListBoxModel;
import java.io.IOException;
import java.io.OutputStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import jxl.Workbook;
import jxl.write.Label;
import jxl.write.WritableCellFormat;
import jxl.write.WritableFont;
import jxl.write.WritableSheet;
import jxl.write.WritableWorkbook;
import jxl.write.WriteException;
import org.kohsuke.stapler.DataBoundConstructor;

public class SynergyPublisher extends Notifier {

  @Extension
  public static class DescriptorImpl extends BuildStepDescriptor<Publisher> {

    public DescriptorImpl() {
      super(SynergyPublisher.class);
    }

    @Override
    public String getDisplayName() {
      return "Create a Synergy Baseline";
    }

    @Override
    public boolean isApplicable(Class<? extends AbstractProject> jobType) {
      return true;
    }

    public ListBoxModel doFillTimeItems() {
      ListBoxModel listBoxModel = new ListBoxModel();
      listBoxModel.add("Before building the project (not working yet)", "before");
      listBoxModel.add("After building the project", "after");
      listBoxModel.add("If the build is successful", "success");
      return listBoxModel;
    }

  }

  public static class SynergyObject {

    private static Pattern p = Pattern.compile("([^~]+)~([^:]+):([^:]+):([^:]+)");
    public String name, version, type, instance, objectname;

    public SynergyObject(String name, String version, String type, String instance) {
      this.name = name;
      this.version = version;
      this.type = type;
      this.instance = instance;
      this.objectname = name + "~" + version + ":" + type + ":" + instance;
    }

    public SynergyObject(String objectname) {
      this.objectname = objectname;
      Matcher m = p.matcher(objectname);
      if (m.matches()) {
        name = m.group(1);
        version = m.group(2);
        type = m.group(3);
        instance = m.group(4);
      }
    }

    @Override
    public String toString() {
      return objectname;
    }
  }

  private static class CopyProcessRuleCommand extends Command {

    private String source, dest;

    public CopyProcessRuleCommand(String source, String dest) {
      this.source = source;
      this.dest = dest;
    }

    @Override
    public String[] buildCommand(String ccmExe) {
      return new String[]{ccmExe, "process_rule", "-copy", source, dest};
    }

    @Override
    public void parseResult(String result) {
    }
  }

  private static class SetBaselineCommand extends Command {

    private String prRule, matching;

    public SetBaselineCommand(String prRule, String matching) {
      this.prRule = prRule;
      this.matching = matching;
    }

    @Override
    public String[] buildCommand(String ccmExe) {
      return new String[]{ccmExe, "process_rule", "-modify", "-matching", matching, prRule};
    }

    @Override
    public void parseResult(String result) {
    }
  }

  private static class GetFolderQueryCommand extends Command {

    private final static Pattern p = Pattern.compile("^\\((.*)\\)$");
    private String folderName, query;

    public GetFolderQueryCommand(String folderName) {
      this.folderName = folderName;
    }

    @Override
    public String[] buildCommand(String ccmExe) {
      return new String[]{ccmExe, "folder_temp", "-show", "query", folderName};
    }

    @Override
    public void parseResult(String result) {
      query = result.replace("Folder Template " + folderName + ": ", "");
      Matcher m = p.matcher(query);
      if (m.matches()) {
        query = m.group(1);
      }
    }

    public String getQuery() {
      return query;
    }
  };

  private static class SetFolderQueryCommand extends Command {

    private String folderName, query, scope;
    private List<String> releases;

    public SetFolderQueryCommand(String folderName, String scope, List<String> releases) {
      this.folderName = folderName;
      this.scope = scope;
      this.releases = releases;
    }

    public SetFolderQueryCommand(String folderName, String query) {
      this.folderName = folderName;
      this.query = query;
    }

    @Override
    public String[] buildCommand(String ccmExe) {
      List<String> cmd = new ArrayList<String>();
      cmd.add(ccmExe);
      cmd.add("folder_temp");
      cmd.add("-modify");
      if (scope != null && releases != null) {
        cmd.add("-scope");
        cmd.add(scope);
        Iterator<String> it = releases.iterator();
        while (it.hasNext()) {
          cmd.add("-release");
          cmd.add(it.next());
        }
      } else {
        cmd.add("-custom");
        cmd.add(query);
      }
      cmd.add(folderName);
      return cmd.toArray(new String[0]);
    }

    @Override
    public void parseResult(String result) {
    }
  };

  private static class CreateReleaseCommand extends Command {

    private String release, from, baseline;

    public CreateReleaseCommand(String release, String from, String baseline) {
      this.release = release;
      this.from = from;
      this.baseline = baseline;
    }

    @Override
    public String[] buildCommand(String ccmExe) {
      Vector<String> commands = new Vector<String>(Arrays.asList(new String[]{ccmExe, "release", "-create"}));
      if (from != null) {
        commands.add("-from");
        commands.add(from);
      }
      if (baseline != null) {
        commands.add("-baseline");
        commands.add(baseline);
      }
      commands.add(release);
      return commands.toArray(new String[]{});
    }

    @Override
    public void parseResult(String result) {
    }
  };

  private static class ActivateReleaseCommand extends Command {

    private String release;

    public ActivateReleaseCommand(String release) {
      this.release = release;
    }

    @Override
    public String[] buildCommand(String ccmExe) {
      return new String[]{ccmExe, "release", "-modify", "-active", release};
    }

    @Override
    public void parseResult(String result) {
    }
  };

  private static class InactivateReleaseCommand extends Command {

    private String release;

    public InactivateReleaseCommand(String release) {
      this.release = release;
    }

    @Override
    public String[] buildCommand(String ccmExe) {
      return new String[]{ccmExe, "release", "-modify", "-inactive", release};
    }

    @Override
    public void parseResult(String result) {
    }
  };

  private static class CreateTaskCommand extends Command {

    private String release, synopsis, taskNumber;

    public CreateTaskCommand(String release, String synopsis) {
      this.release = release;
      this.synopsis = synopsis;
    }

    @Override
    public String[] buildCommand(String ccmExe) {
      return new String[]{ccmExe, "task", "-create", "-resolver", System.getProperty("user.name"), "-release", release, "-synopsis", synopsis, "-default"};
    }

    @Override
    public void parseResult(String result) {
      Matcher m = Pattern.compile("Task (.*) created").matcher(result);
      if (m.find()) {
        taskNumber = m.group(1);
      }
    }

    public String getTaskNumber() {
      return taskNumber;
    }
  };

  private static class RelateTaskCommand extends Command {

    private String taskNumber, project;

    public RelateTaskCommand(String taskNumber, String project) {
      this.taskNumber = taskNumber;
      this.project = project;
    }

    @Override
    public String[] buildCommand(String ccmExe) {
      return new String[]{ccmExe, "task", "-relate", taskNumber, "-object", project, "@"};
    }

    @Override
    public void parseResult(String result) {
    }
  };

  private static class CheckinTaskCommand extends Command {

    @Override
    public String[] buildCommand(String ccmExe) {
      return new String[]{ccmExe, "task", "-checkin", "default"};
    }

    @Override
    public void parseResult(String result) {
    }
  };

  private static class ExcludeTaskCommand extends Command {

    private String taskNumber;

    public ExcludeTaskCommand(String taskNumber) {
      this.taskNumber = taskNumber;
    }

    @Override
    public String[] buildCommand(String ccmExe) {
      return new String[]{ccmExe, "task", "-state", "excluded", taskNumber};
    }

    @Override
    public void parseResult(String result) {
    }
  };

  private static class CopyProjectCommand extends Command {

    private String fromProject, toVersion, purpose, release;

    public CopyProjectCommand(String fromProject, String toVersion, String purpose, String release) {
      this.fromProject = fromProject;
      this.toVersion = toVersion;
      this.purpose = purpose;
      this.release = release;
    }

    @Override
    public String[] buildCommand(String ccmExe) {
      Vector<String> commands = new Vector<String>(Arrays.asList(new String[]{ccmExe, "copy_project", "-subprojects", "-to", toVersion, "-no_wa"}));
      if (release != null && release.length() > 0) {
        commands.add("-release");
        commands.add(release);
      }
      if (purpose != null && purpose.length() > 0) {
        commands.add("-purpose");
        commands.add(purpose);
      }
      commands.add(fromProject);
      return commands.toArray(new String[]{});
    }

    @Override
    public void parseResult(String result) {
    }
  };

  private static class CatCommand extends Command {

    private String objectName, source;

    public CatCommand(String objectName) {
      this.objectName = objectName;
    }

    @Override
    public String[] buildCommand(String ccmExe) {
      return new String[]{ccmExe, "cat", objectName};
    }

    @Override
    public void parseResult(String result) {
      source = result;
    }

    public String getSource() {
      return source;
    }
  };

  private static class GetReleaseBaselineCommand extends Command {

    private String release, baselineRelease;

    public GetReleaseBaselineCommand(String release) {
      this.release = release;
    }

    @Override
    public String[] buildCommand(String ccmExe) {
      return new String[]{ccmExe, "release", "-show", "information", release};
    }

    @Override
    public void parseResult(String result) {
      Matcher m = Pattern.compile("Baseline:\\s*(\\S+)").matcher(result);
      if (m.find()) {
        baselineRelease = m.group(1);
      }
    }

    public String getBaselineRelease() {
      return baselineRelease;
    }
  };

  private static Pattern patternBugfix = Pattern.compile("('aBT\\d+\\.\\d+\\.\\d+(?:_CR_.*?){0,1}(?:_PRE){0,1}')");
  private static Pattern patternHotfix = Pattern.compile("('aBT\\d+\\.\\d+\\.\\d+\\.\\d+(?:_CR_.*?){0,1}(?:_PRE){0,1}')");
  private static Pattern patternBaseline = Pattern.compile("(BT\\d+(?:\\.\\d+){2,3})\\.(\\d+)((?:_CR_.*?){0,1}(?:_PRE){0,1})");
  private static Pattern patternRelease = Pattern.compile("(BT\\d+(?:\\.\\d+){2,3})((?:_CR_.*?){0,1}(?:_PRE){0,1})");

  /**
   * The moment the baseline should be created (after/before/sucess)
   */
  private String time;

  /**
   * Should the baseline be published
   */
  private boolean publish;
  /**
   * Name of baseline
   */
  private String baselineName;

  /**
   * Version template to use
   */
  private String baselineTemplate;

  /**
   * Create delivery task with all changed objects
   */
  private String phoenicsRelease;

  @DataBoundConstructor
  public SynergyPublisher(String time, boolean publish, String baselineName, String baselineTemplate, String phoenicsRelease) {
    this.time = time;
    this.publish = publish;
    this.baselineName = baselineName;
    this.baselineTemplate = baselineTemplate;
    this.phoenicsRelease = phoenicsRelease;
  }

  @Override
  public BuildStepMonitor getRequiredMonitorService() {
    return BuildStepMonitor.STEP;
  }

  private void createRelease(Commands commands, FilePath path, String release, String from, String baseline) throws IOException, InterruptedException, SynergyException {
    QueryCommand releaseQueryCommand = new QueryCommand("cvtype='releasedef' and component_release='" + release + "'", Arrays.asList(new String[]{"objectname", "active"}));
    commands.executeSynergyCommand(path, releaseQueryCommand);
    final List<Map<String, String>> releases = releaseQueryCommand.getQueryResult();
    if (releases.isEmpty()) {
      CreateReleaseCommand createReleaseCommand = new CreateReleaseCommand(release, from, baseline);
      commands.executeSynergyCommand(path, createReleaseCommand);
    } else if (!releases.isEmpty() && releases.get(0).get("active").equals("FALSE")) {
      ActivateReleaseCommand activateReleaseCommand = new ActivateReleaseCommand(release);
      commands.executeSynergyCommand(path, activateReleaseCommand);
    }
  }

  @Override
  public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener) throws InterruptedException, IOException {
    // Check SCM used.
    SCM scm = build.getParent().getScm();
    if (!(scm instanceof SynergySCM)) {
      listener.getLogger().println("No baseline publishing for non Synergy project");
      return false;
    }

    // Check what needs to be done.
    boolean createBaseline = false;
    boolean publishBaseline = false;
    boolean buildSucess = Result.SUCCESS.equals(build.getResult());

    String createBaselineAtPoint = getTime();

    if ("after".equals(createBaselineAtPoint)) {
      // Always create baseline after build.
      createBaseline = true;
    } else if ("success".equals(createBaselineAtPoint) && buildSucess) {
      // Create baseline only after sucessful build.
      createBaseline = true;
    }
    if (publish && buildSucess) {
      // Publish baseline if build is sucessful.
      publishBaseline = true;
    }

    // Check if we need to go on.
    if (createBaseline || publishBaseline) {
      // Get Synergy parameters.
      SynergySCM synergySCM = (SynergySCM) scm;
      String project = computeDynamicValue(build, synergySCM.getProject()).trim();
      String purpose = synergySCM.getPurpose().trim();
      String release = synergySCM.getRelease().trim();
      String l_baselineName = computeDynamicValue(build, this.baselineName).trim();
      String l_phoenicsRelease = computeDynamicValue(build, this.phoenicsRelease).trim();
      FilePath path = build.getWorkspace();

      Commands commands = null;
      boolean bugfix = Pattern.matches("BT\\d+\\.\\d+\\.\\d+(?:_CR_.*?){0,1}(?:_PRE){0,1}", release);
      boolean pre = Pattern.matches(".*_PRE", l_phoenicsRelease);

      try {
        // Open Session.
        commands = SessionUtils.openSession(path, synergySCM, listener, launcher);

        // Become build manager.
        SetRoleCommand setRoleCommand = new SetRoleCommand(SetRoleCommand.BUILD_MANAGER);
        commands.executeSynergyCommand(path, setRoleCommand);

        if (l_phoenicsRelease.length() > 0) {
          // Release aus Projekt bestimmen
          if (release.length() == 0) {
            GetProjectAttributeCommand getProjectAttributeCommand = new GetProjectAttributeCommand(project, "release");
            commands.executeSynergyCommand(path, getProjectAttributeCommand);
            release = getProjectAttributeCommand.getValue();
          }

          // neue BT Lieferung anhand der letzten Baseline bestimmen
          // falls keine letzte Baseline vorhanden ist, wird release + ".1" verwendet 
          if (l_baselineName.length() == 0) {
            QueryCommand baselineQueryCommand = new QueryCommand("cvtype='baseline' and release='" + release + "' and (status='published_baseline' or status='test_baseline')", Arrays.asList(new String[]{"name", "objectname"}), "-create_time");
            commands.executeSynergyCommand(path, baselineQueryCommand);
            if (baselineQueryCommand.getQueryResult().size() > 0) {
              Matcher m = patternBaseline.matcher(baselineQueryCommand.getQueryResult().get(0).get("name"));
              if (m.matches()) {
                l_baselineName = m.group(1) + "." + (Integer.parseInt(m.group(2)) + 1) + m.group(3);
              }
            } else {
              Matcher m = patternRelease.matcher(release);
              if (m.matches()) {
                l_baselineName = m.group(1) + ".1" + m.group(2);
              }
            }
          }

          // btversion.txt ueberpruefen
          String btversion = null;
          QueryCommand btversionQueryCommand = new QueryCommand("name='btversion.txt' and recursive_is_member_of('" + project + "', '')", Arrays.asList(new String[]{"objectname"}));
          commands.executeSynergyCommand(path, btversionQueryCommand);
          if (btversionQueryCommand.getQueryResult().size() == 1) {
            CatCommand catCommand = new CatCommand(btversionQueryCommand.getQueryResult().get(0).get("objectname"));
            commands.executeSynergyCommand(path, catCommand);
            btversion = catCommand.getSource().trim();
          }
          if (!l_baselineName.equals(btversion)) {
            listener.getLogger().println("BT Version (" + btversion + ") passt nicht zur Auslieferung (" + l_baselineName + ").");
            return false;
          }
        }

        // Compute baseline name.
        if (l_baselineName.length() == 0) {
          Date date = build.getTimestamp().getTime();
          DateFormat format = new SimpleDateFormat("yyyyMMdd-hhmm");
          l_baselineName = build.getProject().getName() + "-" + format.format(date);
        }

        // Create baseline.
        if (createBaseline) {
          CreateProjectBaselineCommand createCommand = new CreateProjectBaselineCommand(l_baselineName, baselineTemplate, project, release, purpose);
          commands.executeSynergyCommand(path, createCommand);
          // zusaetzlich Baseline fuer Hotfix Release erstellen
          if (l_phoenicsRelease.length() > 0 && bugfix) {
            SynergyObject projectObject = new SynergyObject(project);
            String newProjectVersion = projectObject.version.indexOf(release) != -1 ? projectObject.version.replace(release, l_baselineName) : l_baselineName + "_Delivery";
            SynergyObject newProjectObject = new SynergyObject(projectObject.name, newProjectVersion, projectObject.type, projectObject.instance);
            SynergyObject baselineObject = new SynergyObject(projectObject.name, l_baselineName, projectObject.type, projectObject.instance);
            CopyProjectCommand copyProjectCommand = new CopyProjectCommand(baselineObject.objectname, newProjectObject.version, purpose, l_baselineName);
            commands.executeSynergyCommand(path, copyProjectCommand);
            UpdateCommand updateCommand = new UpdateCommand(UpdateCommand.PROJECT, newProjectObject.objectname, false);
            commands.executeSynergyCommand(path, updateCommand);
            // Neue Baseline fuer Hotfix Release erstellen
            Matcher m = patternBaseline.matcher(l_baselineName);
            if (m.matches()) {
              createCommand = new CreateProjectBaselineCommand(m.group(1) + "." + m.group(2) + ".0" + m.group(3), baselineTemplate, newProjectObject.objectname, l_baselineName, purpose);
              commands.executeSynergyCommand(path, createCommand);
            }
          }

          // Publish baseline.
          if (publishBaseline) {
            PublishBaselineCommand publishCommand = new PublishBaselineCommand(l_baselineName);
            commands.executeSynergyCommand(path, publishCommand);
            if (l_phoenicsRelease.length() > 0 && bugfix) {
              Matcher m = patternBaseline.matcher(l_baselineName);
              if (m.matches()) {
                publishCommand = new PublishBaselineCommand(m.group(1) + "." + m.group(2) + ".0" + m.group(3));
                commands.executeSynergyCommand(path, publishCommand);
              }
            }
          }

          // create Phoenics Delivery
          if (l_phoenicsRelease.length() > 0) {
            // Art der Auslieferung bestimmen
            GetFolderQueryCommand getFolderQueryCommand = new GetFolderQueryCommand(l_phoenicsRelease + " Quellfolder");
            commands.executeSynergyCommand(path, getFolderQueryCommand);
            boolean hc = Pattern.compile("%HC%").matcher(getFolderQueryCommand.getQuery()).find();
            boolean qf = Pattern.compile("%QF%").matcher(getFolderQueryCommand.getQuery()).find();
            boolean exclude = false;

            // PrÃ¼fen, ob noch anderer Bugfix in der PRE ist
            if (pre) {
              Pattern p = Pattern.compile("(BT\\d+\\.\\d+\\.\\d+)(\\.\\d+)");
              Matcher m = p.matcher(l_baselineName);
              if (m.find()) {
                QueryCommand preQueryCommand = new QueryCommand("cvtype='task' and status='completed' and release match 'a" + m.group(1) + "*_PRE' and task_synopsis match 'Bugfix Lieferung*'", Arrays.asList(new String[]{"task_synopsis"}));
                commands.executeSynergyCommand(path, preQueryCommand);
                if (preQueryCommand.getQueryResult().size() > 0) {
                  exclude = true;
                  for (int i = 0; i < preQueryCommand.getQueryResult().size(); i++) {
                    if (preQueryCommand.getQueryResult().get(i).get("task_synopsis").equals("Bugfix Lieferung " + m.group(1) + m.group(2))) {
                      exclude = false;
                      break;
                    }
                  }
                }
              }
            }

            // aktuelle und vorherige Baseline bestimmen
            Map<String, String> baseline = new HashMap<>();
            Map<String, String> oldBaseline = null;
            QueryCommand baselineQueryCommand = new QueryCommand("cvtype='baseline' and release='" + release + "' and (status='published_baseline' or status='test_baseline')", Arrays.asList(new String[]{"name", "objectname"}), "-create_time");
            commands.executeSynergyCommand(path, baselineQueryCommand);
            if (baselineQueryCommand.getQueryResult().size() > 0) {
              baseline = baselineQueryCommand.getQueryResult().get(0);
            }
            if (baselineQueryCommand.getQueryResult().size() > 1) {
              oldBaseline = baselineQueryCommand.getQueryResult().get(1);
            } else {
              GetReleaseBaselineCommand getReleaseBaselineCommand = new GetReleaseBaselineCommand(release);
              commands.executeSynergyCommand(path, getReleaseBaselineCommand);
              if (getReleaseBaselineCommand.getBaselineRelease() != null) {
                baselineQueryCommand = new QueryCommand("cvtype='baseline' and release='" + getReleaseBaselineCommand.getBaselineRelease() + "' and (status='published_baseline' or status='test_baseline') and name match '" + getReleaseBaselineCommand.getBaselineRelease() + ".*'", Arrays.asList(new String[]{"name", "objectname"}), "-create_time");
                commands.executeSynergyCommand(path, baselineQueryCommand);
                if (baselineQueryCommand.getQueryResult().size() > 0) {
                  oldBaseline = baselineQueryCommand.getQueryResult().get(0);
                }
              }
            }

            // aBT Release anlegen und aktivieren
            String deliveryRelease = "a" + release + (pre ? "_PRE" : "");
            createRelease(commands, path, deliveryRelease, null, null);

            // Auslieferungstask anlegen
            String type;
            if (qf) {
              type = "QS-Fix";
            } else if (bugfix) {
              type = hc ? "HC" : "Bugfix";
            } else {
              type = hc ? "EC" : "Hotfix";
            }
            CreateTaskCommand createTaskCommand = new CreateTaskCommand(deliveryRelease, type + " Lieferung " + l_baselineName);
            commands.executeSynergyCommand(path, createTaskCommand);
            String task = createTaskCommand.getTaskNumber();

            if (pre) {
              listener.getLogger().println("PREINTEGRATION" + (exclude ? " (excluded): " : ": "));
            }
            listener.getLogger().println("Release: " + release);
            listener.getLogger().println("Typ: " + type);
            listener.getLogger().println("BT-Lieferung: " + l_baselineName);
            listener.getLogger().println("Auslieferungstask: " + task);

            // alle neuen Objekte an die Auslieferungstask haengen
            String query = "is_project_in_baseline_of('" + baseline.get("objectname") + "') and name='aus_BT'";
            query = "(" + query + ") or (cvtype='project' and name!='Doktyp-Printserver' and name!='Doktyp-RTF' and is_member_of(" + query + "))";
            QueryCommand recursiveProjectQueryCommand = new QueryCommand(query, Arrays.asList(new String[]{"objectname", "name", "displayname"}));
            commands.executeSynergyCommand(path, recursiveProjectQueryCommand);
            for (Map<String, String> subproject : recursiveProjectQueryCommand.getQueryResult()) {
              query = "type != 'project' and is_member_of('" + subproject.get("objectname") + "')";
              if (oldBaseline != null) {
                query += " and not is_member_of(is_project_in_baseline_of('" + oldBaseline.get("objectname") + "') and name='" + subproject.get("name") + "')";
              }
              QueryCommand compareProjectCommand = new QueryCommand(query, Arrays.asList(new String[]{"objectname"}));
              commands.executeSynergyCommand(path, compareProjectCommand);
              if (compareProjectCommand.getQueryResult().size() > 0) {
                RelateTaskCommand relateTaskCommand = new RelateTaskCommand(task, subproject.get("objectname"));
                commands.executeSynergyCommand(path, relateTaskCommand);
                listener.getLogger().println("Projekt: " + subproject.get("displayname"));
              }
            }

            // Task einchecken und aBT Release deaktivieren
            CheckinTaskCommand checkinTaskCommand = new CheckinTaskCommand();
            commands.executeSynergyCommand(path, checkinTaskCommand);
            InactivateReleaseCommand inactivateReleaseCommand = new InactivateReleaseCommand(deliveryRelease);
            commands.executeSynergyCommand(path, inactivateReleaseCommand);
            // Task excluden, falls sich der vorherige Bugfix noch in der PRE befindet
            if (exclude) {
              Command excludeTaskCommand = new ExcludeTaskCommand(task);
              commands.executeSynergyCommand(path, excludeTaskCommand);
            }

            if (bugfix) {
              // Hotfix Release fÃ¼r nÃ¤chste BF Lieferung anlegen
              Matcher m = patternBaseline.matcher(l_baselineName);
              if (!m.matches()) {
                listener.error("Hotfix Release fÃ¼r nÃ¤chste BF Lieferung kann nicht bestimmt werden");
                return false;
              }
              int baselineNumber = Integer.parseInt(m.group(2));
              String previousBaselineName = baselineNumber > 0 ? m.group(1) + "." + (baselineNumber - 1) + m.group(3) : null;
              String nextBaselineName = m.group(1) + "." + (baselineNumber + 1) + m.group(3);
              createRelease(commands, path, nextBaselineName, previousBaselineName, release);
              // Process Rules kopieren
              for (String purpose2 : Arrays.asList(new String[]{"Collaborative Development", "Integration Testing"})) {
                CopyProcessRuleCommand copyProcessRuleCommand = new CopyProcessRuleCommand(l_baselineName + ":" + purpose2, nextBaselineName + ":" + purpose2);
                commands.executeSynergyCommand(path, copyProcessRuleCommand);
                if (previousBaselineName != null) {
                  copyProcessRuleCommand = new CopyProcessRuleCommand(previousBaselineName + ":" + purpose2, l_baselineName + ":" + purpose2);
                  commands.executeSynergyCommand(path, copyProcessRuleCommand);
                }
              }

              // Folder Queries aktualisieren
              if (!exclude) {
                List<String> releases = new ArrayList<String>();
                releases.add(deliveryRelease);
                if (pre) {
                  releases.add(deliveryRelease.replace("_PRE", ""));
                  releases.add("a" + l_baselineName + "_PRE");
                } else {
                  releases.add("a" + l_baselineName);
                }
                SetFolderQueryCommand setFolderQueryCommand = new SetFolderQueryCommand("Completed (and included) tasks for release " + deliveryRelease, "all_completed", releases);
                commands.executeSynergyCommand(path, setFolderQueryCommand);
              }
            }

            // Taskliste erstellen
            FilePath taskListeFile = new FilePath(path, "Taskliste.xls");
            OutputStream xlsFile = taskListeFile.write();
            WritableWorkbook workbook = Workbook.createWorkbook(xlsFile);
            WritableSheet sheet1 = workbook.createSheet("Tasks", 0);
            WritableCellFormat headingformat = new WritableCellFormat(new WritableFont(WritableFont.ARIAL, 10, WritableFont.BOLD));
            WritableCellFormat wrapFormat = new WritableCellFormat(new WritableFont(WritableFont.ARIAL));
            wrapFormat.setWrap(true);

            Label label = new Label(0, 0, "Liste der neuen Tasks fÃ¼r " + l_baselineName, headingformat);
            sheet1.addCell(label);
            label = new Label(0, 2, "BT-Auslieferung", headingformat);
            sheet1.addCell(label);
            label = new Label(1, 2, l_baselineName);
            sheet1.addCell(label);
            label = new Label(0, 3, "Differenz zu", headingformat);
            sheet1.addCell(label);
            label = new Label(1, 3, oldBaseline != null ? oldBaseline.get("name") : "");
            sheet1.addCell(label);
            label = new Label(0, 4, "Auslieferungstask", headingformat);
            sheet1.addCell(label);
            label = new Label(1, 4, task.toString());
            sheet1.addCell(label);

            int row = 6;
            int col = 0;
            label = new Label(col++, row, "Task Number", headingformat);
            sheet1.addCell(label);
            label = new Label(col++, row, "Status", headingformat);
            sheet1.addCell(label);
            label = new Label(col++, row, "Resolver", headingformat);
            sheet1.addCell(label);
            label = new Label(col++, row, "Release", headingformat);
            sheet1.addCell(label);
            label = new Label(col++, row, "Task Synopsis", headingformat);
            sheet1.addCell(label);
            label = new Label(col++, row, "Task Description", headingformat);
            sheet1.addCell(label);
            row++;

            String taskQuery = "is_task_in_baseline_of('" + baseline.get("objectname") + "') or is_dirty_task_in_baseline_of('" + baseline.get("objectname") + "')";
            if (oldBaseline != null) {
              taskQuery = "(" + taskQuery + ") and not (is_task_in_baseline_of('" + oldBaseline.get("objectname") + "') or is_dirty_task_in_baseline_of('" + oldBaseline.get("objectname") + "') or is_task_in_folder_of(cvtype='folder' and description='" + oldBaseline.get("name") + "'))";
            }
            QueryCommand taskQueryCommand = new QueryCommand(taskQuery, Arrays.asList(new String[]{"task_number", "task_synopsis", "task_description", "resolver", "status", "release"}));
            commands.executeSynergyCommand(path, taskQueryCommand);
            for (final Map<String, String> t : taskQueryCommand.getQueryResult()) {
              col = 0;
              label = new Label(col++, row, t.get("task_number"));
              sheet1.addCell(label);
              label = new Label(col++, row, t.get("status"));
              sheet1.addCell(label);
              label = new Label(col++, row, t.get("resolver"));
              sheet1.addCell(label);
              label = new Label(col++, row, t.get("release"));
              sheet1.addCell(label);
              label = new Label(col++, row, t.get("task_synopsis"));
              sheet1.addCell(label);
              label = new Label(col++, row, t.get("task_description"));
              sheet1.addCell(label);
              row++;
            }

            WritableSheet sheet2 = workbook.createSheet("Objekte", 1);

            label = new Label(0, 0, "Liste der geÃ¤nderten Objekte:", headingformat);
            sheet2.addCell(label);
            row = 2;
            col = 0;
            label = new Label(col++, row, "Name", headingformat);
            sheet2.addCell(label);
            label = new Label(col++, row, "Version", headingformat);
            sheet2.addCell(label);
            label = new Label(col++, row, "Release", headingformat);
            sheet2.addCell(label);
            row++;

            String objectQuery = "not name='pom.xml' and is_associated_cv_of(" + taskQuery + ")";
            QueryCommand objectQueryCommand = new QueryCommand(objectQuery, Arrays.asList(new String[]{"name", "version", "release"}));
            commands.executeSynergyCommand(path, objectQueryCommand);
            for (final Map<String, String> o : objectQueryCommand.getQueryResult()) {
              col = 0;
              label = new Label(col++, row, o.get("name"));
              sheet2.addCell(label);
              label = new Label(col++, row, o.get("version"));
              sheet2.addCell(label);
              label = new Label(col++, row, o.get("release"));
              sheet2.addCell(label);
              row++;
            }

            workbook.write();
            workbook.close();
            xlsFile.close();
          }
        }
      } catch (SynergyException e) {
        return false;
      } catch (WriteException e) {
        return false;
      } finally {
        // Stop Synergy.
        try {
          SessionUtils.closeSession(path, commands, synergySCM.isLeaveSessionOpen());
        } catch (SynergyException e) {
          return false;
        }
      }
    }
    return true;
  }

  /**
   * Replace an expression in the form ${name} in the given String by the value of the matching environment variable.
   */
  private String computeDynamicValue(AbstractBuild build, String parameterizedValue) throws IllegalStateException, InterruptedException, IOException {
    if (parameterizedValue != null && parameterizedValue.indexOf("${") != -1) {
      int start = parameterizedValue.indexOf("${");
      int end = parameterizedValue.indexOf("}", start);
      String parameter = parameterizedValue.substring(start + 2, end);
      String value = (String) build.getEnvironment(TaskListener.NULL).get(parameter);
      if (value == null) {
        throw new IllegalStateException(parameter);
      }
      return parameterizedValue.substring(0, start) + value + (parameterizedValue.length() > end + 1 ? parameterizedValue.substring(end + 1) : "");
    } else {
      return parameterizedValue;
    }
  }

  public void setPublish(boolean publish) {
    this.publish = publish;
  }

  public boolean isPublish() {
    return publish;
  }

  public String getTime() {
    return time;
  }

  public void setTime(String time) {
    this.time = time;
  }

  public String getBaselineName() {
    return baselineName;
  }

  public void setBaselineName(String baselineName) {
    this.baselineName = baselineName;
  }

  public String getBaselineTemplate() {
    return baselineTemplate;
  }

  public void setBaselineTemplate(String baselineTemplate) {
    this.baselineTemplate = baselineTemplate;
  }

  public void setPhoenicsRelease(String phoenicsRelease) {
    this.phoenicsRelease = phoenicsRelease;
  }

  public String getPhoenicsRelease() {
    return phoenicsRelease;
  }
}
