package de.mephisto.vpin.dof;

import de.mephisto.vpin.GameInfo;
import de.mephisto.vpin.util.SystemCommandExecutor;
import de.mephisto.vpin.util.SystemInfo;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.Arrays;
import java.util.List;

public class DOFManager {
  private final static Logger LOG = LoggerFactory.getLogger(DOFManager.class);

  private final static String NO_LED_WIZ = "LedWiz units detected: none";
  private final static String NO_PINSCAPE = "Pinscape units detected: none";

  private boolean boardsFound = false;

  private static DOFManager instance;

  private DOFManager() {
    initialize();
  }

  public static DOFManager create() {
    if(instance == null) {
      instance = new DOFManager();
    }
    return instance;
  }

  public void executeTableLaunchCommands(GameInfo game) {

  }

  public void executeTableExitCommands(GameInfo game) {

  }

  private void initialize() {
    File testerFile = new File(SystemInfo.RESOURCES + "DOFTest/", "DirectOutputTest.exe");
    List<String> commands = Arrays.asList(testerFile.getAbsolutePath());
    try {
      SystemCommandExecutor executor = new SystemCommandExecutor(commands, false);
      executor.setDir(testerFile.getParentFile());
      executor.executeCommand();

      StringBuilder standardOutputFromCommand = executor.getStandardOutputFromCommand();
      StringBuilder standardErrorFromCommand = executor.getStandardErrorFromCommand();
      if (!StringUtils.isEmpty(standardErrorFromCommand.toString())) {
        LOG.error("DOF command '" + String.join(" ", commands) + "' failed: {}", standardErrorFromCommand);
      }
      String output = standardOutputFromCommand.toString();
      validateStatus(output);
    } catch (Exception e) {
      LOG.info("Failed execute DOF command: " + e.getMessage(), e);
    }

    if(!boardsFound) {
      LOG.info("DOFManager initialized, no boards found");
    }
    else {
      LOG.info("DOFManager initialized, found...."); //TODO
    }
  }

  private void validateStatus(String output) {
    this.boardsFound = !output.contains(NO_LED_WIZ) || !output.contains(NO_PINSCAPE);
  }
}
