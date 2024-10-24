package com.scmp.framework.testrail;

import com.scmp.framework.context.ApplicationContextProvider;
import com.scmp.framework.testrail.models.Attachment;
import com.scmp.framework.testrail.models.CustomStepResult;
import com.scmp.framework.testrail.models.TestResult;
import com.scmp.framework.testrail.models.TestRun;
import com.scmp.framework.testrail.models.requests.AddTestResultRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * TestRailDataService - Manages the interaction with TestRail for uploading test results and attachments.
 */
public class TestRailDataService {
	private static final Logger frameworkLogger = LoggerFactory.getLogger(TestRailDataService.class);

	private final int testcaseId;
	private final TestRun testRun;
	private TestResult testResultForUploadAttachments;
	private final List<CustomStepResult> testRailCustomStepResultList = new ArrayList<>();
	private final ExecutorService taskExecuterService = Executors.newFixedThreadPool(5);
	private final CountDownLatch initializationLatch = new CountDownLatch(1);
	private TestRailManager testRailManager;

	public TestRailDataService(int testcaseId, TestRun testRun) {
		this.testcaseId = testcaseId;
		this.testRun = testRun;
		this.initTestResultForUploadAttachments();
	}

	/**
	 * Initialize the test result for uploading attachments.
	 */
	private void initTestResultForUploadAttachments() {
		ApplicationContext context = ApplicationContextProvider.getApplicationContext();
		this.testRailManager = context.getBean(TestRailManager.class);

		this.taskExecuterService.submit(() -> {
			// Create a new test result for adding attachment
			String comment = "Mark In Progress Status";
			AddTestResultRequest request =
					new AddTestResultRequest(TestRailStatus.IN_PROGRESS, comment, "", new ArrayList<>());
			try {
				this.testResultForUploadAttachments =
						testRailManager.addTestResult(this.testRun.getId(), this.testcaseId, request);

				// Signal that initialization is complete
				initializationLatch.countDown();
			} catch (IOException e) {
				frameworkLogger.error("Failed to create test result.", e);
			}
		});
	}

	/**
	 * Add a test step result to the list and upload the attachment if provided.
	 *
	 * @param status   the status of the test step
	 * @param content  the content of the test step
	 * @param filePath the file path of the attachment
	 */
	public void addStepResult(int status, String content, String filePath) {
		final CustomStepResult stepResult = new CustomStepResult(content, status);
		testRailCustomStepResultList.add(stepResult);

		if (filePath != null) {
			this.taskExecuterService.submit(() -> {
				try {
					// Wait for test result for attachment to be ready
					initializationLatch.await();

					frameworkLogger.info("Uploading attachment: " + filePath);
					Attachment attachment =
							testRailManager.addAttachmentToTestResult(testResultForUploadAttachments.getId(), filePath);

					String attachmentRef =
							String.format(Attachment.ATTACHMENT_REF_STRING, attachment.getAttachmentId());
					frameworkLogger.info("Attachment uploaded: " + attachmentRef);
					stepResult.setContent(stepResult.getContent() + " \n " + attachmentRef);
				} catch (Exception e) {
					frameworkLogger.error("Failed to upload attachment.", e);
				}
			});
		}
	}

	/**
	 * Upload the collected test data to TestRail.
	 *
	 * @param finalTestResult the final status of the test
	 * @param elapsedInSecond the elapsed time of the test in seconds
	 */
	public void uploadDataToTestRail(int finalTestResult, long elapsedInSecond) {
		// Shut down the ExecutorService to reject new tasks
		taskExecuterService.shutdown();
		// Wait for pending tasks to complete
		try {
			boolean terminated = taskExecuterService.awaitTermination(60, TimeUnit.SECONDS);
			if (!terminated) {
				// Timeout occurred before all tasks completed
				frameworkLogger.warn("Timeout occurred. Not all tasks completed.");
			} else {
				// All tasks completed within the timeout duration
				frameworkLogger.info("All tasks completed.");
			}
		} catch (InterruptedException e) {
			frameworkLogger.error("Ops!", e);
		}

		AddTestResultRequest request =
				new AddTestResultRequest(
						finalTestResult, "", elapsedInSecond + "s", testRailCustomStepResultList);
		try {
			testRailManager.addTestResult(this.testRun.getId(), this.testcaseId, request);
		} catch (IOException e) {
			frameworkLogger.error("Failed to create test result.", e);
		}
	}
}