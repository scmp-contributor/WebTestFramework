package com.scmp.framework.test;

import com.scmp.framework.context.ApplicationContextProvider;
import com.scmp.framework.context.RunTimeContext;
import com.scmp.framework.testng.model.TestInfo;
import lombok.Getter;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.html5.LocalStorage;
import org.openqa.selenium.html5.WebStorage;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Map;

import static com.scmp.framework.utils.Constants.TEST_INFO_OBJECT;

/**
 * BaseApp - Abstract base class for application-specific actions and utilities.
 */
public abstract class BaseApp {
	private static final Logger frameworkLogger = LoggerFactory.getLogger(BaseApp.class);

	@Getter
	private final RemoteWebDriver driver;
	private final RunTimeContext runTimeContext;

	/**
	 * Constructor to initialize the RemoteWebDriver and RunTimeContext.
	 *
	 * @param driver the RemoteWebDriver instance
	 */
	public BaseApp(RemoteWebDriver driver) {
		this.driver = driver;

		ApplicationContext context = ApplicationContextProvider.getApplicationContext();
		runTimeContext = context.getBean(RunTimeContext.class);
	}

	/**
	 * Initializes the application by setting up page objects.
	 */
	protected void initApp() {
		Field[] fields = this.getClass().getDeclaredFields();
		for (Field field : fields) {
			if (BasePage.class.isAssignableFrom(field.getType())) {
				try {
					field.set(this, field.getType().getConstructors()[0].newInstance(driver));
				} catch (Exception e) {
					frameworkLogger.error("Error initializing page object: ", e);
				}
			}
		}
	}

	/**
	 * Refreshes the current page.
	 */
	public void refresh() {
		this.getDriver().navigate().refresh();
	}

	/**
	 * Navigates to a specified path relative to the current URL.
	 *
	 * @param path the path to navigate to
	 */
	public void navigateTo(String path) {
		this.getDriver().navigate().to(this.getURL() + path);
	}

	/**
	 * Retrieves the current URL without a trailing slash.
	 *
	 * @return the current URL
	 */
	public String getURL() {
		String url = this.driver.getCurrentUrl();
		if (url.endsWith("/")) {
			return url.substring(0, url.length() - 1);
		}
		return url;
	}

	/**
	 * Performs a mouse click on a specified WebElement.
	 *
	 * @param element the WebElement to click
	 */
	public void mouseClick(WebElement element) {
		new Actions(this.getDriver()).moveToElement(element).click().perform();
	}

	/**
	 * Performs a JavaScript click on a specified WebElement.
	 *
	 * @param element the WebElement to click
	 */
	public void javascriptClick(WebElement element) {
		this.getDriver().executeScript("arguments[0].click();", element);
	}

	/**
	 * Moves the mouse focus to a specified WebElement.
	 *
	 * @param element the WebElement to focus on
	 */
	public void mouseFocus(WebElement element) {
		new Actions(this.getDriver()).moveToElement(element).perform();
	}

	/**
	 * Executes a JavaScript script with optional arguments.
	 *
	 * @param script    the JavaScript script to execute
	 * @param arguments the arguments for the script
	 */
	public void executeScript(String script, Object... arguments) {
		this.getDriver().executeScript(script, arguments);
	}

	/**
	 * Switches to a browser tab by its sequence number.
	 *
	 * @param tabSequence the sequence number of the tab to switch to
	 */
	public void switchToTab(int tabSequence) {
		ArrayList<String> tabs = new ArrayList<>(driver.getWindowHandles());
		driver.switchTo().window(tabs.get(tabSequence));
	}

	/**
	 * Loads local storage items from the test context.
	 */
	public void loadLocalStorageItems() {
		TestInfo testInfo = (TestInfo) runTimeContext.getTestLevelVariables(TEST_INFO_OBJECT);
		this.setLocalStorageItems(testInfo.getCustomLocalStorage());
	}

	/**
	 * Sets local storage items from a given data map.
	 *
	 * @param dataMap the data map containing key-value pairs to set in local storage
	 */
	public void setLocalStorageItems(Map<String, String> dataMap) {
		LocalStorage local = ((WebStorage) this.driver).getLocalStorage();
		for (String key : dataMap.keySet()) {
			local.setItem(key, dataMap.get(key));
		}
	}

	/**
	 * Retrieves a local storage item by its key.
	 *
	 * @param key the key of the local storage item
	 * @return the value of the local storage item
	 */
	public String getLocalStorageItem(String key) {
		LocalStorage local = ((WebStorage) this.driver).getLocalStorage();
		return local.getItem(key);
	}

	/**
	 * Sets a local storage item by its key and value.
	 *
	 * @param key   the key of the local storage item
	 * @param value the value of the local storage item
	 */
	public void setLocalStorageItem(String key, String value) {
		LocalStorage local = ((WebStorage) this.driver).getLocalStorage();
		local.setItem(key, value);
	}

	/**
	 *
	 * @param key the key of the local storage item
	 * @return the removed value of the local storage item
	 */
	public String removeLocalStorageItem(String key) {
		LocalStorage local = ((WebStorage) this.driver).getLocalStorage();
		return local.removeItem(key);
	}
}