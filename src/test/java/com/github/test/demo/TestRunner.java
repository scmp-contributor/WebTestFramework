package com.github.test.demo;

import com.scmp.framework.executor.TestExecutor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests;
import org.springframework.util.StringUtils;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.List;

@SpringBootTest(classes = com.scmp.framework.TestFramework.class)
public class TestRunner extends AbstractTestNGSpringContextTests {

	@Autowired
	private TestExecutor testExecutor;

	/* Please input below environment variables in order to load the correct config
        SPRING_CONFIG_NAME=config.properties
        PROJECT_TEST_PACKAGES=com.github.test.demo
 	*/
	@Value("${project.test.packages}")
	private String testPackages;
	@Test
	public void testApp() throws Exception {

		List<String> packages = new ArrayList<>(StringUtils.commaDelimitedListToSet(testPackages));
		boolean hasFailures = testExecutor.runTests(packages);

		Assert.assertFalse(hasFailures, "Testcases execution failed.");
	}
}
