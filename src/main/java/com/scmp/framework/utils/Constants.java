package com.scmp.framework.utils;

import java.io.File;

public class Constants {

	// System Properties
	public static final String USER_DIR = "user.dir";
	public static final String TARGET_PATH = System.getProperty(USER_DIR) + File.separator + "target";

	// WebDriver Manager Cache Path
	public static final String WDM_CACHE_PATH = "wdm.cachePath";

	// WebDriver System Property Names
	public static final String CHROME_DRIVER_SYSTEM_PROPERTY_NAME = "webdriver.chrome.driver";
	public static final String FIREFOX_DRIVER_SYSTEM_PROPERTY_NAME = "webdriver.gecko.driver";

	// WebDriver Paths
	public static final String CHROME_DRIVER_PATH = "CHROME_DRIVER_PATH";
	public static final String FIREFOX_DRIVER_PATH = "FIREFOX_DRIVER_PATH";

	// Keys for Global Variables
	public static final String TEST_INFO_OBJECT = "TEST_INFO_OBJECT";
	public static final String TEST_RUN_OBJECT = "TEST_RUN_OBJECT";
	public static final String FILTERED_TEST_OBJECT = "FILTERED_TEST_OBJECT";

	// Messages
	public static final String INVALID_PROJECT_ID_MESSAGE = "Config TESTRAIL_PROJECT_ID [%s] is invalid!";
	public static final String DEFAULT_TEST_RUN_NAME = "Automated Test Run %s";
	public static final String TEST_RUN_CREATION_FAILED_MESSAGE = "Failed to create Test Run in TestRail.";
	public static final String TEST_RUN_NOT_CREATED_MESSAGE = "Test Run is NOT created in TestRail, empty Test Case List is detected.";
}