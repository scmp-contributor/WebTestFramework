package com.scmp.framework.testng.model;

import com.scmp.framework.model.Browser;
import lombok.Getter;

public class RetryMethod {
	@Getter
	private int retryCount = 0;
	@Getter
	private int maxRetryCount = 0;
	@Getter
	private String methodName = "";
	@Getter
	private Browser browserType = null;
	private boolean isRetried = false;

	public RetryMethod(int retryCount, int maxRetryCount, String methodName) {
		this.retryCount = retryCount;
		this.maxRetryCount = maxRetryCount;
		this.methodName = methodName;
	}

	public boolean needRetry() {
		return retryCount < maxRetryCount;
	}

	public void increaseRetryCount() {
		isRetried = true;
		retryCount++;
	}

	public void decreaseRetryCount() {
		retryCount--;
	}

	public void setBrowserType(Browser browserType) {
		this.browserType = browserType;
	}

	public void setRetried(boolean status) {
		this.isRetried = status;
	}

	public boolean isRetried() {
		return this.isRetried;
	}
}
