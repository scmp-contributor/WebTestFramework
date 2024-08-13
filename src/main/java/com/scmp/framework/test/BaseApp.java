package com.scmp.framework.test;

import com.scmp.framework.context.ApplicationContextProvider;
import com.scmp.framework.context.RunTimeContext;
import lombok.Getter;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;

import java.lang.reflect.Field;

public abstract class BaseApp {
	private static final Logger frameworkLogger = LoggerFactory.getLogger(BaseApp.class);
	@Getter
	private final RemoteWebDriver driver;
    @Getter
    private final PageNavigator pageNavigator;
	@Getter
	private final PageProperties pageProperties;

	public BaseApp(RemoteWebDriver driver) {
		this.driver = driver;

		ApplicationContext context = ApplicationContextProvider.getApplicationContext();
        RunTimeContext runTimeContext = context.getBean(RunTimeContext.class);

		this.pageProperties = new PageProperties(driver, runTimeContext);
		this.pageNavigator = new PageNavigator(driver, this.pageProperties);
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

	public String getURL() {
		return this.pageNavigator.getURL();
	}
}
