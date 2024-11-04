package com.github.test.demo.pom;

import com.scmp.framework.context.ApplicationContextProvider;
import com.scmp.framework.context.RunTimeContext;
import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.context.ApplicationContext;

import java.time.Duration;

public abstract class BasePage {
	public RemoteWebDriver driver;
	public RunTimeContext runTimeContext;

	public BasePage(RemoteWebDriver driver) {
		this.driver = driver;
		ApplicationContext context = ApplicationContextProvider.getApplicationContext();
		runTimeContext = context.getBean(RunTimeContext.class);
	}

	public abstract void launch();

	public void sleep(long millis) {
		try {
			Thread.sleep(millis);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	public abstract boolean waitForPageLoad();

	public boolean waitForVisible(WebElement element) {
		return waitForVisible(element, 60);
	}

	public boolean waitForVisible(WebElement element, long seconds) {
		try {
			new WebDriverWait(driver, Duration.ofSeconds(seconds))
					.until(ExpectedConditions.visibilityOf(element));

			return true;
		} catch (Exception e) {
			return false;
		}
	}

	public boolean waitForElementToBeClickable(WebElement element, long seconds) {
		try {
			new WebDriverWait(driver, Duration.ofSeconds(seconds)).until(ExpectedConditions.elementToBeClickable(element));

			return true;
		} catch (Exception e) {
			return false;
		}
	}

	public boolean waitForPageToLoad(WebElement element) {
		return waitForPageToLoad(element, 15);
	}

	public boolean waitForPageToLoad(WebElement element, int secondsToWait) {
		try {
			WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(secondsToWait));
			wait.until(ExpectedConditions.elementToBeClickable(element));

			return true;
		} catch (Exception e) {
			return false;
		}

	}

	public void waitForElementToDisAppear(String id) {
		WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(15));
		wait.until(ExpectedConditions.invisibilityOfElementLocated(By.id(id)));
	}

	public WebElement waitForElement(WebElement arg) {
		waitForPageToLoad(arg);

		return arg;
	}

	public boolean isElementPresent(By by) {
		try {
			driver.findElement(by);

			return true;
		} catch (NoSuchElementException e) {
			return false;
		}

	}
}
