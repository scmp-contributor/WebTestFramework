package com.scmp.framework.testng.listeners;

import org.testng.IAnnotationTransformer;
import org.testng.IRetryAnalyzer;
import org.testng.annotations.ITestAnnotation;
import org.testng.internal.annotations.DisabledRetryAnalyzer;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

public class AnnotationTransformerListener implements IAnnotationTransformer {
	@Override
	public void transform(
			ITestAnnotation iTestAnnotation, Class aClass, Constructor constructor, Method method) {

		// Handle Retry
		Class<? extends IRetryAnalyzer> retry = iTestAnnotation.getRetryAnalyzerClass();
		if (retry == null || retry.equals(DisabledRetryAnalyzer.class)) {
			iTestAnnotation.setRetryAnalyzer(RetryAnalyzer.class);
		}
	}
}
