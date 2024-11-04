package com.scmp.framework.testng.listeners;

import com.scmp.framework.annotations.RetryCount;
import com.scmp.framework.context.ApplicationContextProvider;
import com.scmp.framework.context.RunTimeContext;
import com.scmp.framework.testng.model.RetryMethod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.testng.IRetryAnalyzer;
import org.testng.ITestResult;

import java.lang.reflect.Method;
import java.util.concurrent.ConcurrentHashMap;

/**
 * RetryAnalyzer - Handles retry logic for failed test cases.
 */
public class RetryAnalyzer implements IRetryAnalyzer {
	private static final Logger frameworkLogger = LoggerFactory.getLogger(RetryAnalyzer.class);
	private final ConcurrentHashMap<String, RetryMethod> retryMap = new ConcurrentHashMap<>();
	private final RunTimeContext runTimeContext;

	public RetryAnalyzer() {
		ApplicationContext context = ApplicationContextProvider.getApplicationContext();
		runTimeContext = context.getBean(RunTimeContext.class);
	}

	/**
	 * Check if the method has been retried.
	 *
	 * @param iTestResult the test result
	 * @return true if the method has been retried, false otherwise
	 */
	public boolean isRetriedMethod(ITestResult iTestResult) {
		return this.getRetryMethod(iTestResult).isRetried();
	}

	/**
	 * Check if the method needs to be retried.
	 *
	 * @param iTestResult the test result
	 * @return true if the method needs to be retried, false otherwise
	 */
	public boolean isRetriedRequired(ITestResult iTestResult) {
		return this.getRetryMethod(iTestResult).needRetry();
	}

	@Override
	public synchronized boolean retry(ITestResult iTestResult) {
		if (iTestResult.getStatus() == ITestResult.FAILURE) {
			RetryMethod method = getRetryMethod(iTestResult);
			frameworkLogger.error("Test Failed - {}", method.getMethodName());
			if (method.needRetry()) {
				method.increaseRetryCount();
				frameworkLogger.info("Retrying Failed Test Case {} out of {}", method.getRetryCount(), method.getMaxRetryCount());
				return true;
			} else {
				frameworkLogger.info("Reached maximum retry count [{}]", method.getMaxRetryCount());
				return false;
			}
		}
		return false;
	}

	/**
	 * Get the retry method details for the given test result.
	 *
	 * @param iTestResult the test result
	 * @return the retry method details
	 */
	public RetryMethod getRetryMethod(ITestResult iTestResult) {
		String methodName = iTestResult.getMethod().getMethodName();
		String key = methodName + Thread.currentThread().getId();

		return retryMap.computeIfAbsent(key, k -> {
			int maxRetryCount = getMaxRetryCount(iTestResult, methodName);
			return new RetryMethod(0, maxRetryCount, methodName);
		});
	}

	/**
	 * Get the maximum retry count for the given test result and method name.
	 *
	 * @param iTestResult the test result
	 * @param methodName  the method name
	 * @return the maximum retry count
	 */
	private int getMaxRetryCount(ITestResult iTestResult, String methodName) {
		int maxRetryCount = runTimeContext.getFrameworkConfigs().getMaxRetryCount();
		Method[] methods = iTestResult.getInstance().getClass().getMethods();
		for (Method method : methods) {
			if (method.getName().equals(methodName) && method.isAnnotationPresent(RetryCount.class)) {
				RetryCount retryCount = method.getAnnotation(RetryCount.class);
				maxRetryCount = retryCount.maxRetryCount();
				break;
			}
		}
		return maxRetryCount;
	}
}