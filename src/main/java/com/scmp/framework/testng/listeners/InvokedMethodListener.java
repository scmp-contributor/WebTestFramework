package com.scmp.framework.testng.listeners;

import com.scmp.framework.context.ApplicationContextProvider;
import com.scmp.framework.context.RunTimeContext;
import com.scmp.framework.services.ReportService;
import com.scmp.framework.services.WebDriverService;
import com.scmp.framework.testng.model.TestInfo;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.MutableCapabilities;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.testng.*;

import java.lang.reflect.Method;

import static com.scmp.framework.utils.Constants.TEST_INFO_OBJECT;

public final class InvokedMethodListener implements IInvokedMethodListener {
	private static final Logger frameworkLogger = LoggerFactory.getLogger(InvokedMethodListener.class);

	private final WebDriverService webDriverService;
	private final RunTimeContext runTimeContext;
	private final ReportService reportService;

	public InvokedMethodListener() {
		ApplicationContext context = ApplicationContextProvider.getApplicationContext();
		webDriverService = context.getBean(WebDriverService.class);
		runTimeContext = context.getBean(RunTimeContext.class);
		reportService = context.getBean(ReportService.class);
	}

	/**
	 * Before each method invocation
	 * Initialize Web Driver and Report Manager
	 */
	@Override
	public void beforeInvocation(IInvokedMethod method, ITestResult testResult) {
		// Clear all runtime variables
		runTimeContext.clearRunTimeVariables();

		TestInfo testInfo = new TestInfo(method, testResult, runTimeContext);
		// Save TestInfo to runtime memory
		runTimeContext.setTestLevelVariables(TEST_INFO_OBJECT, testInfo);

		// Skip beforeInvocation if current method is not with Annotation Test, or
		// Current Test need to be skipped
		if (!testInfo.isTestMethod() || testInfo.isSkippedTest()) {
			throw new SkipException("Skipped Test - " + testInfo.getTestName());
		}

		frameworkLogger.info("Start running test [{}]", testInfo.getMethodName());
		try {
			if (testInfo.needLaunchBrowser()) {
				setupDriverForTest(testInfo);
			}
			setupReporterForTest(testInfo);
		} catch (Exception ex) {
			frameworkLogger.error("Failed to setup test driver.", ex);
			reportService.setSetupStatus(false);
			Assert.fail("Fails to setup test driver.");
		}
	}

	/**
	 * After each method invocation
	 * Update test result to report manager and stop Web Driver
	 */
	@Override
	public void afterInvocation(IInvokedMethod method, ITestResult testResult) {
		TestInfo testInfo = (TestInfo) runTimeContext.getTestLevelVariables(TEST_INFO_OBJECT);
		// Skip afterInvocation if current method is not with Annotation Test, or
		// Current Test need to be skipped
		if (!testInfo.isTestMethod() || testInfo.isSkippedTest()) {
			return;
		}

		Method refMethod = method.getTestMethod().getConstructorOrMethod().getMethod();
		String methodName = refMethod.getName();

		frameworkLogger.info("Completed running test [{}]", methodName);

		// If fails to set up test
		if (!reportService.getSetupStatus()) {
			if (testInfo.needLaunchBrowser()) {
				webDriverService.stopWebDriver();
			}
			return;
		}

		try {
			reportService.endLogTestResults(testResult);
			// Clear all runtime variables
			runTimeContext.clearRunTimeVariables();

			// Stop driver
			if (testInfo.needLaunchBrowser()) {
				webDriverService.stopWebDriver();
			}
		} catch (Exception e) {
			frameworkLogger.error("Error during afterInvocation", e);
		}
	}

	/**
	 * Setup reporter in report manager
	 *
	 * @param testInfo test metadata for the test case
	 */
	private void setupReporterForTest(TestInfo testInfo) {
		try {
			// Create test node for test class in test report
			reportService.setupReportForTestSet(testInfo);
			reportService.setTestResult(testInfo.getTestResult());

			// Create test case in test report
			reportService.setTestInfo(testInfo);
			reportService.setSetupStatus(true);
		} catch (Exception e) {
			frameworkLogger.error("Error setting up reporter for test", e);
		}
	}

	/**
	 * Setup web driver for current test
	 *
	 * @param testInfo test metadata for the test case
	 * @throws Exception exception for starting driver instance
	 */
	private void setupDriverForTest(TestInfo testInfo) throws Exception {
		MutableCapabilities browserOptions = testInfo.getBrowserOption();
		Dimension deviceDimension = testInfo.getDeviceDimension();

		try {
			// Setup web driver
			webDriverService.startDriverInstance(browserOptions, deviceDimension);
		} catch (Exception ex1) {
			if (!runTimeContext.isLocalExecutionMode()) {
				webDriverService.stopWebDriver();
				// Wait 30 seconds and retry driver setup
				Thread.sleep(30000);

				// Setup web driver
				webDriverService.startDriverInstance(browserOptions, deviceDimension);
			} else {
				throw ex1;
			}
		}
	}
}