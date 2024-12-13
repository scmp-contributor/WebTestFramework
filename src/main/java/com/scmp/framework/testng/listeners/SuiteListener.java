// src/main/java/com/scmp/framework/testng/listeners/SuiteListener.java
package com.scmp.framework.testng.listeners;

import com.scmp.framework.annotations.testrail.TestRailTestCase;
import com.scmp.framework.context.ApplicationContextProvider;
import com.scmp.framework.context.FrameworkConfigs;
import com.scmp.framework.context.RunTimeContext;
import com.scmp.framework.testrail.TestRailManager;
import com.scmp.framework.testrail.TestRailStatus;
import com.scmp.framework.testrail.models.TestRun;
import com.scmp.framework.testrail.models.TestRunResult;
import com.scmp.framework.testrail.models.TestRunTest;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.testng.ISuite;
import org.testng.ISuiteListener;

import java.io.IOException;
import java.time.LocalDate;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static com.scmp.framework.utils.Constants.FILTERED_TEST_OBJECT;
import static com.scmp.framework.utils.Constants.TEST_RUN_OBJECT;

public class SuiteListener implements ISuiteListener {

	private static final Logger frameworkLogger = LoggerFactory.getLogger(SuiteListener.class);

	private final RunTimeContext runTimeContext;
	private final FrameworkConfigs frameworkConfigs;
	private final TestRailManager testRailManager;

	public SuiteListener() {
		// TestNG's context doesn't load the Application context from Spring
		// That is why use ApplicationContextProvider.getApplicationContext() instead of @Autowired
		ApplicationContext context = ApplicationContextProvider.getApplicationContext();
		runTimeContext = context.getBean(RunTimeContext.class);
		testRailManager = context.getBean(TestRailManager.class);
		frameworkConfigs = context.getBean(FrameworkConfigs.class);
	}

	@Override
	public void onFinish(ISuite suite) {
		// Log the completion of the test suite execution
		frameworkLogger.info("Test Suite execution completed.");

		// Log consecutive failed test cases
		if(!runTimeContext.isLocalExecutionMode() && frameworkConfigs.isSendFailedCaseNotification()){
			frameworkLogger.info("Logging consecutive failed test cases...");
			logConsecutiveFailedTestCase();
		}
	}

	@Override
	public void onStart(ISuite suite) {

		// Check if TestRail upload is enabled in the configuration
		if (frameworkConfigs.isTestRailUploadTestResult()) {
			try {
				setupTestRunInTestRail(suite);
			} catch (IOException e) {
				frameworkLogger.error("Failed to create Test Run in TestRail.", e);
				throw new RuntimeException("Failed to create Test Run in TestRail.", e);
			} catch (Exception e) {
				frameworkLogger.error("Unexpected error occurred while creating Test Run in TestRail.", e);
				throw new RuntimeException("Unexpected error occurred while creating Test Run in TestRail.", e);
			}
		}
	}

	/**
	 * Retrieve all TestRail test case IDs from the suite.
	 *
	 * @param suite the test suite
	 * @return a list of TestRail test case IDs
	 */
	private List<Integer> getAllTestRailTestCases(@NotNull ISuite suite) {
		return suite.getAllMethods().stream()
				.map(method -> {
					TestRailTestCase testRailTestCase = method.getConstructorOrMethod().getMethod().getAnnotation(TestRailTestCase.class);
					return testRailTestCase != null ? testRailTestCase.id() : null;
				})
				.filter(Objects::nonNull)
				.sorted()
				.collect(Collectors.toList());
	}

	/**
	 * Set up a Test Run in TestRail.
	 *
	 * @param suite the test suite
	 * @throws IOException if an I/O error occurs
	 */
	private void setupTestRunInTestRail(ISuite suite) throws IOException {
		frameworkLogger.info("Creating Test Run in TestRail...");

		String projectId = frameworkConfigs.getTestRailProjectId();
		validateProjectId(projectId);

		LocalDate today = LocalDate.now(runTimeContext.getZoneId());
		String timestamp = String.valueOf(today.minusDays(7).atStartOfDay(runTimeContext.getZoneId()).toEpochSecond());
		String defaultTestRunName = String.format("Automated Test Run - %s", today);
		String testRunName = frameworkConfigs.getTestRailTestRunName().isEmpty() ? defaultTestRunName : frameworkConfigs.getTestRailTestRunName().trim();

		if (!frameworkConfigs.isTestRailCreateNewTestRun()) {
			Optional<TestRun> existingTestRun = findExistingTestRun(projectId, timestamp, testRunName);
			if (existingTestRun.isPresent()) {
				useExistingTestRun(existingTestRun.get());
				return;
			}
		}

		createNewTestRun(suite, projectId, testRunName);
	}

	/**
	 * Validate the TestRail project ID.
	 *
	 * @param projectId the project ID to validate
	 */
	private void validateProjectId(String projectId) {
		if (projectId == null || !Pattern.compile("[0-9]+").matcher(projectId).matches()) {
			throw new IllegalArgumentException(String.format("Invalid project ID: %s", projectId));
		}
	}

	/**
	 * Find an existing Test Run in TestRail.
	 *
	 * @param projectId   the project ID
	 * @param timestamp   the timestamp to filter Test Runs
	 * @param testRunName the name of the Test Run
	 * @return an optional containing the existing Test Run if found
	 * @throws IOException if an I/O error occurs
	 */
	@NotNull
	private Optional<TestRun> findExistingTestRun(String projectId, String timestamp, String testRunName) throws IOException {
		TestRunResult testRunResult = testRailManager.getTestRuns(projectId, timestamp);
		return testRunResult.getTestRunList().stream()
				.filter(testRun -> testRun.getName().trim().equalsIgnoreCase(testRunName))
				.findFirst();
	}

	/**
	 * Use an existing Test Run in TestRail.
	 *
	 * @param existingTestRun the existing Test Run
	 * @throws IOException if an I/O error occurs
	 */
	private void useExistingTestRun(@NotNull TestRun existingTestRun) throws IOException {
		frameworkLogger.info("Use existing TestRun, Id: {}, Name: {}", existingTestRun.getId(), existingTestRun.getName());
		runTimeContext.setGlobalVariables(TEST_RUN_OBJECT, existingTestRun);

		String statusFilter = frameworkConfigs.getTestRailTestStatusFilter().replace(" ", "");
		List<TestRunTest> matchedTests = testRailManager.getAllTestRunTests(existingTestRun.getId(), statusFilter);
		runTimeContext.setGlobalVariables(FILTERED_TEST_OBJECT, matchedTests);
	}

	/**
	 * Create a new Test Run in TestRail.
	 *
	 * @param suite       the test suite
	 * @param projectId   the project ID
	 * @param testRunName the name of the Test Run
	 * @throws IOException if an I/O error occurs
	 */
	private void createNewTestRun(ISuite suite, String projectId, String testRunName) throws IOException {
		List<Integer> testCaseIdList = frameworkConfigs.isTestRailIncludeAllAutomatedTestCases() ?
				testRailManager.getAllAutomatedTestCases(projectId).stream().map(TestRunTest::getId).collect(Collectors.toList()) :
				getAllTestRailTestCases(suite);

		if (!testCaseIdList.isEmpty()) {
			TestRun testRun = testRailManager.addTestRun(projectId, testRunName, testCaseIdList);
			runTimeContext.setGlobalVariables(TEST_RUN_OBJECT, testRun);
			if (testRun != null && testRun.getId() > 0) {
				frameworkLogger.info("Test Run created in TestRail - Id: {}, Name: {}", testRun.getId(), testRun.getName());
			} else {
				frameworkLogger.error("Failed to create Test Run in TestRail.");
				throw new RuntimeException("Failed to create Test Run in TestRail.");
			}
		} else {
			frameworkLogger.warn("No Test Run created as no test cases were found.");
		}
	}

	/**
	 * Log consecutive failed test cases on master branch:
	 * 1. It will search the latest ${NOTIFICATION_TESTRUN_COUNT} test runs with format ${NOTIFICATION_FAILED_CASE_TESTRUN_PATTERN.regexp} within ${NOTIFICATION_FAILED_CASE_TESTRUN_WITHIN_DAYS} days based on the configuration.
	 * 2. For each test run, it will filter the failed case
	 * 3. Filter out which test cases are not in all test runs and remove it from the final list
	 * 4. Filter out which test cases are in the exclude list and remove it from the final list
	 */
	private void logConsecutiveFailedTestCase() {

		String projectId = frameworkConfigs.getTestRailProjectId();

		// Timestamp x days before set in the configuration
		LocalDate today = LocalDate.now(runTimeContext.getZoneId());
		String timestamp = String.valueOf(today.minusDays(frameworkConfigs.getFailedCaseTestRunWithinDays()).atStartOfDay(runTimeContext.getZoneId()).toEpochSecond());
		HashSet<Integer> failedTestCasesIdSet = new HashSet<>();
		boolean isFirstRunLoop = true;
		Map<Integer, String> finalResultMap = new HashMap<>();

		try {

			Pattern matchTestRunPattern = Pattern.compile(frameworkConfigs.getFailedCaseTestRunNotificationPattern());

			// Get the latest x test runs and match with pattern set in configuration
			List<TestRun> runs = testRailManager.getTestRuns(projectId, timestamp).getTestRunList().stream()
					.filter(testRun -> matchTestRunPattern.matcher(testRun.getName()).find())
					.limit(frameworkConfigs.getFailedCaseNotificationCount())
					.toList();

			// Search for consecutive failed test cases
			for (TestRun run : runs) {

				// Skip listing if the test run is in progress
				if (!testRailManager.getAllTestRunTests(run.getId(), String.valueOf(TestRailStatus.IN_PROGRESS)).isEmpty()) {
					return;
				}

				// Get all failed test cases
				Map<Integer, String> runResult = testRailManager.getAllTestRunTests(run.getId(), String.valueOf(TestRailStatus.Failed))
						.stream()
						.collect(Collectors.toMap(TestRunTest::getCaseId, TestRunTest::getTitle));

				if (isFirstRunLoop) {
					failedTestCasesIdSet.addAll(runResult.keySet());
					finalResultMap.putAll(runResult);
					isFirstRunLoop = false;
				} else {
					failedTestCasesIdSet.retainAll(runResult.keySet());
				}
			}

			// Remove those test that are failed first time only but not consecutive times
			finalResultMap.keySet().removeIf(testId -> !failedTestCasesIdSet.contains(testId));

			// Remove test case in exclude list
			HashSet<String> excludeTestCase = new HashSet<>(Arrays.asList(frameworkConfigs.getFailedCaseExcludeList().split(",")));
			finalResultMap.keySet().removeIf(testId -> excludeTestCase.contains(String.valueOf(testId)));


			// Display list
			if (!finalResultMap.isEmpty()) {
				frameworkLogger.info("Consecutive failed test cases: ");
				finalResultMap.forEach((k, v) -> frameworkLogger.info("Test Case ID: {}, Title: {}", k, v));
			} else {
				frameworkLogger.info("No consecutive failed test cases found.");
			}

		} catch (Exception e) {
			frameworkLogger.error("Failed to get consecutive failed test cases", e);
		}
	}
}