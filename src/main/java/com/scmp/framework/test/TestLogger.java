package com.scmp.framework.test;

import com.scmp.framework.services.ReportService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.testng.Assert;

/**
 * TestLogger - Utility class for logging test information and capturing screenshots.
 */
@Component
public class TestLogger {

	private static final Logger frameworkLogger = LoggerFactory.getLogger(TestLogger.class);
	private final ReportService reportService;

	@Autowired
	public TestLogger(ReportService reportService) {
		this.reportService = reportService;
	}

	/**
	 * Captures a screenshot and returns the file path.
	 *
	 * @return the file path of the captured screenshot
	 */
	public String captureScreen() {
		return reportService.captureScreenShot();
	}

	/**
	 * Retrieves the image path for a given image name.
	 *
	 * @param imageName the name of the image
	 * @return the file path of the image
	 */
	public String getImagePath(String imageName) {
		return reportService.getImagePath(imageName);
	}

	/**
	 * Attaches an image to the report.
	 *
	 * @param image the file path of the image
	 */
	public void attachImage(String image) {
		reportService.attachImage(image);
	}

	/**
	 * Logs a screenshot and returns the file path.
	 *
	 * @return the file path of the logged screenshot
	 */
	public String logScreenshot() {
		return reportService.logScreenshot();
	}

	/**
	 * Logs an informational message.
	 *
	 * @param message the message to log
	 */
	public void logInfo(String message) {
		frameworkLogger.info(message);
		reportService.logInfo(message);
	}

	/**
	 * Logs an informational message with a screenshot.
	 *
	 * @param message the message to log
	 */
	public void logInfoWithScreenshot(String message) {
		frameworkLogger.info(message);
		reportService.logInfoWithScreenshot(message);
	}

	/**
	 * Logs a pass message.
	 *
	 * @param message the message to log
	 */
	public void logPass(String message) {
		frameworkLogger.info("[PASSED] {}", message);
		reportService.logPass(message);
	}

	/**
	 * Logs a pass message with a screenshot.
	 *
	 * @param message the message to log
	 */
	public void logPassWithScreenshot(String message) {
		frameworkLogger.info("[PASSED] {}", message);
		reportService.logPassWithScreenshot(message);
	}

	/**
	 * Logs a fail message.
	 *
	 * @param message the message to log
	 */
	public void logFail(String message) {
		frameworkLogger.error("[FAILED] {}", message);
		reportService.logFail(message);
	}

	/**
	 * Logs a fail message without a screenshot.
	 *
	 * @param message the message to log
	 */
	public void logFailWithoutScreenshot(String message) {
		frameworkLogger.error("[FAILED] {}", message);
		reportService.logFailWithoutScreenshot(message);
	}

	/**
	 * Logs a fail message with an image.
	 *
	 * @param message   the message to log
	 * @param imagePath the file path of the image
	 */
	public void logFailWithImage(String message, String imagePath) {
		frameworkLogger.error("[FAILED] {}", message);
		reportService.logFailWithImage(message, imagePath);
	}

	/**
	 * Logs a fatal error message and fails the test.
	 *
	 * @param message the message to log
	 */
	public void logFatalError(String message) {
		frameworkLogger.error("[ERROR] {}", message);
		Assert.fail(message);
	}

	/**
	 * Logs a JSON string to the report.
	 *
	 * @param json     the JSON string to log
	 * @param fileName the name of the file to save the JSON
	 */
	public void logJson(String json, String fileName) {
		frameworkLogger.info(json);
		reportService.logJson(json, fileName);
	}
}