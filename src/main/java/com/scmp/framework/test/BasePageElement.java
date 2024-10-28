package com.scmp.framework.test;

import com.scmp.framework.context.ApplicationContextProvider;
import com.scmp.framework.context.RunTimeContext;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;

import java.lang.reflect.Field;
import java.time.Duration;

/**
 * BasePageElement - Abstract base class for common page element actions and utilities.
 */
public abstract class BasePageElement {
	private static final Logger frameworkLogger = LoggerFactory.getLogger(BasePageElement.class);
	private final RemoteWebDriver driver;
	public final RunTimeContext runTimeContext;

	/**
	 * Constructor to initialize the RemoteWebDriver and RunTimeContext.
	 *
	 * @param driver the RemoteWebDriver instance
	 */
	public BasePageElement(RemoteWebDriver driver) {
		this.driver = driver;
		ApplicationContext context = ApplicationContextProvider.getApplicationContext();
		runTimeContext = context.getBean(RunTimeContext.class);

		// Initialize page components
		Field[] fields = this.getClass().getFields();
		for (Field field : fields) {
			if (BasePageComponent.class.isAssignableFrom(field.getType())) {
				try {
					field.set(this, field.getType().getConstructors()[0].newInstance(driver));
				} catch (Exception e) {
					frameworkLogger.error("Error initializing page component: ", e);
				}
			}
		}
	}

	/**
	 * Pauses the execution for a specified duration.
	 *
	 * @param millis the duration to pause in milliseconds
	 */
	public void sleep(long millis) {
		try {
			Thread.sleep(millis);
		} catch (InterruptedException e) {
			frameworkLogger.error("Interrupted during sleep: ", e);
		}
	}

	/**
	 * Waits for a WebElement to be visible.
	 *
	 * @param element the WebElement to wait for
	 * @return true if the element is visible, false otherwise
	 */
	public boolean waitForVisible(WebElement element) {
		return waitForVisible(element, 60);
	}

	/**
	 * Waits for a WebElement to be visible within a specified duration.
	 *
	 * @param element the WebElement to wait for
	 * @param seconds the duration to wait in seconds
	 * @return true if the element is visible, false otherwise
	 */
	public boolean waitForVisible(WebElement element, long seconds) {
		try {
			new WebDriverWait(driver, Duration.ofSeconds(seconds)).until(ExpectedConditions.visibilityOf(element));
			return true;
		} catch (Exception e) {
			return false;
		}
	}

	/**
	 * Waits for a WebElement to be clickable.
	 *
	 * @param element the WebElement to wait for
	 * @return true if the element is clickable, false otherwise
	 */
	public boolean waitForElementToBeClickable(WebElement element) {
		return waitForElementToBeClickable(element, 15);
	}

	/**
	 * Waits for a WebElement to be clickable within a specified duration.
	 *
	 * @param element the WebElement to wait for
	 * @param seconds the duration to wait in seconds
	 * @return true if the element is clickable, false otherwise
	 */
	public boolean waitForElementToBeClickable(WebElement element, long seconds) {
		try {
			new WebDriverWait(driver, Duration.ofSeconds(seconds)).until(ExpectedConditions.elementToBeClickable(element));
			return true;
		} catch (Exception e) {
			return false;
		}
	}

	/**
	 * Waits for an element to disappear by its ID.
	 *
	 * @param id the ID of the element to wait for
	 */
	public void waitForElementToDisAppear(String id) {
		WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(15));
		wait.until(ExpectedConditions.invisibilityOfElementLocated(By.id(id)));
	}

	/**
	 * Waits for a WebElement to disappear.
	 *
	 * @param element the WebElement to wait for
	 * @return true if the element disappears, false otherwise
	 */
	public boolean waitForElementToDisAppear(WebElement element) {
		try {
			new WebDriverWait(driver, Duration.ofSeconds(15)).until(ExpectedConditions.invisibilityOf(element));
			return true;
		} catch (Exception e) {
			return false;
		}
	}

	/**
	 * Waits for a WebElement to load and returns it.
	 *
	 * @param element the WebElement to wait for
	 * @return the WebElement
	 */
	public WebElement waitForElement(WebElement element) {
		waitForElementToLoad(element);
		return element;
	}

	/**
	 * Checks if an iframe is loaded.
	 *
	 * @param element the iframe WebElement
	 * @return true if the iframe is loaded, false otherwise
	 */
	public boolean isIframeLoaded(WebElement element) {
		return this.isIframeLoaded(element, 5);
	}

	/**
	 * Checks if an iframe is loaded within a specified duration.
	 *
	 * @param element the iframe WebElement
	 * @param secondToWait the duration to wait in seconds
	 * @return true if the iframe is loaded, false otherwise
	 */
	public boolean isIframeLoaded(WebElement element, int secondToWait) {
		try {
			new WebDriverWait(driver, Duration.ofSeconds(secondToWait))
					.until(ExpectedConditions.frameToBeAvailableAndSwitchToIt(element));
			return true;
		} catch (Exception e) {
			return false;
		}
	}

	/**
	 * Checks if a WebElement is displayed.
	 *
	 * @param element the WebElement to check
	 * @return true if the element is displayed, false otherwise
	 */
	public boolean isElementDisplayed(WebElement element) {
		return waitForVisible(element, 3);
	}

	/**
	 * Waits for a WebElement to load.
	 *
	 * @param element the WebElement to wait for
	 * @return true if the element is loaded, false otherwise
	 */
	public boolean waitForElementToLoad(WebElement element) {
		return waitForElementToBeClickable(element, 15);
	}

	/**
	 * Waits for a WebElement to load within a specified duration.
	 *
	 * @param element the WebElement to wait for
	 * @param secondsToWait the duration to wait in seconds
	 * @return true if the element is loaded, false otherwise
	 */
	public boolean waitForElementToLoad(WebElement element, long secondsToWait) {
		return waitForElementToBeClickable(element, secondsToWait);
	}
}