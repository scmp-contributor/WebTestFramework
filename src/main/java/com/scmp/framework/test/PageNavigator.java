package com.scmp.framework.test;

import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;
import java.util.ArrayList;

public class PageNavigator {
    private final RemoteWebDriver driver;
    private final PageProperties pageProperties;

    public PageNavigator(RemoteWebDriver driver, PageProperties pageProperties) {
        this.driver = driver;
        this.pageProperties = pageProperties;
    }

    public void refresh() {
        this.driver.navigate().refresh();
    }

    public String getURL() {
        String url = this.driver.getCurrentUrl();
        if (url.endsWith("/")) {
            return url.substring(0, url.length() - 1);
        }
        return url;
    }

    public void navigateTo(String path) {
        this.driver.navigate().to(this.getURL() + path);
    }

    public void mouseClick(WebElement element) {
        new Actions(this.driver).moveToElement(element).click().perform();
    }

    public void javascriptClick(WebElement element) {
        this.driver.executeScript("arguments[0].click();", element);
    }

    public void mouseFocus(WebElement element) {
        new Actions(this.driver).moveToElement(element).perform();
    }

    public void executeScript(String script, Object... arguments) {
        this.driver.executeScript(script, arguments);
    }

    public void switchToTab(int tabSequence) {
        ArrayList<String> tabs = new ArrayList<>(driver.getWindowHandles());
        driver.switchTo().window(tabs.get(tabSequence));
    }

    public void scrollDown() {
        this.scrollDown(1);
    }

    public void scrollDown(int times) {
        scrollDown(times, 1000);
    }

    public void scrollDown(int times, int waitInMilliSecond) {
        for (int i = 0; i < times; ++i) {
            long delta = (long) (this.pageProperties.getScreenHeight() * 0.75);
            this.driver.executeScript("window.scrollBy(0, arguments[0])", delta);
            this.sleep(waitInMilliSecond);
        }
    }

    /**
     * Use Javascript function scrollIntoView() to move element to current view
     * @param element
     */
    public void scrollElementToView(WebElement element) {
        // Scroll the element into view
        this.driver.executeScript(
                "var viewPortHeight = Math.max(document.documentElement.clientHeight, window.innerHeight || 0);"
                        + "var elementTop = arguments[0].getBoundingClientRect().top;"
                        + "window.scrollBy(0, elementTop-(viewPortHeight/2));"
                , element);
        sleep(1000);
    }

    public boolean scrollToElement(WebElement element) {
        return this.scrollToElement(element, 30);
    }

    public boolean scrollToElement(WebElement element, int maxScrollCount) {

        for (int count = 0; count < maxScrollCount; count++) {
            if (this.waitForElementToBeClickable(element, 1)) {
                // adjust the element position so it can be clicked
                this.scrollElementToView(element);
                return true;
            }

            scrollDown();
        }

        return false;
    }

    public boolean waitForElementToBeClickable(WebElement element, long seconds) {
        try {
            (new WebDriverWait(this.driver, Duration.ofSeconds(seconds)))
                    .until(ExpectedConditions.elementToBeClickable(element));
            return true;
        } catch (Exception var5) {
            return false;
        }
    }

    public void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException var4) {
            // DO NOTHING
        }
    }
}
