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
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
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
	 * Log consecutive failed test cases on master branch
	 * */
	private void logConsecutiveFailedTestCase(){

		String projectId = frameworkConfigs.getTestRailProjectId();

		// Timestamp 10 days before
		LocalDate today = LocalDate.now(runTimeContext.getZoneId());
		String timestamp = String.valueOf(today.minusDays(10).atStartOfDay(runTimeContext.getZoneId()).toEpochSecond());
		HashSet<Integer> failedTestCasesIdSet = new HashSet<>();
		boolean isFirstRunLoop = true;

		try {

			// Get the latest 3 test runs with the name containing "master"
			List<TestRun> runs = testRailManager.getTestRuns(projectId,timestamp).getTestRunList().stream()
					.filter(testRun -> testRun.getName().contains("master"))
					.limit(3)
					.toList();

			// Search for consecutive failed test cases
			for (TestRun run : runs){
				List<Integer> failedTests = testRailManager.getAllTestRunTests(run.getId(), String.valueOf(TestRailStatus.Failed))
						.stream()
						.map(TestRunTest::getCaseId)
						.toList();

				if(isFirstRunLoop){
					failedTestCasesIdSet.addAll(failedTests);
					isFirstRunLoop = false;
				}else{
					failedTestCasesIdSet.retainAll(failedTests);
				}
			}

			// Display list
			frameworkLogger.info("Consecutive failed test cases: {}", failedTestCasesIdSet);


		}catch (IOException e){
			throw new RuntimeException(e);
		}

	}
}