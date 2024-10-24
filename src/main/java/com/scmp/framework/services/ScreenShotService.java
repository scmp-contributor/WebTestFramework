package com.scmp.framework.services;

import com.aventstack.extentreports.Status;
import com.scmp.framework.context.RunTimeContext;
import org.apache.commons.io.FileUtils;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

/**
 * ScreenShotService - Handles capturing and saving screenshots during test execution.
 */
@Component
public class ScreenShotService {
	private static final Logger frameworkLogger = LoggerFactory.getLogger(ScreenShotService.class);

	private final RunTimeContext context;
	private final WebDriverService webDriverService;

	@Autowired
	public ScreenShotService(RunTimeContext context, WebDriverService webDriverService) {
		this.webDriverService = webDriverService;
		this.context = context;
	}

	/**
	 * Capture a screenshot and save it to the target directory.
	 *
	 * @param status     the status of the test (e.g., PASS, FAIL)
	 * @param className  the name of the test class
	 * @param methodName the name of the test method
	 * @return the path to the saved screenshot
	 */
	public synchronized String captureScreenShot(Status status, String className, String methodName) {
		// If driver is not setup properly
		if (webDriverService.getDriver() == null) {
			frameworkLogger.warn("WebDriver is not initialized.");
			return "";
		}

		// Capture the screenshot
		File scrFile = ((TakesScreenshot) webDriverService.getDriver()).getScreenshotAs(OutputType.FILE);
		String screenShotNameWithTimeStamp = currentDateAndTime();

		// Save the screenshot to the target directory
		return copyScreenshotToTarget(status, scrFile, methodName, className, screenShotNameWithTimeStamp);
	}

	/**
	 * Capture a screenshot with a given name.
	 *
	 * @param screenShotName the name of the screenshot
	 * @return the path to the saved screenshot
	 */
	public String captureScreenShot(String screenShotName) {
		String className = new Exception().getStackTrace()[1].getClassName();
		return captureScreenShot(Status.INFO, className, screenShotName);
	}

	/**
	 * Get the path to save a screenshot.
	 *
	 * @param className    the name of the test class
	 * @param methodName   the name of the test method
	 * @param snapshotName the name of the snapshot
	 * @return the path to save the screenshot
	 */
	public String getScreenshotPath(String className, String methodName, String snapshotName) {
		String filePath = this.context.getLogPath("screenshot", className, methodName);
		String screenShotNameWithTimeStamp = currentDateAndTime();
		return filePath + File.separator + screenShotNameWithTimeStamp + "_" + methodName + "_" + snapshotName + ".png";
	}

	/**
	 * Get the current date and time formatted as a string.
	 *
	 * @return the current date and time formatted as a string
	 */
	private String currentDateAndTime() {
		LocalDateTime now = LocalDateTime.now();
		DateTimeFormatter dtf = DateTimeFormatter.ISO_DATE_TIME;
		return now.truncatedTo(ChronoUnit.SECONDS).format(dtf).replace(":", "-");
	}

	/**
	 * Copy the screenshot to the target directory.
	 *
	 * @param status                      the status of the test (e.g., PASS, FAIL)
	 * @param scrFile                     the screenshot file
	 * @param methodName                  the name of the test method
	 * @param className                   the name of the test class
	 * @param screenShotNameWithTimeStamp the timestamped name of the screenshot
	 * @return the path to the saved screenshot
	 */
	private String copyScreenshotToTarget(Status status, File scrFile, String methodName, String className, String screenShotNameWithTimeStamp) {
		String filePath = this.context.getLogPath("screenshot", className, methodName);
		String fileName = screenShotNameWithTimeStamp + "_" + methodName + (status == Status.FAIL ? "_failed.png" : "_results.png");
		String fullPath = filePath + File.separator + fileName;

		try {
			FileUtils.copyFile(scrFile, new File(fullPath.trim()));
			frameworkLogger.info("Screenshot saved to: {}", fullPath);
			return fullPath.trim();
		} catch (IOException e) {
			frameworkLogger.error("Error copying screenshot to target: ", e);
			return "";
		}
	}
}