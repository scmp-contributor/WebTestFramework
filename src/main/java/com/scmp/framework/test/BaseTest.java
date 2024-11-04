package com.scmp.framework.test;

import com.scmp.framework.context.ApplicationContextProvider;
import com.scmp.framework.context.RunTimeContext;
import com.scmp.framework.services.WebDriverService;
import lombok.Getter;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;

import java.util.Random;

/**
 * BaseTest - Base class for all test classes providing common functionalities.
 */
public class BaseTest {
	private static final Logger frameworkLogger = LoggerFactory.getLogger(BaseTest.class);
	protected final TestLogger logger;
	@Getter
	private final RunTimeContext runTimeContext;
	private final WebDriverService webDriverService;

	/**
	 * Constructor to initialize the context, logger, runtime context, and WebDriver service.
	 */
	public BaseTest() {
		ApplicationContext context = ApplicationContextProvider.getApplicationContext();
		logger = context.getBean(TestLogger.class);
		runTimeContext = context.getBean(RunTimeContext.class);
		webDriverService = context.getBean(WebDriverService.class);
	}

	/**
	 * Retrieves the RemoteWebDriver instance.
	 *
	 * @return the RemoteWebDriver instance
	 */
	public RemoteWebDriver getDriver() {
		return webDriverService.getDriver();
	}

	/**
	 * Generates a random number string of a specified length.
	 *
	 * @param length the length of the random number string
	 * @return the generated random number string
	 */
	public String getRandomNumberString(int length) {
		StringBuilder output = new StringBuilder();
		Random random = new Random();

		for (int i = 0; i < length; i++) {
			output.append(random.nextInt(10));
		}

		frameworkLogger.info("Generated random Number String: {}", output);
		return output.toString();
	}

	/**
	 * Pauses the execution for a specified duration.
	 *
	 * @param millis the duration to pause in milliseconds
	 */
	public void sleep(long millis) {
		try {
			frameworkLogger.info("Wait for {} milliseconds", millis);
			Thread.sleep(millis);
		} catch (InterruptedException e) {
			frameworkLogger.error("Interrupted during sleep: ", e);
		}
	}
}