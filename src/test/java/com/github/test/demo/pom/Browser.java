package com.github.test.demo.pom;

import org.openqa.selenium.remote.RemoteWebDriver;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;

public class Browser {
	public GoogleHome GoogleHome;
	public GoogleSearchResult GoogleSearchResult;
	private RemoteWebDriver driver;

	public Browser(RemoteWebDriver driver) {
		this.driver = driver;

		Field[] fields = this.getClass().getDeclaredFields();
		for (Field field : fields) {
			// Skip RemoteWebDriver
			if (field.getType().getSimpleName().equals("RemoteWebDriver")) {
				continue;
			}

			try {
				field.set(this, field.getType().getConstructors()[0].newInstance(driver));
			} catch (IllegalArgumentException | IllegalAccessException | InstantiationException
					 | InvocationTargetException | SecurityException e) {
				e.printStackTrace();
			}
		}
	}
}
