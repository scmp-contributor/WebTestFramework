package com.scmp.framework.executor;

import com.scmp.framework.context.RunTimeContext;
import com.scmp.framework.testng.listeners.AnnotationTransformerListener;
import com.scmp.framework.testng.listeners.InvokedMethodListener;
import com.scmp.framework.testng.listeners.SuiteListener;
import com.scmp.framework.utils.Figlet;
import io.github.bonigarcia.wdm.WebDriverManager;
import org.jetbrains.annotations.NotNull;
import org.reflections.Reflections;
import org.reflections.scanners.MethodAnnotationsScanner;
import org.reflections.util.ConfigurationBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.testng.TestNG;
import org.testng.xml.XmlClass;
import org.testng.xml.XmlSuite;
import org.testng.xml.XmlSuite.ParallelMode;
import org.testng.xml.XmlTest;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

import static com.scmp.framework.utils.Constants.*;

@Component
public class TestExecutor {
	private final RunTimeContext context;
	private final List<String> packageList = new ArrayList<>();
	private static final Logger frameworkLogger = LoggerFactory.getLogger(TestExecutor.class);

	@Autowired
	public TestExecutor(RunTimeContext context) {
		this.context = context;
		if (this.context.isLocalExecutionMode()) {
			prepareWebDriver();
		}
	}

	/**
	 * Prepare Web driver for testing browser
	 * e.g. download web driver, set environment variables
	 */
	private void prepareWebDriver() {
		File directory = new File(context.getFrameworkConfigs().getDriverHome());
		if (!directory.exists()) {
			directory.mkdir();
		}

		System.setProperty(WDM_CACHE_PATH, context.getFrameworkConfigs().getDriverHome());
		setupWebDriver(WebDriverManager.chromedriver(), CHROME_DRIVER_PATH);
		setupWebDriver(WebDriverManager.firefoxdriver(), FIREFOX_DRIVER_PATH);
	}

	/**
	 * Setup WebDriver and set global variable
	 * @param manager WebDriverManager instance
	 * @param driverPathKey Key for the driver path
	 */
	private void setupWebDriver(@NotNull WebDriverManager manager, String driverPathKey) {
		manager.setup();
		context.setGlobalVariables(driverPathKey, manager.getDownloadedDriverPath());
	}

	/**
	 * Run tests under specific packages and defined test classes
	 *
	 * @param packages Package list
	 * @return userDefinedTestClasses status, passed / failed
	 * @throws Exception exception
	 */
	public boolean runTests(List<String> packages) throws Exception {
		System.out.println("***************************************************");
		this.packageList.addAll(packages);

		List<URL> testPackagesUrls = getTestPackagesUrls();
		Set<Method> testNGTests = findTestMethods(testPackagesUrls);
		Map<String, List<Method>> methods = createTestsMap(testNGTests);

		ExecutorService executor = Executors.newCachedThreadPool();
		List<FutureTask<Boolean>> list = new ArrayList<>();

		String[] browsers = context.getFrameworkConfigs().getBrowserType().split(",");
		for (String browser : browsers) {
			XmlSuite suite = buildXmlSuite(browser, methods);
			String suiteFile = writeTestNGFile(suite, "testsuite" + "-" + browser);

			FutureTask<Boolean> futureTask = new FutureTask<>(new TestExecutorService(suiteFile));
			list.add(futureTask);
			executor.submit(futureTask);
		}

		boolean hasFailure = waitForTestCompletion(executor, list);
		Figlet.print("Test Completed");

		return hasFailure;
	}

	/**
	 * Get URLs for test packages
	 * @return List of URLs
	 * @throws Exception exception
	 */
	@NotNull
	private List<URL> getTestPackagesUrls() throws Exception {
		List<URL> testPackagesUrls = new ArrayList<>();
		String testClassPackagePath = "file:" + TARGET_PATH + File.separator + "test-classes" + File.separator;

		for (String packageName : packageList) {
			URL testPackagesUrl = new URL(testClassPackagePath + packageName.replaceAll("\\.", "/"));
			testPackagesUrls.add(testPackagesUrl);
		}
		return testPackagesUrls;
	}

	/**
	 * Find test methods annotated with @Test
	 * @param testPackagesUrls List of URLs
	 * @return Set of Methods
	 */
	private Set<Method> findTestMethods(List<URL> testPackagesUrls) {
		Reflections reflections = new Reflections(new ConfigurationBuilder().setUrls(testPackagesUrls).setScanners(new MethodAnnotationsScanner()));
		return reflections.getMethodsAnnotatedWith(org.testng.annotations.Test.class);
	}

	/**
	 * Wait for the test completion
	 * @param executor ExecutorService
	 * @param list List of FutureTask
	 * @return boolean indicating if there was any failure
	 * @throws InterruptedException exception
	 * @throws ExecutionException exception
	 */
	private boolean waitForTestCompletion(ExecutorService executor, @NotNull List<FutureTask<Boolean>> list) throws InterruptedException, ExecutionException {
		while (true) {
			boolean isDone = true;
			for (FutureTask<Boolean> futureTask : list) {
				isDone = isDone && futureTask.isDone();
			}

			if (isDone) {
				executor.shutdown();
				break;
			} else {
				TimeUnit.SECONDS.sleep(1);
			}
		}

		boolean hasFailure = false;
		for (FutureTask<Boolean> result : list) {
			hasFailure = hasFailure || result.get();
		}
		return hasFailure;
	}

	/**
	 * Create the xml testng suite for execution
	 *
	 * @param browser browser name
	 * @param methods test methods
	 * @return XML suite
	 */
	public XmlSuite buildXmlSuite(String browser, Map<String, List<Method>> methods) {
		XmlSuite suite = initializeXmlSuite();
		XmlTest test = initializeXmlTest(suite, browser);

		test.setXmlClasses(createXmlClassList(methods));
		return suite;
	}

	/**
	 * Initialize XML Suite
	 * @return XML Suite
	 */
	@NotNull
	private XmlSuite initializeXmlSuite() {
		XmlSuite suite = new XmlSuite();
		suite.setName("Test Suite");
		suite.setPreserveOrder(true);
		suite.setThreadCount(context.getFrameworkConfigs().getThreadCount());
		suite.setDataProviderThreadCount(context.getFrameworkConfigs().getDataProviderThreadCount());
		suite.setParallel(ParallelMode.METHODS);
		suite.setVerbose(2);

		List<String> listeners = Arrays.asList(
				SuiteListener.class.getName(),
				InvokedMethodListener.class.getName(),
				AnnotationTransformerListener.class.getName()
		);
		suite.setListeners(listeners);
		return suite;
	}

	/**
	 * Initialize XML Test
	 * @param suite XML Suite
	 * @param browser Browser name
	 * @return XML Test
	 */
	@NotNull
	private XmlTest initializeXmlTest(XmlSuite suite, String browser) {
		XmlTest test = new XmlTest(suite);
		test.setName("Automated Test");
		test.addParameter("browser", browser);

		List<String> groupsInclude = Arrays.asList(context.getFrameworkConfigs().getIncludeGroups().split("\\s*,\\s*"));
		List<String> groupsExclude = Arrays.asList(context.getFrameworkConfigs().getExcludeGroups().split("\\s*,\\s*"));

		test.setIncludedGroups(groupsInclude);
		test.setExcludedGroups(groupsExclude);
		return test;
	}

	/**
	 * Create TestNG XML Class
	 * (not handling methods, methods will be controlled by includes and excludes rules)
	 *
	 * @param methods all available test methods
	 * @return TestNG XML class list
	 */
	public List<XmlClass> createXmlClassList(@NotNull Map<String, List<Method>> methods) {
		return methods.keySet().stream()
				.filter(className -> !className.contains("TestRunner"))
				.map(XmlClass::new)
				.collect(Collectors.toList());
	}

	/**
	 * Write the XML suite to target folder
	 *
	 * @param suite    XML suite
	 * @param fileName file name to be created
	 * @return full file path of the xml file
	 */
	@NotNull
	private String writeTestNGFile(@NotNull XmlSuite suite, String fileName) {
		System.out.println(suite.toXml());
		String suiteXML = System.getProperty("user.dir") + "/target/" + fileName + ".xml";

		try (FileWriter writer = new FileWriter(suiteXML)) {
			writer.write(suite.toXml());
		} catch (IOException e) {
			frameworkLogger.error("Failed to write TestNG suite file", e);
		}

		return suiteXML;
	}

	/**
	 * Create a test class, test methods mapping
	 *
	 * @param methods all testng tests
	 * @return test class, test method map
	 */
	public Map<String, List<Method>> createTestsMap(@NotNull Set<Method> methods) {
		Map<String, List<Method>> testsMap = new HashMap<>();
		methods.forEach(method -> {
			String className = method.getDeclaringClass().getPackage().getName() + "." + method.getDeclaringClass().getSimpleName();
			testsMap.computeIfAbsent(className, k -> new ArrayList<>()).add(method);
		});
		return testsMap;
	}
}

class TestExecutorService implements Callable<Boolean> {
	private final String suite;

	public TestExecutorService(String file) {
		suite = file;
	}

	@Override
	public Boolean call() {
		List<String> suiteFiles = Collections.singletonList(suite);

		TestNG testNG = new TestNG();
		testNG.setTestSuites(suiteFiles);
		testNG.run();

		return testNG.hasFailure();
	}
}