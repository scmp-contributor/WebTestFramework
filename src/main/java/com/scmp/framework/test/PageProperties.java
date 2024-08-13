package com.scmp.framework.test;

import com.scmp.framework.context.RunTimeContext;
import com.scmp.framework.testng.model.TestInfo;
import org.openqa.selenium.html5.LocalStorage;
import org.openqa.selenium.html5.WebStorage;
import org.openqa.selenium.remote.RemoteWebDriver;

import java.util.Map;

import static com.scmp.framework.utils.Constants.TEST_INFO_OBJECT;

public class PageProperties {
    private final RemoteWebDriver driver;
    private final RunTimeContext runTimeContext;

    public PageProperties(RemoteWebDriver driver, RunTimeContext runTimeContext) {
        this.driver = driver;
        this.runTimeContext = runTimeContext;
    }

    public int getScreenHeight() {
        return this.driver.manage().window().getSize().getHeight();
    }

    public int getScreenWidth() {
        return this.driver.manage().window().getSize().getWidth();
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
