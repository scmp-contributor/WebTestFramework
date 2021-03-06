package com.scmp.framework.context;

import java.io.File;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;

import com.scmp.framework.utils.ConfigFileKeys;
import com.scmp.framework.utils.ConfigFileReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.scmp.framework.utils.Constants.TARGET_PATH;

public class RunTimeContext {
  private static RunTimeContext instance;
  private ThreadLocal<HashMap<String, Object>> testLevelVariables = new ThreadLocal<>();
  private ConcurrentHashMap<String, Object> globalVariables = new ConcurrentHashMap<>();
  private ConfigFileReader configFileReader;

  private static final Logger frameworkLogger = LoggerFactory.getLogger(RunTimeContext.class);

  private RunTimeContext() {

    String configFile = "config.properties";
    if (System.getenv().containsKey("CONFIG_FILE")) {
      configFile = System.getenv().get("CONFIG_FILE");
      frameworkLogger.info("Using config file from " + configFile);
    }

    this.configFileReader = new ConfigFileReader(configFile);
  }

  public static synchronized RunTimeContext getInstance() {
    if (instance == null) {
      instance = new RunTimeContext();
    }

    return instance;
  }

  public void setGlobalVariables(String name, Object data) {
    this.globalVariables.put(name, data);
  }

  public Object getGlobalVariables(String name) {
    return this.globalVariables.get(name);
  }

  public Object getTestLevelVariables(String name) {
    return this.testLevelVariables.get().getOrDefault(name, null);
  }

  public void setTestLevelVariables(String name, Object data) {
    if (this.testLevelVariables.get() == null) {
      this.testLevelVariables.set(new HashMap<>());
    }

    this.testLevelVariables.get().put(name, data);
  }

  public void clearRunTimeVariables() {
    if (this.testLevelVariables.get() != null) {
      this.testLevelVariables.get().clear();
    }
  }

  public String getProperty(String name) {
    return this.getProperty(name, null);
  }

  public String getProperty(String key, String defaultValue) {
    String value = System.getenv(key);
    if (value == null || value.isEmpty()) {
      value = configFileReader.getProperty(key, defaultValue);
    }

    return value;
  }

  public String getURL() {
    String url = this.getProperty(ConfigFileKeys.URL, "");
    if (url.endsWith("/")) {
      url = url.substring(0, url.length() - 1);
    }

    return url;
  }

  public synchronized String getLogPath(String category, String className, String methodName) {
    String path =
        TARGET_PATH
            + File.separator
            + category
            + File.separator
            + className
            + File.separator
            + methodName;

    File file = new File(path);
    if (!file.exists()) {
      if (file.mkdirs()) {
        frameworkLogger.info("Directory [" + path + "] is created!");
      } else {
        frameworkLogger.error("Failed to create directory!");
      }
    }

    return path;
  }

  public boolean isLocalExecutionMode() {
    String isDebugMode = this.getProperty(ConfigFileKeys.DEBUG_MODE, "OFF");
    String isLocalExecutionMode = this.getProperty(ConfigFileKeys.LOCAL_EXECUTION, "OFF");

    return "ON".equalsIgnoreCase(isLocalExecutionMode) || "ON".equalsIgnoreCase(isDebugMode);
  }

  public boolean removeFailedTestB4Retry() {
    return "true"
        .equalsIgnoreCase(this.getProperty(ConfigFileKeys.REMOVE_FAILED_TEST_B4_RETRY, "false"));
  }

  public boolean isUploadToTestRail() {
    return "true".equalsIgnoreCase(this.getProperty(ConfigFileKeys.TESTRAIL_UPLOAD_FLAG, "false"));
  }

  public boolean isCreateNewTestRunInTestRail() {
    return "true"
        .equalsIgnoreCase(
            this.getProperty(ConfigFileKeys.TESTRAIL_CREATE_NEW_TEST_RUN, "false"));
  }

  public boolean isIncludeAllAutomatedTestCaseToTestRail() {
    return "true"
            .equalsIgnoreCase(
                    this.getProperty(ConfigFileKeys.TESTRAIL_INCLUDE_ALL_AUTOMATED_TEST_CASES, "false"));
  }

  public ZoneId getZoneId() {
    return ZoneId.of("Asia/Hong_Kong");
  }
}
