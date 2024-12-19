package com.scmp.framework.context;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.PropertySource;
import org.springframework.core.env.Environment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Collections;

@Component
@PropertySource("file:${spring.config.name:config.properties}")
@Getter
public class FrameworkConfigs {

	@Autowired
	public FrameworkConfigs(ConfigurableApplicationContext context, Environment env) {
		String zoneId = env.getProperty("ZONE_ID", "Asia/Hong_Kong");
		String today = LocalDate.now(ZoneId.of(zoneId)).format(DateTimeFormatter.ofPattern("M/dd/yyy"));
		context.getEnvironment().getPropertySources()
				.addLast(new MapPropertySource("runtimeProperties", Collections.singletonMap("date", today)));
	}

	@Value("${ZONE_ID:Asia/Hong_Kong}")
	private String zoneId;

	@Value("${BROWSER_TYPE}")
	private String browserType;

	@Value("${HOST_URL:#{''}}")
	private String hostUrl;

	@Value("${REMOTE_DRIVER_VERSION:#{0}}")
	private int remoteDriverVersion;

	@Value("${GLOBAL_CHROME_OPTIONS::#{''}}")
	private String globalChromeOptions;

	@Value("${PRELOAD_LOCAL_STORAGE_DATA:#{false}}")
	private boolean preloadLocalStorageData;

	@Value("${LOCAL_STORAGE_DATA_PATH:#{''}}")
	private String localStorageDataPath;

	@Value("${DEFAULT_LOCAL_EXTENSION_PATH:#{''}}")
	private String defaultLocalExtensionPath;

	@Value("${DEFAULT_REMOTE_EXTENSION_PATH:#{''}}")
	private String defaultRemoteExtensionPath;

	@Value("${FRAMEWORK}")
	private String framework;

	@Value("${THREAD_COUNT}")
	private int threadCount;

	@Value("${DATAPROVIDER_THREAD_COUNT}")
	private int dataProviderThreadCount;

	@Value("${MAX_RETRY_COUNT}")
	private int maxRetryCount;

	@Value("${REMOVE_FAILED_TEST_B4_RETRY:#{false}}")
	private boolean removeFailedTestB4Retry;

	@Value("${TESTRAIL_SERVER:#{''}}")
	private String testRailServer;

	@Value("${TESTRAIL_USER_NAME:#{''}}")
	private String testRailUserName;

	@Value("${TESTRAIL_API_KEY:#{''}}")
	private String testRailAPIKey;

	@Value(value = "${TESTRAIL_TEST_RUN_NAME:#{''}}")
	private String testRailTestRunName;

	@Value("${TESTRAIL_PROJECT_ID:#{''}}")
	private String testRailProjectId;

	@Value("${TESTRAIL_STATUS_IN_PROGRESS_ID:#{''}}")
	private String testRailStatusInProgressId;

	@Value("${TESTRAIL_CREATE_NEW_TEST_RUN:#{false}}")
	private boolean testRailCreateNewTestRun;

	@Value("${TESTRAIL_TEST_STATUS_FILTER:#{''}}")
	private String testRailTestStatusFilter;

	@Value("${TESTRAIL_INCLUDE_ALL_AUTOMATED_TEST_CASES:#{false}}")
	private boolean testRailIncludeAllAutomatedTestCases;

	@Value("${TESTRAIL_UPLOAD_FLAG:#{false}}")
	private boolean testRailUploadTestResult;

	@Value("${EXCLUDE_GROUPS:#{''}}")
	private String excludeGroups;

	@Value("${INCLUDE_GROUPS}")
	private String includeGroups;

	@Value("${URL}")
	private String url;

	@Value("${FEATURE_DESCRIPTION:#{''}}")
	private String featureDescription;

	@Value("${DRIVER_HOME}")
	private String driverHome;

	@Value("${LOCAL_EXECUTION:OFF}")
	private String localExecutionMode;

	@Value("${EXTENT_XML_PATH:#{null}}")
	private String extentXMLPath;

	@Value("${NOTIFICATION_SEND_FAILED_CASE:#{false}}")
	private boolean sendFailedCaseNotification;

	@Value("${NOTIFICATION_FAILED_CASE_TESTRUN_PATTERN_REGEXP:#{''}}")
	private String failedCaseTestRunNotificationPattern;

	@Value("${NOTIFICATION_TESTRUN_COUNT:3}")
	private int failedCaseNotificationCount;

	@Value("${NOTIFICATION_FAILED_CASE_TESTRUN_WITHIN_DAYS:3}")
	private int failedCaseTestRunWithinDays;

	@Value("${NOTIFICATION_FAILED_CASE_EXCLUDE_LIST:#{''}}")
	private String failedCaseExcludeList;

	@Value("${NOTIFICATION_FAILED_CASE_SLACK_USERID:${GITLAB_USER_LOGIN:}}")
	private String failedCaseNotificationUserId;

	@Value("${NOTIFICATION_FAILED_CASE_SLACK_WEBHOOK:#{''}}")
	private String failedCaseNotificationSlackWebhook;
}
