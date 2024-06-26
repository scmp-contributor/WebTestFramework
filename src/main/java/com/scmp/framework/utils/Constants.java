package com.scmp.framework.utils;

import java.io.File;

public class Constants {

	public static String USER_DIR = "user.dir";
	public static String TARGET_PATH = System.getProperty(USER_DIR) + File.separator + "target";

	public static String WDM_CACHE_PATH = "wdm.cachePath";
	public static String CHROME_DRIVER_SYSTEM_PROPERTY_NAME = "webdriver.chrome.driver";
	public static String FIREFOX_DRIVER_SYSTEM_PROPERTY_NAME = "webdriver.gecko.driver";

	public static String CHROME_DRIVER_PATH = "CHROME_DRIVER_PATH";
	public static String FIREFOX_DRIVER_PATH = "FIREFOX_DRIVER_PATH";

	// Key for TestInfo
	public static String TEST_INFO_OBJECT = "TEST_INFO_OBJECT";

	// Key for TestRun
	public static String TEST_RUN_OBJECT = "TEST_RUN_OBJECT";

	// Key for Filtered Tests
	public static String FILTERED_TEST_OBJECT = "FILTERED_TEST_OBJECT";
}
