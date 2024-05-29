package com.scmp.framework.testrail;

import com.scmp.framework.testrail.models.Attachment;
import com.scmp.framework.testrail.models.CustomStepResult;
import com.scmp.framework.testrail.models.TestResult;
import com.scmp.framework.testrail.models.TestRun;
import com.scmp.framework.testrail.models.requests.AddTestResultRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class TestRailDataService {
	private static final Logger frameworkLogger = LoggerFactory.getLogger(TestRailDataService.class);

	private final int testcaseId;
	private final TestRun testRun;
	private boolean isTestResultForUploadAttachmentsReady = false;
	private TestResult testResultForUploadAttachments;
	private final List<CustomStepResult> testRailCustomStepResultList = new ArrayList<>();
	private final ExecutorService taskExecuterService = Executors.newFixedThreadPool(5);

	@Autowired
	private TestRailManager testRailManager;

	public TestRailDataService(int testcaseId, TestRun testRun) {
		this.testcaseId = testcaseId;
		this.testRun = testRun;
		this.initTestResultForUploadAttachments();
	}

	private void initTestResultForUploadAttachments() {
		this.taskExecuterService.submit(() -> {
			// Create a new test result for adding attachment
			String comment = "Mark In Progress Status";
			AddTestResultRequest request =
					new AddTestResultRequest(TestRailStatus.IN_PROGRESS, comment, "", new ArrayList<>());
			try {
				this.testResultForUploadAttachments =
						testRailManager.addTestResult(this.testRun.getId(), this.testcaseId, request);

				this.isTestResultForUploadAttachmentsReady = true;
			} catch (IOException e) {
				frameworkLogger.error("Failed to create test result.", e);
			}
		});
	}

	/**
	 * Add Test Step Result
	 *
	 * @param status
	 * @param content
	 * @param filePath
	 */
	public void addStepResult(int status, String content, String filePath) {
		final CustomStepResult stepResult = new CustomStepResult(content, status);
		testRailCustomStepResultList.add(stepResult);

		if (filePath!=null) {
			this.taskExecuterService.submit(() -> {
				try {
					// Wait for test result for attachment ready
					int maxWaitSeconds = 20;
					int seconds = 0;
					while (!this.isTestResultForUploadAttachmentsReady && seconds < maxWaitSeconds) {
						try {
							seconds++;
							TimeUnit.SECONDS.sleep(1);
						} catch (InterruptedException e) {
							frameworkLogger.error("Ops!", e);
						}
					}

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
	 * Upload data to Test Rail
	 *
	 * @param finalTestResult
	 * @param elapsedInSecond
	 */
	public void uploadDataToTestRail(int finalTestResult, long elapsedInSecond) {
		// Shut down the ExecutorService to reject new tasks
		taskExecuterService.shutdown();
		// Wait for pending tasks to complete
		try {
			boolean terminated = taskExecuterService.awaitTermination(20, TimeUnit.SECONDS);
		} catch (InterruptedException e) {
			frameworkLogger.error("Ops!", e);
		}

//    frameworkLogger.info("========RESULT CONTENT==========");
//    testRailCustomStepResultList.forEach(result -> {frameworkLogger.info(result.getContent());});
//    frameworkLogger.info("================================");

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
