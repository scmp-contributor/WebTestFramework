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

public abstract class BaseApp {
	private static final Logger frameworkLogger = LoggerFactory.getLogger(BaseApp.class);
	@Getter
	private final RemoteWebDriver driver;
	private final RunTimeContext runTimeContext;

	public BaseApp(RemoteWebDriver driver) {
		this.driver = driver;

		ApplicationContext context = ApplicationContextProvider.getApplicationContext();
		runTimeContext = context.getBean(RunTimeContext.class);
	}

	protected void initApp() {
		Field[] fields = this.getClass().getDeclaredFields();
		for (Field field : fields) {
			if (BasePage.class.isAssignableFrom(field.getType())) {
				try {
					field.set(this, field.getType().getConstructors()[0].newInstance(driver));
				} catch (Exception e) {
					frameworkLogger.error("Ops!", e);
				}
			}
		}
	}

	public void refresh() {
		this.getDriver().navigate().refresh();
	}

	public void navigateTo(String path) {
		this.getDriver().navigate().to(this.getURL() + path);
	}

	public String getURL() {
		String url = this.driver.getCurrentUrl();
		if (url.endsWith("/")) {
			return url.substring(0, url.length() - 1);
		}
		return url;
	}

	public void mouseClick(WebElement element) {
		new Actions(this.getDriver()).moveToElement(element).click().perform();
	}

	public void javascriptClick(WebElement element) {
		this.getDriver().executeScript("arguments[0].click();", element);
	}

	public void mouseFocus(WebElement element) {
		new Actions(this.getDriver()).moveToElement(element).perform();
	}

	public void executeScript(String script, Object... arguments) {
		this.getDriver().executeScript(script, arguments);
	}

	public void switchToTab(int tabSequence) {
		ArrayList<String> tabs = new ArrayList<>(driver.getWindowHandles());
		driver.switchTo().window(tabs.get(tabSequence));
	}

	public void loadLocalStorageItems() {
		TestInfo testInfo = (TestInfo) runTimeContext.getTestLevelVariables(TEST_INFO_OBJECT);
		this.setLocalStorageItems(testInfo.getCustomLocalStorage());
	}

	public void setLocalStorageItems(Map<String, String> dataMap) {
		LocalStorage local = ((WebStorage) this.driver).getLocalStorage();
		for (String key : dataMap.keySet()) {
			local.setItem(key, dataMap.get(key));
		}
	}

	public String getLocalStorageItem(String key) {
		LocalStorage local = ((WebStorage) this.driver).getLocalStorage();
		return local.getItem(key);
	}

	public void setLocalStorageItem(String key, String value) {
		LocalStorage local = ((WebStorage) this.driver).getLocalStorage();
		local.setItem(key, value);
	}

	public String removeLocalStorageItem(String key) {
		LocalStorage local = ((WebStorage) this.driver).getLocalStorage();
		return local.removeItem(key);
	}
}
