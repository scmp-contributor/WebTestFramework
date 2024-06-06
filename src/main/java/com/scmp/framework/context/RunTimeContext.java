package com.scmp.framework.context;

import lombok.Getter;
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
	private final ThreadLocal<HashMap<String, Object>> testLevelVariables = new ThreadLocal<>();
	private final ConcurrentHashMap<String, Object> globalVariables = new ConcurrentHashMap<>();
	private static final Logger frameworkLogger = LoggerFactory.getLogger(RunTimeContext.class);
	private final Environment env;

	@Getter
	private final FrameworkConfigs frameworkConfigs;

	@Autowired
	public RunTimeContext(Environment env, FrameworkConfigs frameworkConfigs) {
		this.env = env;
		this.frameworkConfigs = frameworkConfigs;
	}

	public static String currentDateAndTime() {
		LocalDateTime now = LocalDateTime.now();
		DateTimeFormatter dtf = DateTimeFormatter.ISO_DATE_TIME;
		return now.truncatedTo(ChronoUnit.SECONDS).format(dtf).replace(":", "-");
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
		if (this.testLevelVariables.get()==null) {
			this.testLevelVariables.set(new HashMap<>());
		}

		this.testLevelVariables.get().put(name, data);
	}

	public void clearRunTimeVariables() {
		if (this.testLevelVariables.get()!=null) {
			this.testLevelVariables.get().clear();
		}
	}

	public String getProperty(String name) {
		return this.getProperty(name, "");
	}

	public String getProperty(String key, String defaultValue) {
		return env.getProperty(key, defaultValue);
	}

	public String getURL() {
		String url = this.getFrameworkConfigs().getUrl();
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
		return "ON".equalsIgnoreCase(this.frameworkConfigs.getLocalExecutionMode());
	}

	public ZoneId getZoneId() {
		return ZoneId.of(frameworkConfigs.getZoneId());
	}

	public String getDefaultExtensionPath() {
		if (this.isLocalExecutionMode()) {
			return this.frameworkConfigs.getDefaultLocalExtensionPath();
		} else {
			return this.frameworkConfigs.getDefaultRemoteExtensionPath();
		}
	}
}
