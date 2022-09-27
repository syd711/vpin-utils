package de.mephisto.vpin.util;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.io.*;
import java.net.DatagramSocket;
import java.net.ServerSocket;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class SystemInfo {
  private final static Logger LOG = LoggerFactory.getLogger(SystemInfo.class);

  private final static String VPX_REG_KEY = "HKEY_CURRENT_USER\\SOFTWARE\\Visual Pinball\\VP10\\RecentDir";
  private final static String POPPER_REG_KEY = "HKEY_LOCAL_MACHINE\\SYSTEM\\ControlSet001\\Control\\Session Manager\\Environment";
  private final static String VPREG_STG = "VPReg.stg";

  private final static String PINUP_SYSTEM_INSTALLATION_DIR_INST_DIR = "pinupSystem.installationDir";
  private final static String VISUAL_PINBALL_INST_DIR = "visualPinball.installationDir";

  private final static String PINEMHI_FOLDER = "pinemhi";
  private final static String PINEMHI_COMMAND = "PINemHi.exe";
  private final static String PINEMHI_INI = "pinemhi.ini";


  public static final String RESOURCES = "./resources/";

  private File pinUPSystemInstallationFolder;
  private File visualPinballInstallationFolder;

  private File pinemhiNvRamFolder;

  private static SystemInfo instance;

  private SystemInfo() {
    initBaseFolders();
    initPinemHiFolders();
    logSystemInfo();
  }

  private void initBaseFolders() {
    PropertiesStore store = PropertiesStore.create("env");

    this.pinUPSystemInstallationFolder = this.resolvePinUPSystemInstallationFolder();
    if (!store.containsKey(PINUP_SYSTEM_INSTALLATION_DIR_INST_DIR)) {
      store.set(PINUP_SYSTEM_INSTALLATION_DIR_INST_DIR, pinUPSystemInstallationFolder.getAbsolutePath().replaceAll("\\\\", "/"));
    }
    else {
      this.pinUPSystemInstallationFolder = new File(store.get(PINUP_SYSTEM_INSTALLATION_DIR_INST_DIR));
    }

    this.visualPinballInstallationFolder = this.resolveVisualPinballInstallationFolder();
    if (!store.containsKey(VISUAL_PINBALL_INST_DIR)) {
      store.set(VISUAL_PINBALL_INST_DIR, visualPinballInstallationFolder.getAbsolutePath().replaceAll("\\\\", "/"));
    }
    else {
      this.visualPinballInstallationFolder = new File(store.get(VISUAL_PINBALL_INST_DIR));
    }

    getB2SImageExtractionFolder().mkdirs();
  }

  private void initPinemHiFolders() {
    try {
      File file = new File(PINEMHI_FOLDER, PINEMHI_INI);
      if (!file.exists()) {
        throw new FileNotFoundException("pinemhi.ini file (" + file.getAbsolutePath() + ") not found.");
      }

      FileInputStream fileInputStream = new FileInputStream(file);
      java.util.List<String> lines = IOUtils.readLines(fileInputStream, StandardCharsets.UTF_8);
      fileInputStream.close();

      boolean writeUpdates = false;
      List<String> updatedLines = new ArrayList<>();
      for (String line : lines) {
        if (line.startsWith("VP=")) {
          String vpValue = line.split("=")[1];
          pinemhiNvRamFolder = new File(vpValue);
          if (!pinemhiNvRamFolder.exists()) {
            pinemhiNvRamFolder = SystemInfo.getInstance().getNvramFolder();
            line = "VP=" + pinemhiNvRamFolder.getAbsolutePath() + "\\";
            writeUpdates = true;
          }
        }
        updatedLines.add(line);
      }

      if (writeUpdates) {
        FileOutputStream out = new FileOutputStream(file);
        IOUtils.writeLines(updatedLines, "\n", out, StandardCharsets.UTF_8);
        out.close();
        LOG.info("Written updates to " + file.getAbsolutePath());
      }

      LOG.info("Finished pinemhi installation check.");
    } catch (Exception e) {
      String msg = "Failed to run installation for pinemhi: " + e.getMessage();
      LOG.error(msg, e);
    }
  }

  private void logSystemInfo() {
    LOG.info("********************************* Installation Overview ***********************************************");
    LOG.info(formatPathLog("Locale", Locale.getDefault().getDisplayName()));
    LOG.info(formatPathLog("Charset", Charset.defaultCharset().displayName()));
    LOG.info(formatPathLog("PinUP System Folder", this.getPinUPSystemFolder()));
    LOG.info(formatPathLog("PinUP Media Folder", this.getPinUPMediaFolder()));
    LOG.info(formatPathLog("PinUP Database File", this.getPUPDatabaseFile()));
    LOG.info(formatPathLog("Visual Pinball Folder", this.getVisualPinballInstallationFolder()));
    LOG.info(formatPathLog("Visual Pinball Tables Folder", this.getVPXTablesFolder()));
    LOG.info(formatPathLog("Mame Folder", this.getMameFolder()));
    LOG.info(formatPathLog("ROM Folder", this.getMameRomFolder()));
    LOG.info(formatPathLog("NVRam Folder", this.getNvramFolder()));
    LOG.info(formatPathLog("Pinemhi NVRam Folder", this.pinemhiNvRamFolder));
    LOG.info(formatPathLog("Pinemhi Command", this.getPinemhiCommandFile()));
    LOG.info(formatPathLog("Extracted VPReg Folder", this.getExtractedVPRegFolder()));
    LOG.info(formatPathLog("B2S Extraction Folder", this.getB2SImageExtractionFolder()));
    LOG.info(formatPathLog("VPX Files", String.valueOf(this.getVPXTables().length)));
    LOG.info("*******************************************************************************************************");
  }

  private String formatPathLog(String label, String value) {
    return formatPathLog(label, value, null, null);
  }

  private String formatPathLog(String label, File file) {
    return formatPathLog(label, file.getAbsolutePath(), file.exists(), file.canRead());
  }

  public File getB2SImageExtractionFolder() {
    return new File(RESOURCES, "b2s/");
  }

  private String formatPathLog(String label, String value, Boolean exists, Boolean readable) {
    StringBuilder b = new StringBuilder(label);
    b.append(":");
    while (b.length() < 33) {
      b.append(" ");
    }
    b.append(value);

    if (exists != null) {
      while (b.length() < 89) {
        b.append(" ");
      }
      if(!exists) {
        b.append("   [NOT FOUND]");
      }
      else if(!readable){
        b.append("[NOT READABLE]");
      }
      else {
        b.append("          [OK]");
      }
    }
    return b.toString();
  }

  private File resolvePinUPSystemInstallationFolder() {
    try {
      String popperInstDir = System.getenv("PopperInstDir");
      if (!StringUtils.isEmpty(popperInstDir)) {
        return new File(popperInstDir, "PinUPSystem");
      }

      String output = readRegistry(POPPER_REG_KEY, "PopperInstDir");
      if (output != null && output.trim().length() > 0) {
        String path = extractRegistryValue(output);
        File folder = new File(path, "PinUPSystem");
        if (folder.exists()) {
          return folder;
        }
      }
    } catch (Exception e) {
      LOG.error("Failed to read installation folder: " + e.getMessage(), e);
    }
    return new File("C:/vPinball/Visual Pinball");
  }

  private File resolveVisualPinballInstallationFolder() {
    File file = new File(pinUPSystemInstallationFolder.getParent(), "VisualPinball");
    if (!file.exists()) {
      LOG.info("The system info could not derive the Visual Pinball installation folder from the PinUP Popper installation, checking windows registry next.");
      String tablesDir = readRegistry(VPX_REG_KEY, "LoadDir");
      if (tablesDir != null) {
        tablesDir = extractRegistryValue(tablesDir);
        LOG.info("Resolve Visual Pinball tables folder " + tablesDir);
        file = new File(tablesDir);
        if (file.exists()) {
          return file.getParentFile();
        }
      }
    }
    return file;
  }

  public static SystemInfo getInstance() {
    if (instance == null) {
      instance = new SystemInfo();
    }
    return instance;
  }

  public File getPinemhiCommandFile() {
    return new File(PINEMHI_FOLDER, PINEMHI_COMMAND);
  }

  @SuppressWarnings("unused")
  public Dimension getScreenSize() {
    return Toolkit.getDefaultToolkit().getScreenSize();
  }

  public File getVPRegFile() {
    return new File(this.getVisualPinballInstallationFolder() + "/User/", VPREG_STG);
  }

  public File getMameRomFolder() {
    return new File(getVisualPinballInstallationFolder(), "VPinMAME/roms/");
  }

  @SuppressWarnings("unused")
  public File getNvramFolder() {
    return new File(getMameFolder(), "nvram/");
  }

  public File getMameFolder() {
    return new File(getVisualPinballInstallationFolder(), "VPinMAME/");
  }

  public File getExtractedVPRegFolder() {
    return new File("./", "VPReg");
  }

  public File[] getVPXTables() {
    return getVPXTablesFolder().listFiles((dir, name) -> name.endsWith(".vpx"));
  }

  public File getVisualPinballInstallationFolder() {
    return visualPinballInstallationFolder;
  }

  public String get7ZipCommand() {
    return new File(SystemInfo.RESOURCES, "7z.exe").getAbsolutePath();
  }

  public File getVPXTablesFolder() {
    return new File(getVisualPinballInstallationFolder(), "Tables/");
  }

  public File getPinUPSystemFolder() {
    return pinUPSystemInstallationFolder;
  }

  public File getPinUPMediaFolder() {
    return new File(getPinUPSystemFolder(), "POPMedia");
  }

  /**
   * Checks to see if a specific port is available.
   *
   * @param port the port to check for availability
   */
  public static boolean isAvailable(int port) {
    ServerSocket ss = null;
    DatagramSocket ds = null;
    try {
      ss = new ServerSocket(port);
      ss.setReuseAddress(true);
      ds = new DatagramSocket(port);
      ds.setReuseAddress(true);
      return true;
    } catch (IOException e) {
    } finally {
      if (ds != null) {
        ds.close();
      }

      if (ss != null) {
        try {
          ss.close();
        } catch (IOException e) {
          /* should not be thrown */
        }
      }
    }

    return false;
  }

  static String extractRegistryValue(String output) {
    String result = output;
    result = result.replace("\n", "").replace("\r", "").trim();

    String[] s = result.split("    ");
    return s[3];
  }

  static final String readRegistry(String location, String key) {
    try {
      // Run reg query, then read output with StreamReader (internal class)
      String cmd = "reg query " + '"' + location;
      if (key != null) {
        cmd = "reg query " + '"' + location + "\" /v " + key;
      }
      Process process = Runtime.getRuntime().exec(cmd);
      StreamReader reader = new StreamReader(process.getInputStream());
      reader.start();
      process.waitFor();
      reader.join();
      return reader.getResult();
    } catch (Exception e) {
      LOG.error("Failed to read registry key " + location);
      return null;
    }
  }

  public File getPUPDatabaseFile() {
    return new File(getPinUPSystemFolder(), "PUPDatabase.db");
  }

  static class StreamReader extends Thread {
    private InputStream is = null;
    private final StringWriter sw = new StringWriter();

    public StreamReader(InputStream is) {
      this.is = is;
    }

    public void run() {
      try {
        int c;
        while ((c = is.read()) != -1)
          sw.write(c);
      } catch (IOException e) {
        LOG.error("Failed to execute stream reader: " + e.getMessage(), e);
      }
    }

    public String getResult() {
      return sw.toString();
    }
  }
}
