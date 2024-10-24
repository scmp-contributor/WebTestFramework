package com.scmp.framework.context;

import lombok.Getter;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.PropertySource;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.io.File;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;

import static com.scmp.framework.utils.Constants.TARGET_PATH;

@Component
@PropertySource("file:${spring.config.name:config.properties}")
public class RunTimeContext {
	private static final Logger frameworkLogger = LoggerFactory.getLogger(RunTimeContext.class);
	private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ISO_DATE_TIME;

	private final ThreadLocal<HashMap<String, Object>> testLevelVariables = new ThreadLocal<>();
	private final ConcurrentHashMap<String, Object> globalVariables = new ConcurrentHashMap<>();
	private final Environment env;

	@Getter
	private final FrameworkConfigs frameworkConfigs;

	@Autowired
	public RunTimeContext(Environment env, FrameworkConfigs frameworkConfigs) {
		this.env = env;
		this.frameworkConfigs = frameworkConfigs;
	}

	/**
	 * Get current date and time formatted as ISO_DATE_TIME
	 *
	 * @return formatted current date and time
	 */
	public static String currentDateAndTime() {
		LocalDateTime now = LocalDateTime.now();
		return now.truncatedTo(ChronoUnit.SECONDS).format(DATE_TIME_FORMATTER).replace(":", "-");
	}

	/**
	 * Set a global variable
	 *
	 * @param name variable name
	 * @param data variable value
	 */
	public void setGlobalVariables(String name, Object data) {
		this.globalVariables.put(name, data);
	}

	/**
	 * Get a global variable
	 *
	 * @param name variable name
	 * @return variable value
	 */
	public Object getGlobalVariables(String name) {
		return this.globalVariables.get(name);
	}

	/**
	 * Get a test-level variable
	 *
	 * @param name variable name
	 * @return variable value
	 */
	public Object getTestLevelVariables(String name) {
		return this.testLevelVariables.get().getOrDefault(name, null);
	}

	/**
	 * Set a test-level variable
	 *
	 * @param name variable name
	 * @param data variable value
	 */
	public void setTestLevelVariables(String name, Object data) {
		if (this.testLevelVariables.get() == null) {
			this.testLevelVariables.set(new HashMap<>());
		}
		this.testLevelVariables.get().put(name, data);
	}

	/**
	 * Clear all test-level variables
	 */
	public void clearRunTimeVariables() {
		if (this.testLevelVariables.get() != null) {
			this.testLevelVariables.get().clear();
		}
	}

	/**
	 * Get a property value
	 *
	 * @param name property name
	 * @return property value
	 */
	public String getProperty(String name) {
		return this.getProperty(name, "");
	}

	/**
	 * Get a property value with a default
	 *
	 * @param key          property name
	 * @param defaultValue default value
	 * @return property value
	 */
	public String getProperty(String key, String defaultValue) {
		return env.getProperty(key, defaultValue);
	}

	/**
	 * Get the URL from framework configs
	 *
	 * @return URL
	 */
	public String getURL() {
		String url = this.getFrameworkConfigs().getUrl();
		if (url.endsWith("/")) {
			url = url.substring(0, url.length() - 1);
		}
		return url;
	}

	/**
	 * Get the log path for a given category, class, and method
	 *
	 * @param category   log category
	 * @param className  class name
	 * @param methodName method name
	 * @return log path
	 */
	public synchronized String getLogPath(String category, String className, String methodName) {
		String path = buildLogPath(category, className, methodName);
		createDirectoryIfNotExists(path);
		return path;
	}

	/**
	 * Check if local execution mode is enabled
	 *
	 * @return true if local execution mode is ON, false otherwise
	 */
	public boolean isLocalExecutionMode() {
		return "ON".equalsIgnoreCase(this.frameworkConfigs.getLocalExecutionMode());
	}

	/**
	 * Get the zone ID from framework configs
	 *
	 * @return zone ID
	 */
	public ZoneId getZoneId() {
		return ZoneId.of(frameworkConfigs.getZoneId());
	}

	/**
	 * Get the default extension path based on execution mode
	 *
	 * @return default extension path
	 */
	public String getDefaultExtensionPath() {
		if (this.isLocalExecutionMode()) {
			return this.frameworkConfigs.getDefaultLocalExtensionPath();
		} else {
			return this.frameworkConfigs.getDefaultRemoteExtensionPath();
		}
	}

	/**
	 * Build the log path
	 *
	 * @param category   log category
	 * @param className  class name
	 * @param methodName method name
	 * @return log path
	 */
	@NotNull
	@Contract(pure = true)
	private String buildLogPath(String category, String className, String methodName) {
		return TARGET_PATH + File.separator + category + File.separator + className + File.separator + methodName;
	}

	/**
	 * Create directory if it does not exist
	 *
	 * @param path directory path
	 */
	private void createDirectoryIfNotExists(String path) {
		File file = new File(path);
		if (!file.exists() && file.mkdirs()) {
			frameworkLogger.info("Directory [{}] is created!", path);
		} else if (!file.exists()) {
			frameworkLogger.error("Failed to create directory [{}]!", path);
		}
	}
}