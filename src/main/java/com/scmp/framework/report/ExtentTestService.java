package com.scmp.framework.report;

import com.aventstack.extentreports.ExtentTest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;

@Component
public class ExtentTestService {
	private static final Logger frameworkLogger = LoggerFactory.getLogger(ExtentTestService.class);

	private final ConcurrentHashMap<String, ExtentTest> extentReportMap = new ConcurrentHashMap<>();
	private final ExtentManager extentManager;

	@Autowired
	public ExtentTestService(ExtentManager extentManager) {
		this.extentManager = extentManager;
	}

	/**
	 * Push all updates to the report.
	 */
	public void flush() {
		extentManager.getExtent().flush();
	}

	/**
	 * Remove the extent test record if the previous run fails.
	 *
	 * @param test extent test record to be removed
	 */
	public void removeTest(ExtentTest test) {
		extentManager.getExtent().removeTest(test);
	}

	/**
	 * Create or reuse an extent report record by test name and description.
	 *
	 * @param name        test name
	 * @param description test description
	 * @return extent test record
	 */
	public synchronized ExtentTest createTest(String name, String description) {
		ExtentTest test;

		if (extentReportMap.containsKey(name)) {
			frameworkLogger.info("Reuse Test Thread ID: {}, Key: {}", Thread.currentThread().getId(), name);
			test = extentReportMap.get(name);
		} else {
			frameworkLogger.info("Create new Test Thread ID: {}, Key: {}", Thread.currentThread().getId(), name);
			test = extentManager.getExtent().createTest(name, description);
			extentReportMap.put(name, test);
		}

		return test;
	}
}