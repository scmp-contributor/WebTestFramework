package com.scmp.framework.services;

import com.scmp.framework.context.RunTimeContext;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.MutableCapabilities;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxOptions;
import org.openqa.selenium.remote.Browser;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.time.Duration;

import static com.scmp.framework.utils.Constants.*;

/**
 * WebDriverService - Manages the lifecycle of WebDriver instances for test execution.
 */
@Component
public class WebDriverService {
	private static final Logger frameworkLogger = LoggerFactory.getLogger(WebDriverService.class);
	private final ThreadLocal<RemoteWebDriver> remoteWebDriver = new ThreadLocal<>();
	private final RunTimeContext context;

	@Autowired
	public WebDriverService(RunTimeContext context) {
		this.context = context;
	}

	/**
	 * Get the current WebDriver instance.
	 *
	 * @return the current RemoteWebDriver instance
	 */
	public RemoteWebDriver getDriver() {
		return remoteWebDriver.get();
	}

	/**
	 * Set the current WebDriver instance.
	 *
	 * @param driver the RemoteWebDriver instance to set
	 */
	protected void setDriver(RemoteWebDriver driver) {
		remoteWebDriver.set(driver);
	}

	/**
	 * Start a new WebDriver instance based on the provided browser capabilities and screen dimensions.
	 *
	 * @param browser         the browser capabilities (e.g., ChromeOptions, FirefoxOptions)
	 * @param screenDimension the desired screen dimensions for the browser window
	 * @throws Exception if an error occurs while starting the WebDriver instance
	 */
	public void startDriverInstance(MutableCapabilities browser, Dimension screenDimension) throws Exception {
		RemoteWebDriver currentDriverSession;

		// For Execution Mode
		if (!context.isLocalExecutionMode()) {
			// Start a remote WebDriver session
			currentDriverSession = new RemoteWebDriver(new URI(context.getFrameworkConfigs().getHostUrl()).toURL(), browser);
		} else {
			// For Debug Mode, launch local driver
			if (browser.getBrowserName().equals(Browser.CHROME.browserName())) {
				frameworkLogger.info("Launching local Chrome Browser");
				System.setProperty(CHROME_DRIVER_SYSTEM_PROPERTY_NAME, context.getGlobalVariables(CHROME_DRIVER_PATH).toString());
				currentDriverSession = new ChromeDriver((ChromeOptions) browser);
			} else {
				frameworkLogger.info("Launching local Firefox Browser");
				System.setProperty(FIREFOX_DRIVER_SYSTEM_PROPERTY_NAME, context.getGlobalVariables(FIREFOX_DRIVER_PATH).toString());
				currentDriverSession = new FirefoxDriver((FirefoxOptions) browser);
			}
		}

		// Set implicit wait timeout
		currentDriverSession.manage().timeouts().implicitlyWait(Duration.ofSeconds(5));
		this.setDriver(currentDriverSession);

		// Set screen dimensions
		currentDriverSession.manage().window().setSize(screenDimension);
	}

	/**
	 * Stop the current WebDriver instance.
	 */
	public void stopWebDriver() {
		if (this.getDriver() != null) {
			this.getDriver().quit();
		}
	}
}