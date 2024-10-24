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

public abstract class BasePage extends BasePageElement {

	@Getter
	private final RemoteWebDriver driver;
	private String viewSelector;
	private int screenHeight = -1;
	private int screenWidth = -1;
	public String PATH = "";

	public BasePage(RemoteWebDriver driver, String viewSelector) {
		super(driver);
		this.driver = driver;
		this.viewSelector = viewSelector;
	}

	public void launch() {
		this.getDriver().get(this.getURL());
		this.waitForPageLoad();
	}

	public void launch(String path) {
		this.PATH = path;
		this.launch();
	}

	public void launchWithSetups(String path) {
		this.PATH = path;
		this.launchWithSetups();
	}

	public void launchWithoutWaiting() {
		this.getDriver().get(this.getURL());
	}

	public void launchWithSetups() {
		this.getDriver().get(this.getURL());
		this.postLaunchActions();

		// Reload the page
		this.getDriver().get(this.getURL());
		this.waitForPageLoad();
	}

	public void postLaunchActions() {
		this.loadLocalStorageItems();
	}

	public String getPath() {
		String path = this.PATH;
		if (path.endsWith("/")) {
			return path.substring(0, path.length() - 1);
		}
		return path;
	}

	public String getURL() {
		String url = this.runTimeContext.getURL() + this.PATH;
		if (url.endsWith("/")) {
			return url.substring(0, url.length() - 1);
		}
		return url;
	}

	public abstract boolean waitForPageLoad();

	public void scrollToTop() {
		this.getDriver().executeScript("" +
						"var cssSelector = arguments[0];" +
						"var element = document.querySelector(cssSelector);" +
						"element.scrollTo(0, 0)",
				this.viewSelector);

		sleep(500);
	}

	public boolean scrollToElement(WebElement element) {
		return this.scrollToElement(element, 30);
	}

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

				// Element on screen
				return true;

			} else {
				// Scroll down
				scrollDown();
				int scrollTopAfterScoll = this.getScrollTop(this.viewSelector);

				// If reach bottom of the div
				if (scrollTopB4Scroll == scrollTopAfterScoll) {
					return false;
				} else {
					scrollTopB4Scroll = scrollTopAfterScoll;
				}
			}
		}

		// element not found
		return false;
	}

	public void scrollUp() {
		scroll(this.viewSelector, -this.getScreenHeight() * 3 / 4);
	}

	public void scrollUp(int delta) {
		this.scroll(this.viewSelector, -delta);
	}

	public void scrollDown() {
		scroll(this.viewSelector, this.getScreenHeight() * 3 / 4);
	}

	public void scrollDown(int times) {
		for (int i = 0; i < times; i++) {
			scrollDown();
		}
	}

	public void scrollDown(int times, int waitInMilliSecond) {
		for (int i = 0; i < times; i++) {
			scrollDown();
			sleep(waitInMilliSecond);
		}
	}

	public void scrollDownBy(int delta) {
		this.scroll(this.viewSelector, delta);
	}

	public void scroll(String cssSelector, int delta) {
		this.getDriver().executeScript("" +
						"var cssSelector = arguments[0];" +
						"var delta = arguments[1];" +
						"var element = document.querySelector(cssSelector);" +
						"element.scrollBy(0, delta)",
				cssSelector, delta);

		sleep(500);
	}

	public int getScrollTop(String cssSelector) {
		Long scollTop = (Long) this.getDriver().executeScript("" +
						"var cssSelector = arguments[0];" +
						"var element = document.querySelector(cssSelector);" +
						"return parseInt(element.scrollTop);",
				cssSelector);

		return scollTop.intValue();
	}

	public int getScreenHeight() {
		if (this.screenHeight < 0) {
			this.screenHeight = getDriver().manage().window().getSize().getHeight();
		}
		return this.screenHeight;
	}

	public int getScreenWidth() {
		if (this.screenWidth < 0) {
			this.screenWidth = getDriver().manage().window().getSize().getWidth();
		}
		return this.screenWidth;
	}

	public WebElement getParent(WebElement child) {
		return child.findElement(By.xpath(".."));
	}

	public WebElement getAncestorByTag(WebElement child, String tag) {
		return getAncestorByTag(child, tag, 5);
	}

	public WebElement getAncestorByTag(WebElement child, String tag, int level) {
		WebElement parent = null;
		String currentTag = "";

		for (int i = 0; i < level - 1; i++) {
			parent = this.getParent(child);
			currentTag = parent.getTagName();
			if (currentTag.equalsIgnoreCase(tag)) {
				return parent;
			} else if (currentTag.equalsIgnoreCase(HTMLTags.BODY)) {
				// Reach top level
				break;
			} else {
				child = parent;
			}
		}

		return parent;
	}

	public void loadLocalStorageItems() {
		TestInfo testInfo = (TestInfo) this.runTimeContext.getTestLevelVariables(TEST_INFO_OBJECT);
		this.setLocalStorageItems(testInfo.getCustomLocalStorage());
	}

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
