package com.scmp.framework.test;

import com.scmp.framework.testng.model.TestInfo;
import com.scmp.framework.utils.HTMLTags;
import lombok.Getter;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.html5.LocalStorage;
import org.openqa.selenium.html5.WebStorage;
import org.openqa.selenium.remote.RemoteExecuteMethod;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.openqa.selenium.remote.html5.RemoteWebStorage;

import java.util.Map;

import static com.scmp.framework.utils.Constants.TEST_INFO_OBJECT;

/**
 * BasePage - Abstract base class for page-specific actions and utilities.
 */
public abstract class BasePage extends BasePageElement {

	@Getter
	private final RemoteWebDriver driver;
	private String viewSelector;
	private int screenHeight = -1;
	private int screenWidth = -1;
	public String PATH = "";

	/**
	 * Constructor to initialize the RemoteWebDriver and view selector.
	 *
	 * @param driver       the RemoteWebDriver instance
	 * @param viewSelector the CSS selector for the view
	 */
	public BasePage(RemoteWebDriver driver, String viewSelector) {
		super(driver);
		this.driver = driver;
		this.viewSelector = viewSelector;
	}

	/**
	 * Launches the page and waits for it to load.
	 */
	public void launch() {
		this.getDriver().get(this.getURL());
		this.waitForPageLoad();
	}

	/**
	 * Launches the page with a specified path and waits for it to load.
	 *
	 * @param path the path to navigate to
	 */
	public void launch(String path) {
		this.PATH = path;
		this.launch();
	}

	/**
	 * Launches the page with setups and waits for it to load.
	 *
	 * @param path the path to navigate to
	 */
	public void launchWithSetups(String path) {
		this.PATH = path;
		this.launchWithSetups();
	}

	/**
	 * Launches the page without waiting for it to load.
	 */
	public void launchWithoutWaiting() {
		this.getDriver().get(this.getURL());
	}

	/**
	 * Launches the page with setups, performs post-launch actions, and waits for it to load.
	 */
	public void launchWithSetups() {
		this.getDriver().get(this.getURL());
		this.postLaunchActions();

		// Reload the page
		this.getDriver().get(this.getURL());
		this.waitForPageLoad();
	}

	/**
	 * Performs post-launch actions such as loading local storage items.
	 */
	public void postLaunchActions() {
		this.loadLocalStorageItems();
	}

	/**
	 * Retrieves the path without a trailing slash.
	 *
	 * @return the path
	 */
	public String getPath() {
		String path = this.PATH;
		if (path.endsWith("/")) {
			return path.substring(0, path.length() - 1);
		}
		return path;
	}

	/**
	 * Retrieves the current URL without a trailing slash.
	 *
	 * @return the current URL
	 */
	public String getURL() {
		String url = this.runTimeContext.getURL() + this.PATH;
		if (url.endsWith("/")) {
			return url.substring(0, url.length() - 1);
		}
		return url;
	}

	/**
	 * Abstract method to wait for the page to load.
	 *
	 * @return true if the page is loaded, false otherwise
	 */
	public abstract boolean waitForPageLoad();

	/**
	 * Scrolls to the top of the view.
	 */
	public void scrollToTop() {
		this.getDriver().executeScript(
      """
				var cssSelector = arguments[0];
				var element = document.querySelector(cssSelector);
				element.scrollTo(0, 0);
				""",
				this.viewSelector
		);
		sleep(500);
	}

	/**
	 * Scrolls to a specified WebElement.
	 *
	 * @param element the WebElement to scroll to
	 * @return true if the element is found and scrolled to, false otherwise
	 */
	public boolean scrollToElement(WebElement element) {
		return this.scrollToElement(element, 30);
	}

	/**
	 * Scrolls to a specified WebElement with a maximum scroll count.
	 *
	 * @param element       the WebElement to scroll to
	 * @param maxScrollCount the maximum number of scroll attempts
	 * @return true if the element is found and scrolled to, false otherwise
	 */
	public boolean scrollToElement(WebElement element, int maxScrollCount) {
		int scrollCounter = 0;
		int scrollTopB4Scroll = this.getScrollTop(this.viewSelector);
		int elementPosition = -1;

		while (scrollCounter < maxScrollCount) {
			scrollCounter++;

			if (this.waitForVisible(element, 1)) {
				elementPosition = element.getLocation().getY();
			}

			if (elementPosition != -1 && elementPosition < this.getScreenHeight()) {
				this.scrollDownBy(elementPosition - this.getScreenHeight() / 2);
				return true; // Element on screen
			} else {
				scrollDown();
				int scrollTopAfterScroll = this.getScrollTop(this.viewSelector);

				if (scrollTopB4Scroll == scrollTopAfterScroll) {
					return false; // Reached bottom of the div
				} else {
					scrollTopB4Scroll = scrollTopAfterScroll;
				}
			}
		}
		return false; // Element not found
	}

	/**
	 * Scrolls up by three-quarters of the screen height.
	 */
	public void scrollUp() {
		scroll(this.viewSelector, -this.getScreenHeight() * 3 / 4);
	}

	/**
	 * Scrolls up by a specified delta.
	 *
	 * @param delta the amount to scroll up
	 */
	public void scrollUp(int delta) {
		this.scroll(this.viewSelector, -delta);
	}

	/**
	 * Scrolls down by three-quarters of the screen height.
	 */
	public void scrollDown() {
		scroll(this.viewSelector, this.getScreenHeight() * 3 / 4);
	}

	/**
	 * Scrolls down a specified number of times.
	 *
	 * @param times the number of times to scroll down
	 */
	public void scrollDown(int times) {
		for (int i = 0; i < times; i++) {
			scrollDown();
		}
	}

	/**
	 * Scrolls down a specified number of times with a wait time between each scroll.
	 *
	 * @param times            the number of times to scroll down
	 * @param waitInMilliSecond the wait time in milliseconds between each scroll
	 */
	public void scrollDown(int times, int waitInMilliSecond) {
		for (int i = 0; i < times; i++) {
			scrollDown();
			sleep(waitInMilliSecond);
		}
	}

	/**
	 * Scrolls down by a specified delta.
	 *
	 * @param delta the amount to scroll down
	 */
	public void scrollDownBy(int delta) {
		this.scroll(this.viewSelector, delta);
	}

	/**
	 * Scrolls a specified element by a specified delta.
	 *
	 * @param cssSelector the CSS selector of the element to scroll
	 * @param delta       the amount to scroll
	 */
	public void scroll(String cssSelector, int delta) {
		this.getDriver().executeScript(
				"""
						var cssSelector = arguments[0];
						var delta = arguments[1];
						var element = document.querySelector(cssSelector);
						element.scrollBy(0, delta)
						""",
				cssSelector, delta
		);
		sleep(500);
	}

	/**
	 * Retrieves the scroll top position of a specified element.
	 *
	 * @param cssSelector the CSS selector of the element
	 * @return the scroll top position
	 */
	public int getScrollTop(String cssSelector) {
		Long scrollTop = (Long) this.getDriver().executeScript(
				"""
						var cssSelector = arguments[0];
						var element = document.querySelector(cssSelector);
						return parseInt(element.scrollTop);
						""",
				cssSelector
		);
		return scrollTop.intValue();
	}

	/**
	 * Retrieves the screen height.
	 *
	 * @return the screen height
	 */
	public int getScreenHeight() {
		if (this.screenHeight < 0) {
			this.screenHeight = getDriver().manage().window().getSize().getHeight();
		}
		return this.screenHeight;
	}

	/**
	 * Retrieves the screen width.
	 *
	 * @return the screen width
	 */
	public int getScreenWidth() {
		if (this.screenWidth < 0) {
			this.screenWidth = getDriver().manage().window().getSize().getWidth();
		}
		return this.screenWidth;
	}

	/**
	 * Retrieves the parent element of a specified child element.
	 *
	 * @param child the child element
	 * @return the parent element
	 */
	public WebElement getParent(WebElement child) {
		return child.findElement(By.xpath(".."));
	}

	/**
	 * Retrieves the ancestor element of a specified child element by tag name.
	 *
	 * @param child the child element
	 * @param tag   the tag name of the ancestor element
	 * @return the ancestor element
	 */
	public WebElement getAncestorByTag(WebElement child, String tag) {
		return getAncestorByTag(child, tag, 5);
	}

	/**
	 * Retrieves the ancestor element of a specified child element by tag name with a specified level.
	 *
	 * @param child the child element
	 * @param tag   the tag name of the ancestor element
	 * @param level the maximum level to search for the ancestor
	 * @return the ancestor element
	 */
	public WebElement getAncestorByTag(WebElement child, String tag, int level) {
		WebElement parent = null;
		String currentTag = "";

		for (int i = 0; i < level - 1; i++) {
			parent = this.getParent(child);
			currentTag = parent.getTagName();
			if (currentTag.equalsIgnoreCase(tag)) {
				return parent;
			} else if (currentTag.equalsIgnoreCase(HTMLTags.BODY)) {
				break; // Reach top level
			} else {
				child = parent;
			}
		}
		return parent;
	}

	/**
	 * Loads local storage items from the test context.
	 */
	public void loadLocalStorageItems() {
		TestInfo testInfo = (TestInfo) this.runTimeContext.getTestLevelVariables(TEST_INFO_OBJECT);
		this.setLocalStorageItems(testInfo.getCustomLocalStorage());
	}

	/**
	 * Sets local storage items from a given data map.
	 *
	 * @param inputData the data map containing key-value pairs to set in local storage
	 */
	public void setLocalStorageItems(Object inputData) {
		if (inputData instanceof Map) {
			LocalStorage localStorage;
			Map<String, String> dataMap = (Map<String, String>) inputData;

			if (this.runTimeContext.isLocalExecutionMode()) {
				localStorage = ((WebStorage) this.driver).getLocalStorage();
			} else {
				RemoteExecuteMethod executeMethod = new RemoteExecuteMethod(this.driver);
				RemoteWebStorage webStorage = new RemoteWebStorage(executeMethod);
				localStorage = webStorage.getLocalStorage();
			}

			for (String key : dataMap.keySet()) {
				localStorage.setItem(key, dataMap.get(key));
			}
		} else {
			throw new IllegalArgumentException("Invalid argument: inputData");
		}
	}
}