// src/main/java/com/scmp/framework/services/ReportService.java
package com.scmp.framework.services;

import com.aventstack.extentreports.ExtentTest;
import com.aventstack.extentreports.Status;
import com.scmp.framework.context.RunTimeContext;
import com.scmp.framework.report.ExtentTestService;
import com.scmp.framework.testng.listeners.RetryAnalyzer;
import com.scmp.framework.testng.model.TestInfo;
import com.scmp.framework.testrail.TestRailStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.testng.IRetryAnalyzer;
import org.testng.ITestResult;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;

import static com.scmp.framework.utils.Constants.TARGET_PATH;

/**
 * ReportManager - Handles all Reporting activities e.g. communication with ExtentManager, etc
 */
@Component
public class ReportService {
	private static final Logger frameworkLogger = LoggerFactory.getLogger(ReportService.class);
	private final ThreadLocal<TestInfo> testInfo = new ThreadLocal<>();
	private final ThreadLocal<ExtentTest> parentTestClass = new ThreadLocal<>();
	private final ThreadLocal<ExtentTest> currentTestMethod = new ThreadLocal<>();
	private final ThreadLocal<ITestResult> testResult = new ThreadLocal<>();
	private final ThreadLocal<Boolean> setupStatus = new ThreadLocal<>();
	private final ScreenShotService screenshotService;
	private final RunTimeContext runTimeContext;
	private final ExtentTestService extentTestService;

	@Autowired
	private ReportService(RunTimeContext runTimeContext, ExtentTestService extentTestService, ScreenShotService screenshotService) {
		this.runTimeContext = runTimeContext;
		this.screenshotService = screenshotService;
		this.extentTestService = extentTestService;
	}

	/**
	 * Remove the current test from the report.
	 */
	public void removeTest() {
		extentTestService.removeTest(currentTestMethod.get());
	}

	/**
	 * Mark the current test as skipped.
	 */
	public void skipTest() {
		currentTestMethod.get().getModel().setStatus(Status.SKIP);
	}

	/**
	 * Log details for failure case
	 * 1. Print stack trace
	 * 2. Capture screenshot
	 *
	 * @param result TestNG test result
	 */
	private void handleTestFailure(ITestResult result) {
		if (result.getStatus() == ITestResult.FAILURE) {
			// Print exception stack trace if any
			Throwable throwable = result.getThrowable();
			if (throwable != null) {
				frameworkLogger.error("Test failed with exception: ", throwable);
				currentTestMethod.get().log(Status.FAIL, "<pre>" + result.getThrowable().getMessage() + "</pre>");
				this.addTestRailLog(TestRailStatus.Failed, result.getThrowable().getMessage(), null);
			}

			// Add screenshot
			addScreenshotToReport(result, Status.FAIL);
		}
	}

	/**
	 * End logging test results and handle retries if necessary.
	 *
	 * @param result TestNG test result
	 */
	public void endLogTestResults(ITestResult result) {
		this.testInfo.get().setTestEndTime();
		if (result.isSuccess()) {
			String message = "Test Passed: " + result.getMethod().getMethodName();
			currentTestMethod.get().log(Status.PASS, message);
			this.addTestRailLog(TestRailStatus.Passed, message, null);
		} else {
			if (result.getStatus() == ITestResult.FAILURE) {
				handleTestFailure(result);
			}
		}

		if (result.getStatus() == ITestResult.SKIP) {
			currentTestMethod.get().log(Status.SKIP, "Test skipped");
		}

		extentTestService.flush();

		// Handling for Retry
		handleRetry(result);

		extentTestService.flush();

		this.testInfo.get().uploadTestResultsToTestRail();
	}

	/**
	 * Handle retry logic for failed tests.
	 *
	 * @param result TestNG test result
	 */
	private void handleRetry(ITestResult result) {
		IRetryAnalyzer analyzer = result.getMethod().getRetryAnalyzer(result);
		if (analyzer instanceof RetryAnalyzer) {
			if (((RetryAnalyzer) analyzer).isRetriedMethod(result) || result.getStatus() == ITestResult.FAILURE) {
				this.addTag("RETRIED");
			}

			if (runTimeContext.getFrameworkConfigs().isRemoveFailedTestB4Retry()
					&& result.getStatus() == ITestResult.FAILURE
					&& ((RetryAnalyzer) analyzer).isRetriedRequired(result)) {
				this.removeTest();
			}
		}

		if (result.getStatus() == ITestResult.SKIP) {
			this.skipTest();
		}
	}

	/**
	 * Set the current test result.
	 *
	 * @param testResult TestNG test result
	 */
	public void setTestResult(ITestResult testResult) {
		this.testResult.set(testResult);
	}

	/**
	 * Set the setup status.
	 *
	 * @param status setup status
	 */
	public void setSetupStatus(boolean status) {
		this.setupStatus.set(status);
	}

	/**
	 * Get the setup status.
	 *
	 * @return setup status
	 */
	public boolean getSetupStatus() {
		return this.setupStatus.get() != null && this.setupStatus.get();
	}

	/**
	 * Setup the report for a test set.
	 *
	 * @param testInfo test information
	 */
	public synchronized void setupReportForTestSet(TestInfo testInfo) {
		ExtentTest parent = extentTestService.createTest(testInfo.getClassName(), testInfo.getClassDescription());
		if (testInfo.getClassLevelGroups() != null) {
			parent.assignCategory(testInfo.getClassLevelGroups());
		}

		parentTestClass.set(parent);
	}

	/**
	 * Set the test information for the current test.
	 *
	 * @param testInfo test information
	 */
	public synchronized void setTestInfo(TestInfo testInfo) {
		this.testInfo.set(testInfo);

		ExtentTest child = parentTestClass.get().createNode(testInfo.getTestName(), testInfo.getTestMethodDescription());
		currentTestMethod.set(child);

		// Update authors
		if (testInfo.getAuthorNames() != null) {
			currentTestMethod.get().assignAuthor(testInfo.getAuthorNames());
		}

		// Update groups to category
		currentTestMethod.get().assignCategory(testInfo.getTestGroups());
		// Added browser type tag to test
		currentTestMethod.get().assignCategory(testInfo.getBrowserType().toString());
	}

	/**
	 * Add a tag to the current test.
	 *
	 * @param tag tag to add
	 */
	public void addTag(String tag) {
		this.currentTestMethod.get().assignCategory(tag);
	}

	/**
	 * Get the image path for a screenshot.
	 *
	 * @param imageName name of the image
	 * @return image path
	 */
	public String getImagePath(String imageName) {
		String[] classAndMethod = getTestClassNameAndMethodName().split(",");
		try {
			return screenshotService.getScreenshotPath(classAndMethod[0], classAndMethod[1], imageName);
		} catch (Exception e) {
			frameworkLogger.error("Error getting screenshot path: ", e);
			return null;
		}
	}

	/**
	 * Add a log entry to TestRail.
	 *
	 * @param status    status of the test
	 * @param message   log message
	 * @param imagePath path to the image
	 */
	private void addTestRailLog(int status, String message, String imagePath) {
		this.testInfo.get().addTestResultForTestRail(status, message, imagePath);
	}

	/**
	 * Log an informational message.
	 *
	 * @param message log message
	 */
	public void logInfo(String message) {
		this.currentTestMethod.get().log(Status.INFO, message);
		this.addTestRailLog(TestRailStatus.Passed, message, null);
	}

	/**
	 * Log a screenshot and return its path.
	 *
	 * @return path to the screenshot
	 */
	public String logScreenshot() {
		String imagePath = this.logScreenshot(Status.INFO);
		this.addTestRailLog(TestRailStatus.Passed, "", imagePath);

		return imagePath;
	}

	/**
	 * Log a screenshot with a specific status.
	 *
	 * @param status status of the log entry
	 * @return path to the screenshot
	 */
	private String logScreenshot(Status status) {
		try {
			String[] classAndMethod = getTestClassNameAndMethodName().split(",");
			String screenShotAbsolutePath = screenshotService.captureScreenShot(Status.INFO, classAndMethod[0], classAndMethod[1]);
			String screenShotRelativePath = getRelativePathToReport(screenShotAbsolutePath);
			this.currentTestMethod.get().log(status,
					"<img data-featherlight=" + screenShotRelativePath + " width=\"10%\" src=" + screenShotRelativePath + " data-src=" + screenShotRelativePath + ">");

			return screenShotAbsolutePath;
		} catch (Exception e) {
			frameworkLogger.error("Error capturing screenshot: ", e);
		}

		return "";
	}

	/**
	 * Log an informational message with a screenshot.
	 *
	 * @param message log message
	 */
	public void logInfoWithScreenshot(String message) {
		this.currentTestMethod.get().log(Status.INFO, message);
		String imagePath = this.logScreenshot();

		this.addTestRailLog(TestRailStatus.Passed, message, imagePath);
	}

	/**
	 * Log a pass message.
	 *
	 * @param message log message
	 */
	public void logPass(String message) {
		this.currentTestMethod.get().log(Status.PASS, message);
		this.addTestRailLog(TestRailStatus.Passed, message, null);
	}

	/**
	 * Log a pass message with a screenshot.
	 *
	 * @param message log message
	 */
	public void logPassWithScreenshot(String message) {
		this.currentTestMethod.get().log(Status.PASS, message);
		String imagePath = this.logScreenshot(Status.PASS);

		this.addTestRailLog(TestRailStatus.Passed, message, imagePath);
	}

	/**
	 * Log a fail message.
	 *
	 * @param message log message
	 */
	public void logFail(String message) {
		this.testResult.get().setStatus(ITestResult.FAILURE);
		this.currentTestMethod.get().log(Status.FAIL, message);
		String imagePath = this.logScreenshot(Status.FAIL);

		this.addTestRailLog(TestRailStatus.Failed, message, imagePath);
	}

	/**
	 * Log a fail message without a screenshot.
	 *
	 * @param message log message
	 */
	public void logFailWithoutScreenshot(String message) {
		this.currentTestMethod.get().log(Status.FAIL, message);
		this.testResult.get().setStatus(ITestResult.FAILURE);

		this.addTestRailLog(TestRailStatus.Failed, message, null);
	}

	/**
	 * Log a fail message with an image.
	 *
	 * @param message           log message
	 * @param originalImagePath path to the image
	 */
	public void logFailWithImage(String message, String originalImagePath) {
		String imageRelativePath = getRelativePathToReport(originalImagePath);
		try {
			this.currentTestMethod.get().log(Status.FAIL, message);
			this.currentTestMethod.get().log(Status.FAIL,
					"<img data-featherlight=" + imageRelativePath + " width=\"10%\" src=" + imageRelativePath + " data-src=" + imageRelativePath + ">");
			this.testResult.get().setStatus(ITestResult.FAILURE);

			this.addTestRailLog(TestRailStatus.Failed, message, originalImagePath);
		} catch (Exception e) {
			frameworkLogger.error("Error logging fail with image: ", e);
		}
	}

	/**
	 * Log a JSON string to a file.
	 *
	 * @param json     JSON string
	 * @param fileName name of the file
	 */
	public void logJson(String json, String fileName) {
		StackTraceElement[] stElements = Thread.currentThread().getStackTrace();
		int classIndex = 0;
		for (int i = 0; i < stElements.length; i++) {
			if (stElements[i].getClassName().equals("sun.reflect.NativeMethodAccessorImpl")) {
				classIndex = i - 1;
				break;
			}
		}
		String filePath = runTimeContext.getLogPath("json", stElements[classIndex].getClassName(), stElements[classIndex].getMethodName());
		if (fileName == null) {
			filePath = filePath + File.separator + RunTimeContext.currentDateAndTime() + ".json";
		} else {
			filePath = filePath + File.separator + fileName + ".json";
		}

		try (FileWriter fw = new FileWriter(filePath)) {
			fw.write(json);
			fw.flush();

			this.logInfo("<a target='_blank' href='" + getRelativePathToReport(filePath) + "'> " + fileName + " </a>");
		} catch (IOException e) {
			frameworkLogger.error("Error writing JSON to file: ", e);
		}
	}

	/**
	 * Capture a screenshot and return its path.
	 *
	 * @return path to the screenshot
	 */
	public String captureScreenShot() {
		try {
			String[] classAndMethod = getTestClassNameAndMethodName().split(",");
			return screenshotService.captureScreenShot(Status.INFO, classAndMethod[0], classAndMethod[1]);
		} catch (Exception e) {
			frameworkLogger.error("Error capturing screenshot: ", e);
		}

		return null;
	}

	/**
	 * Attach an image to the current test.
	 *
	 * @param imagePath path to the image
	 */
	public void attachImage(String imagePath) {
		try {
			this.currentTestMethod.get().addScreenCaptureFromPath(imagePath);
			this.addTestRailLog(TestRailStatus.Passed, "", imagePath);
		} catch (Exception e) {
			frameworkLogger.error("Error attaching image: ", e);
		}
	}

	/**
	 * Get the relative path to the report for a given file.
	 *
	 * @param file file path
	 * @return relative path to the report
	 */
	public String getRelativePathToReport(String file) {
		Path path = new File(file).toPath();
		Path targetPath = new File(TARGET_PATH).toPath();
		return targetPath.relativize(path).toString();
	}

	/**
	 * Get the test class name and method name.
	 *
	 * @return test class name and method name
	 */
	private String getTestClassNameAndMethodName() {
		String classAndMethod = "";

		Exception ex = new Exception();
		StackTraceElement[] stacks = ex.getStackTrace();
		for (StackTraceElement e : stacks) {
			classAndMethod = e.getClassName() + "," + e.getMethodName();

			if (e.getMethodName().startsWith("test")) {
				classAndMethod = e.getClassName().substring(e.getClassName().lastIndexOf(".") + 1) + "," + e.getMethodName();
				break;
			}
		}

		return classAndMethod;
	}

	/**
	 * Add a screenshot to the report.
	 *
	 * @param result TestNG test result
	 * @param status status of the log entry
	 */
	private void addScreenshotToReport(ITestResult result, Status status) {
		try {
			String screenShotAbsolutePath = screenshotService.captureScreenShot(
					status,
					result.getInstance().getClass().getSimpleName(),
					result.getMethod().getMethodName());

			String screenShotRelativePath = getRelativePathToReport(screenShotAbsolutePath);
			currentTestMethod.get().addScreenCaptureFromPath(screenShotRelativePath);
			this.addTestRailLog(TestRailStatus.Failed, "", screenShotAbsolutePath);
		} catch (Exception e) {
			frameworkLogger.error("Error adding screenshot to report: ", e);
		}
	}
}