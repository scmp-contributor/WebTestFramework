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

	public synchronized String captureScreenShot(Status status, String className, String methodName) {
		// If driver is not setup properly
		if (webDriverService.getDriver()==null) {
			return "";
		}

		File scrFile =
				((TakesScreenshot) webDriverService.getDriver()).getScreenshotAs(OutputType.FILE);
		String screenShotNameWithTimeStamp = currentDateAndTime();

		return copyScreenshotToTarget(
				status, scrFile, methodName, className, screenShotNameWithTimeStamp);
	}

	public String captureScreenShot(String screenShotName) {
		String className = new Exception().getStackTrace()[1].getClassName();

		return captureScreenShot(Status.INFO, className, screenShotName);
	}

	public String getScreenshotPath(String className, String methodName, String snapshotName) {
		String filePath = this.context.getLogPath("screenshot", className, methodName);
		String screenShotNameWithTimeStamp = currentDateAndTime();

		return filePath
				+ File.separator
				+ screenShotNameWithTimeStamp
				+ "_"
				+ methodName
				+ "_"
				+ snapshotName
				+ ".png";
	}

	private String currentDateAndTime() {
		LocalDateTime now = LocalDateTime.now();
		DateTimeFormatter dtf = DateTimeFormatter.ISO_DATE_TIME;
		return now.truncatedTo(ChronoUnit.SECONDS).format(dtf).replace(":", "-");
	}

	private String copyScreenshotToTarget(
			Status status,
			File scrFile,
			String methodName,
			String className,
			String screenShotNameWithTimeStamp) {
		String filePath = this.context.getLogPath("screenshot", className, methodName);

		try {
			if (status==Status.FAIL) {
				String failedScreen =
						filePath
								+ File.separator
								+ screenShotNameWithTimeStamp
								+ "_"
								+ methodName
								+ "_failed.png";
				FileUtils.copyFile(scrFile, new File(failedScreen.trim()));

				return failedScreen.trim();
			} else {
				String capturedScreen =
						filePath
								+ File.separator
								+ screenShotNameWithTimeStamp
								+ "_"
								+ methodName
								+ "_results.png";
				FileUtils.copyFile(scrFile, new File(capturedScreen.trim()));

				return capturedScreen.trim();
			}
		} catch (IOException e) {
			frameworkLogger.error("Ops!", e);
		}

		return "";
	}
}
