package com.scmp.framework.test;

import org.openqa.selenium.remote.RemoteWebDriver;

/**
 * BasePageComponent - Abstract base class for page component-specific actions and utilities.
 */
public abstract class BasePageComponent extends BasePageElement {
	private final RemoteWebDriver driver;

	/**
	 * Constructor to initialize the RemoteWebDriver.
	 *
	 * @param driver the RemoteWebDriver instance
	 */
	public BasePageComponent(RemoteWebDriver driver) {
		super(driver);
		this.driver = driver;
	}

	/**
	 * Abstract method to wait for the component to load.
	 *
	 * @return true if the component is loaded, false otherwise
	 */
	public abstract boolean waitForComponentLoad();
}