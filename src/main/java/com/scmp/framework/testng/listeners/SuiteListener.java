// src/main/java/com/scmp/framework/testng/listeners/SuiteListener.java
package com.scmp.framework.testng.listeners;

import com.scmp.framework.annotations.testrail.TestRailTestCase;
import com.scmp.framework.context.ApplicationContextProvider;
import com.scmp.framework.context.FrameworkConfigs;
import com.scmp.framework.context.RunTimeContext;
import com.scmp.framework.testrail.TestRailManager;
import com.scmp.framework.testrail.models.TestRun;
import com.scmp.framework.testrail.models.TestRunResult;
import com.scmp.framework.testrail.models.TestRunTest;
import com.scmp.framework.utils.Constants;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.testng.ISuite;
import org.testng.ISuiteListener;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

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
		frameworkLogger.info("Test Suite execution completed.");
	}

	@Override
	public void onStart(ISuite suite) {
		if (frameworkConfigs.isTestRailUploadTestResult()) {
			try {
				setupTestRun(suite);
			} catch (IOException e) {
				frameworkLogger.error(Constants.TEST_RUN_CREATION_FAILED_MESSAGE, e);
				throw new RuntimeException(Constants.TEST_RUN_CREATION_FAILED_MESSAGE, e);
			} catch (Exception e) {
				frameworkLogger.error("Unexpected error occurred while creating Test Run in TestRail.", e);
				throw new RuntimeException("Unexpected error occurred while creating Test Run in TestRail.", e);
			}
		}
	}

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

	private void setupTestRun(ISuite suite) throws IOException {
		frameworkLogger.info("Creating Test Run in TestRail...");

		String projectId = frameworkConfigs.getTestRailProjectId();
		validateProjectId(projectId);

		LocalDate today = LocalDate.now(runTimeContext.getZoneId());
		String timestamp = String.valueOf(today.minusDays(7).atStartOfDay(runTimeContext.getZoneId()).toEpochSecond());
		String testRunName = frameworkConfigs.getTestRailTestRunName().isEmpty() ? String.format(Constants.DEFAULT_TEST_RUN_NAME, today) : frameworkConfigs.getTestRailTestRunName().trim();

		if (!frameworkConfigs.isTestRailCreateNewTestRun()) {
			Optional<TestRun> existingTestRun = findExistingTestRun(projectId, timestamp, testRunName);
			if (existingTestRun.isPresent()) {
				useExistingTestRun(existingTestRun.get());
				return;
			}
		}

		createNewTestRun(suite, projectId, testRunName);
	}

	private void validateProjectId(String projectId) {
		if (projectId == null || !Pattern.compile("[0-9]+").matcher(projectId).matches()) {
			throw new IllegalArgumentException(String.format(Constants.INVALID_PROJECT_ID_MESSAGE, projectId));
		}
	}

	@NotNull
	private Optional<TestRun> findExistingTestRun(String projectId, String timestamp, String testRunName) throws IOException {
		TestRunResult testRunResult = testRailManager.getTestRuns(projectId, timestamp);
		return testRunResult.getTestRunList().stream()
				.filter(testRun -> testRun.getName().trim().equalsIgnoreCase(testRunName))
				.findFirst();
	}

	private void useExistingTestRun(@NotNull TestRun existingTestRun) throws IOException {
		frameworkLogger.info("Use existing TestRun, Id: {}, Name: {}", existingTestRun.getId(), existingTestRun.getName());
		runTimeContext.setGlobalVariables(Constants.TEST_RUN_OBJECT, existingTestRun);

		String statusFilter = frameworkConfigs.getTestRailTestStatusFilter().replace(" ", "");
		List<TestRunTest> matchedTests = testRailManager.getAllTestRunTests(existingTestRun.getId(), statusFilter);
		runTimeContext.setGlobalVariables(Constants.FILTERED_TEST_OBJECT, matchedTests);
	}

	private void createNewTestRun(ISuite suite, String projectId, String testRunName) throws IOException {
		List<Integer> testCaseIdList = frameworkConfigs.isTestRailIncludeAllAutomatedTestCases() ?
				testRailManager.getAllAutomatedTestCases(projectId).stream().map(TestRunTest::getId).collect(Collectors.toList()) :
				getAllTestRailTestCases(suite);

		if (!testCaseIdList.isEmpty()) {
			TestRun testRun = testRailManager.addTestRun(projectId, testRunName, testCaseIdList);
			runTimeContext.setGlobalVariables(Constants.TEST_RUN_OBJECT, testRun);
			if (testRun != null && testRun.getId() > 0) {
				frameworkLogger.info("Test Run created in TestRail - Id: {}, Name: {}", testRun.getId(), testRun.getName());
			} else {
				frameworkLogger.error(Constants.TEST_RUN_CREATION_FAILED_MESSAGE);
				throw new RuntimeException(Constants.TEST_RUN_CREATION_FAILED_MESSAGE);
			}
		} else {
			frameworkLogger.warn(Constants.TEST_RUN_NOT_CREATED_MESSAGE);
		}
	}
}