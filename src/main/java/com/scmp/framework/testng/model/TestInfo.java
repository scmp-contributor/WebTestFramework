package com.scmp.framework.testng.model;

import com.scmp.framework.annotations.*;
import com.scmp.framework.annotations.screens.Device;
import com.scmp.framework.annotations.screens.DeviceName;
import com.scmp.framework.annotations.testrail.TestRailTestCase;
import com.scmp.framework.context.RunTimeContext;
import com.scmp.framework.model.Browser;
import com.scmp.framework.model.IProxyFactory;
import com.scmp.framework.testng.listeners.RetryAnalyzer;
import com.scmp.framework.testrail.TestRailDataService;
import com.scmp.framework.testrail.TestRailStatus;
import com.scmp.framework.testrail.models.TestRun;
import com.scmp.framework.testrail.models.TestRunTest;
import com.scmp.framework.utils.ConfigFileReader;
import lombok.Getter;
import net.lightbody.bmp.BrowserMobProxy;
import net.lightbody.bmp.client.ClientUtil;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.MutableCapabilities;
import org.openqa.selenium.Proxy;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.firefox.FirefoxOptions;
import org.openqa.selenium.logging.LogType;
import org.openqa.selenium.logging.LoggingPreferences;
import org.openqa.selenium.remote.CapabilityType;
import org.testng.IInvokedMethod;
import org.testng.IRetryAnalyzer;
import org.testng.ITestResult;
import org.testng.annotations.Test;

import java.lang.reflect.Method;
import java.net.Inet4Address;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Level;

import static com.scmp.framework.utils.Constants.*;

/**
 * TestInfo - Holds information about the current test being executed.
 */
public class TestInfo {
	private final IInvokedMethod testNGInvokedMethod;
	@Getter
	private final ITestResult testResult;
	private final Method declaredMethod;
	private Browser browserType = null;
	private TestRailDataService testRailDataService = null;
	private final LocalDateTime testStartTime;
	private LocalDateTime testEndTime = null;
	private Boolean isSkippedTest = null;
	private final RunTimeContext runTimeContext;

	public TestInfo(IInvokedMethod methodName, ITestResult testResult, RunTimeContext runTimeContext) {
		this.runTimeContext = runTimeContext;
		this.testNGInvokedMethod = methodName;
		this.testResult = testResult;
		this.declaredMethod = this.testNGInvokedMethod.getTestMethod().getConstructorOrMethod().getMethod();
		this.testStartTime = LocalDateTime.now(runTimeContext.getZoneId());

		// Init TestRail handler
		if (this.isTestMethod() && !this.isSkippedTest() && runTimeContext.getFrameworkConfigs().isTestRailUploadTestResult()) {
			TestRailTestCase testRailCase = this.declaredMethod.getAnnotation(TestRailTestCase.class);
			TestRun testRun = (TestRun) runTimeContext.getGlobalVariables(TEST_RUN_OBJECT);
			if (testRailCase != null && testRun != null) {
				this.testRailDataService = new TestRailDataService(testRailCase.id(), testRun);
			}
		}
	}

	/**
	 * Add test result for TestRail.
	 *
	 * @param status   the status of the test step
	 * @param content  the content of the test step
	 * @param filePath the file path of the screenshot or log
	 */
	public void addTestResultForTestRail(int status, String content, String filePath) {
		if (this.testRailDataService != null) {
			this.testRailDataService.addStepResult(status, content, filePath);
		}
	}

	/**
	 * Set the end time of the test.
	 */
	public void setTestEndTime() {
		this.testEndTime = LocalDateTime.now(runTimeContext.getZoneId());
	}

	/**
	 * Upload test results to TestRail.
	 */
	public void uploadTestResultsToTestRail() {
		if (this.testRailDataService != null) {
			int finalTestResult = TestRailStatus.Untested;
			switch (this.testResult.getStatus()) {
				case ITestResult.SUCCESS:
					finalTestResult = TestRailStatus.Passed;
					break;
				case ITestResult.FAILURE:
					finalTestResult = TestRailStatus.Failed;
					break;
				default:
			}

			if (this.testEndTime == null) {
				this.setTestEndTime();
			}

			long elapsed = Duration.between(this.testStartTime, this.testEndTime).getSeconds();
			elapsed = elapsed == 0 ? 1 : elapsed;
			this.testRailDataService.uploadDataToTestRail(finalTestResult, elapsed);
		}
	}

	/**
	 * Get the class name of the test.
	 *
	 * @return the class name
	 */
	public String getClassName() {
		return this.declaredMethod.getDeclaringClass().getSimpleName();
	}

	/**
	 * Get the groups defined at the class level.
	 *
	 * @return an array of group names
	 */
	public String[] getClassLevelGroups() {
		Test testNgTest = this.declaredMethod.getDeclaringClass().getAnnotation(Test.class);
		return testNgTest == null ? null : testNgTest.groups();
	}

	/**
	 * Get the description of the test class.
	 *
	 * @return the class description
	 */
	public String getClassDescription() {
		Test description = this.declaredMethod.getDeclaringClass().getAnnotation(Test.class);
		return description == null ? "" : description.description();
	}

	/**
	 * Get the method name of the test.
	 *
	 * @return the method name
	 */
	public String getMethodName() {
		return this.declaredMethod.getName();
	}

	/**
	 * Check if the method is a test method.
	 *
	 * @return true if it is a test method, false otherwise
	 */
	public boolean isTestMethod() {
		return this.declaredMethod.getAnnotation(Test.class) != null;
	}

	/**
	 * Get the author names of the test.
	 *
	 * @return an array of author names
	 */
	public String[] getAuthorNames() {
		return declaredMethod.getAnnotation(Authors.class) == null ? null : declaredMethod.getAnnotation(Authors.class).name();
	}

	/**
	 * Get the test name, including data provider if available.
	 *
	 * @return the test name
	 */
	public String getTestName() {
		String dataProvider = null;
		Object dataParameter = this.testNGInvokedMethod.getTestResult().getParameters();
		if (((Object[]) dataParameter).length > 0) {
			dataProvider = (String) ((Object[]) dataParameter)[0];
		}

		return dataProvider == null ? this.declaredMethod.getName() : this.declaredMethod.getName() + " [" + dataProvider + "]";
	}

	/**
	 * Get the description of the test method.
	 *
	 * @return the method description
	 */
	public String getTestMethodDescription() {
		return this.declaredMethod.getAnnotation(Test.class).description();
	}

	/**
	 * Get the groups defined for the test method.
	 *
	 * @return an array of group names
	 */
	public String[] getTestGroups() {
		return this.testNGInvokedMethod.getTestMethod().getGroups();
	}

	/**
	 * Get browser type based on the annotation/configs of each test case.
	 *
	 * @return Browser
	 */
	public Browser getBrowserType() {
		if (this.browserType != null) {
			return this.browserType;
		}

		// Get browser type from retry method
		Browser retryBrowserType = null;
		IRetryAnalyzer analyzer = testResult.getMethod().getRetryAnalyzer(testResult);
		if (analyzer instanceof RetryAnalyzer) {
			retryBrowserType = ((RetryAnalyzer) analyzer).getRetryMethod(testResult).getBrowserType();
		}

		String browserTypeParam = this.testNGInvokedMethod.getTestMethod().getXmlTest().getParameter("browser");
		Browser configBrowserType;
		try {
			configBrowserType = Browser.valueOf(browserTypeParam.toUpperCase());
		} catch (Exception e) {
			throw new RuntimeException("Unsupported browser: " + browserTypeParam.toUpperCase());
		}

		// Override browser type
		FirefoxOnly firefoxOnly = this.declaredMethod.getAnnotation(FirefoxOnly.class);
		ChromeOnly chromeOnly = this.declaredMethod.getAnnotation(ChromeOnly.class);
		CaptureNetworkTraffic4Chrome captureNetworkTraffic4Chrome = this.declaredMethod.getAnnotation(CaptureNetworkTraffic4Chrome.class);

		// Further update browser type based on annotation
		if (retryBrowserType != null) {
			browserType = retryBrowserType;
		} else if (firefoxOnly != null) {
			browserType = Browser.FIREFOX;
		} else if (chromeOnly != null || captureNetworkTraffic4Chrome != null) {
			browserType = Browser.CHROME;
		} else if (configBrowserType == Browser.RANDOM) {
			browserType = Math.round(Math.random()) == 1 ? Browser.CHROME : Browser.FIREFOX;
		} else {
			// Default browser type value from config
			browserType = configBrowserType;
		}

		return browserType;
	}

	/**
	 * Get testing device dimension.
	 *
	 * @return device dimension
	 */
	public Dimension getDeviceDimension() {
		// Check the mobile screen size preference
		Device deviceAnnotationData = this.declaredMethod.getAnnotation(Device.class);
		Dimension deviceDimension;
		if (deviceAnnotationData != null) {
			int width = deviceAnnotationData.device() == DeviceName.OtherDevice ? deviceAnnotationData.width() : deviceAnnotationData.device().width;
			int height = deviceAnnotationData.device() == DeviceName.OtherDevice ? deviceAnnotationData.height() : deviceAnnotationData.device().height;
			deviceDimension = new Dimension(width, height);
		} else {
			// If device dimension is not specified, use desktop by default
			deviceDimension = new Dimension(DeviceName.DeskTopHD.width, DeviceName.DeskTopHD.height);
		}

		return deviceDimension;
	}

	/**
	 * Get Chrome options based on the annotation/configs of each test case.
	 *
	 * @return ChromeOptions
	 */
	public ChromeOptions getChromeOptions() {
		ChromeOptions options = new ChromeOptions();

		// If the test is not tagged skip chrome options, use Global_Chrome_Options which has options separated by comma
		if (this.declaredMethod.getAnnotation(SkipGlobalChromeOptions.class) == null) {
			String global_chrome_options = runTimeContext.getFrameworkConfigs().getGlobalChromeOptions();

			// Only add arguments if global_chrome_options has something
			if (global_chrome_options != null && !global_chrome_options.isEmpty()) {
				String[] parsedOptions = global_chrome_options.split(",");

				for (String parsedOption : parsedOptions) {
					options.addArguments(parsedOption);
				}
			}
		}

		// Temporary solution for fixing the bug: https://stackoverflow.com/questions/75678572/java-io-ioexception-invalid-status-code-403-text-forbidden
		options.addArguments("--remote-allow-origins=*");

		// Get Chrome options/arguments
		ChromeArguments chromeArguments = this.declaredMethod.getAnnotation(ChromeArguments.class);
		if (chromeArguments != null && chromeArguments.options().length > 0) {
			options.addArguments(chromeArguments.options());
		}

		// private mode
		IncognitoPrivateMode privateMode = this.declaredMethod.getAnnotation(IncognitoPrivateMode.class);
		if (privateMode != null) {
			options.addArguments("--incognito");
		}

		// headless mode
		HeadlessMode headlessMode = this.declaredMethod.getAnnotation(HeadlessMode.class);
		// If headless mode is not specified, use the global headless mode
		int majorVersion = 0;
		if (headlessMode == null || headlessMode.status()) {
			// Get version from config if using remote selenium server
			if (!runTimeContext.isLocalExecutionMode()) {
				majorVersion = runTimeContext.getFrameworkConfigs().getRemoteDriverVersion();
			} else {
				// Get local driver version
				String version = runTimeContext.getGlobalVariables(CHROME_DRIVER_VERSION).toString();
				// Split the version string to get the major version
				String[] versionParts = version.split("\\.");
				if (versionParts.length > 0) {
					majorVersion = Integer.parseInt(versionParts[0]);
				}
			}

			// Check if the major version is greater than 109
			if (majorVersion >= 109) {
				options.addArguments("--headless=new");
			} else if (majorVersion >= 96) {
				options.addArguments("--headless=chrome");
			} else {
				/* do nothing */
			}
		}

		// Accept untrusted certificates
		AcceptUntrustedCertificates acceptUntrustedCertificates = this.declaredMethod.getAnnotation(AcceptUntrustedCertificates.class);
		// If acceptUntrustedCertificates is not specified, use the global acceptUntrustedCertificates
		options.setAcceptInsecureCerts(acceptUntrustedCertificates == null || acceptUntrustedCertificates.status());

		// Capture network traffic
		CaptureNetworkTraffic4Chrome captureNetworkTraffic4Chrome = this.declaredMethod.getAnnotation(CaptureNetworkTraffic4Chrome.class);
		if (captureNetworkTraffic4Chrome != null) {
			LoggingPreferences preferences = new LoggingPreferences();
			preferences.enable(LogType.PERFORMANCE, Level.ALL);
			options.setCapability("goog:loggingPrefs", preferences);
		}

		// Enable proxy
		CustomProxy customProxy = this.declaredMethod.getAnnotation(CustomProxy.class);
		if (customProxy != null) {
			BrowserMobProxy proxy;
			Class proxyCls = customProxy.factory();
			if (IProxyFactory.class.isAssignableFrom(proxyCls)) {
				try {
					IProxyFactory proxyObj = (IProxyFactory) proxyCls.getConstructor().newInstance();
					proxy = proxyObj.getProxy(customProxy.name());
					if (proxy == null) {
						throw new RuntimeException("Custom Proxy cannot be null!");
					}

					Proxy seleniumProxy = ClientUtil.createSeleniumProxy(proxy);

					String hostIp = Inet4Address.getLocalHost().getHostAddress();
					seleniumProxy.setHttpProxy(hostIp + ":" + proxy.getPort()); // The port generated by server.start();
					seleniumProxy.setSslProxy(hostIp + ":" + proxy.getPort());

					options.addArguments("--ignore-certificate-errors");
					options.addArguments("--ignore-urlfetcher-cert-requests");

					options.setCapability(CapabilityType.PROXY, seleniumProxy);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}

		// Load default extension
		String extensionPath = runTimeContext.getDefaultExtensionPath();
		if (!extensionPath.isEmpty()) {
			options.addArguments("load-extension=" + extensionPath);
		}

		return options;
	}

	/**
	 * Get Firefox options based on the annotation/configs of each test case.
	 *
	 * @return FirefoxOptions
	 */
	public FirefoxOptions getFirefoxOptions() {
		FirefoxOptions options = new FirefoxOptions();
		// Get Firefox options/arguments
		FirefoxArguments firefoxArguments = this.declaredMethod.getAnnotation(FirefoxArguments.class);
		if (firefoxArguments != null && firefoxArguments.options().length > 0) {
			options.addArguments(firefoxArguments.options());
		}

		// private mode
		IncognitoPrivateMode privateMode = this.declaredMethod.getAnnotation(IncognitoPrivateMode.class);
		if (privateMode != null) {
			options.addArguments("-private");
		}

		// headless mode
		HeadlessMode headlessMode = this.declaredMethod.getAnnotation(HeadlessMode.class);
		// If headless mode is not specified, use the global headless mode
		if (headlessMode == null || headlessMode.status()) {
			options.addArguments("--headless=new");
		}

		// Accept untrusted certificates
		AcceptUntrustedCertificates acceptUntrustedCertificates = this.declaredMethod.getAnnotation(AcceptUntrustedCertificates.class);
		// If acceptUntrustedCertificates is not specified, use the global acceptUntrustedCertificates
		options.setAcceptInsecureCerts(acceptUntrustedCertificates == null || acceptUntrustedCertificates.status());

		return options;
	}

	/**
	 * Get browser options based on the annotation/configs of each test case.
	 *
	 * @return Browser Options
	 */
	public MutableCapabilities getBrowserOption() {
		Browser browserType = this.getBrowserType();
		return switch (browserType) {
			case CHROME -> this.getChromeOptions();
			case FIREFOX -> this.getFirefoxOptions();
			default -> throw new RuntimeException("Unsupported browser: " + browserType);
		};
	}

	/**
	 * Get the local storage data from:
	 * 1. config.properties: LOCAL_STORAGE_DATA_PATH
	 * 2. CustomLocalStorage: path
	 * 3. CustomLocalStorage: LocalStorageData
	 *
	 * @return Map for local storage configs
	 */
	public Map<String, String> getCustomLocalStorage() {
		Map<String, String> customData = new HashMap<>();
		boolean loadDefaultData = runTimeContext.getFrameworkConfigs().isPreloadLocalStorageData();
		CustomLocalStorage customLocalStorage = this.declaredMethod.getAnnotation(CustomLocalStorage.class);
		loadDefaultData = customLocalStorage != null && customLocalStorage.loadDefault() || loadDefaultData;

		// Load default data
		if (loadDefaultData) {
			String filePath = runTimeContext.getFrameworkConfigs().getLocalStorageDataPath();
			customData.putAll(new ConfigFileReader(filePath).getAllProperties());
		}

		// Load custom data file
		if (customLocalStorage != null && !"".equalsIgnoreCase(customLocalStorage.path().trim())) {
			String filePath = customLocalStorage.path().trim();
			customData.putAll(new ConfigFileReader(filePath).getAllProperties());
		}

		// Load custom data
		if (customLocalStorage != null) {
			for (LocalStorageData data : customLocalStorage.data()) {
				customData.put(data.key(), data.value());
			}
		}

		return customData;
	}

	/**
	 * Check if the test is in the TestRail test list.
	 *
	 * @return true if the test is in the TestRail test list, false otherwise
	 */
	public boolean isInTestRailTestList() {
		Object filteredTestsObject = runTimeContext.getGlobalVariables(FILTERED_TEST_OBJECT);
		if (filteredTestsObject instanceof List) {
			List<TestRunTest> filteredTests = (List<TestRunTest>) filteredTestsObject;

			TestRailTestCase testRailTestCase = this.declaredMethod.getAnnotation(TestRailTestCase.class);
			if (testRailTestCase == null) return false;
			Optional<TestRunTest> result = filteredTests.parallelStream().filter(test -> test.getCaseId() == testRailTestCase.id()).findFirst();

			return result.isPresent();
		}

		return true;
	}

	/**
	 * Check if the test is skipped.
	 *
	 * @return true if the test is skipped, false otherwise
	 */
	public boolean isSkippedTest() {
		if (this.isSkippedTest == null) {
			this.isSkippedTest = !this.isInTestRailTestList();
		}

		return this.isSkippedTest;
	}

	/**
	 * Check if the browser needs to be launched for the test.
	 *
	 * @return true if the browser needs to be launched, false otherwise
	 */
	public boolean needLaunchBrowser() {
		LaunchBrowser launchBrowser = this.declaredMethod.getAnnotation(LaunchBrowser.class);
		return launchBrowser == null || launchBrowser.status();
	}
}