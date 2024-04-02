# WebTestFramework

[![](https://jitpack.io/v/scmp-contributor/WebTestFramework.svg)](https://jitpack.io/#scmp-contributor/WebTestFramework)

## Description:
Test framework for Web testing integrated with TestNG and Extent Report.

### Test Entry Point [TestRunner.java](https://github.com/scmp-contributor/WebTestFramework/blob/master/src/test/java/com/github/test/demo/TestRunner.java)
```java
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
```

### How To Start The Test
```bash
URL=<your testing url> mvn clean test -Dtest=TestRunner
# To override the configs from config.properties, e.g. overriding INCLUDE_GROUPS
URL=<your testing url> INCLUDE_GROUPS=<your runtime include groups> mvn clean test -Dtest=WebRunner
```

#### Config below properties to setup the test framework([config.properties](https://github.com/scmp-contributor/WebTestFramework/blob/master/config.properties)):
```properties
############################## WEB ##########################################
# If more than one browser is provided in BROWSER_TYPE
# test will be run on different browsers on parallel thread
# if `random` is set, chrome and firefox will be random assigned unless
# the test is annotated by FirefoxOnly or ChromeOnly
# Available options: chrome,firefox,random
BROWSER_TYPE=random
# Selenium hub in docker server
HOST_URL=http://localhost:4444/wd/hub

####################### FRAMEWORK ###########################################
FRAMEWORK=testng
THREAD_COUNT=3
DATAPROVIDER_THREAD_COUNT=3
MAX_RETRY_COUNT=1
REMOVE_FAILED_TEST_B4_RETRY=true
PRELOAD_LOCAL_STORAGE_DATA=true
LOCAL_STORAGE_DATA_PATH=data/configs/localstorage.properties
DEFAULT_LOCAL_EXTENSION_PATH=
DEFAULT_REMOTE_EXTENSION_PATH=

######################## TESTRAIL #########################################
TESTRAIL_SERVER=http://<server>/testrail/
TESTRAIL_USER_NAME=XXXXXX
TESTRAIL_API_KEY=XXXXXX
# ${date} would be replaced with current date in format 1/20/2021
# ${FEATURE_DESCRIPTION} will read data from environment variable
TESTRAIL_TEST_RUN_NAME=Automated Test Run ${date} ${FEATURE_DESCRIPTION}
TESTRAIL_PROJECT_ID=1
# TESTRAIL_CREATE_NEW_TEST_RUN:
# false: the framework will lookup existing TestRun from TestRail base on the
# TESTRAIL_TEST_RUN_NAME if fails to find on TestRail, a new one will be created
# true: always create a new test run
TESTRAIL_CREATE_NEW_TEST_RUN=false
# TESTRAIL_INCLUDE_ALL_AUTOMATED_TEST_CASES:
# this field will be used when creating a new test run.
# true: all automated test cases will be included
# false: only the selected test from TestNG will be included
TESTRAIL_INCLUDE_ALL_AUTOMATED_TEST_CASES=false
TESTRAIL_UPLOAD_FLAG=false

######################## TEST ###############################################
EXCLUDE_GROUPS=INVALID
INCLUDE_GROUPS=RETRY
URL=https://www.example.com
FEATURE_DESCRIPTION=

######################## DEBUG ##############################################
# DRIVER_HOME ==> Path to store webdriver, drivers will be downloaded base on your platform and browser version
DRIVER_HOME=drivers
# With local execution mode(debug mode) ON, browser will be launched locally using driver in DRIVER_HOME
LOCAL_EXECUTION=ON
```

### Useful Annotations
| Annotation Name                | Description                                                                                                                                             |
|--------------------------------|---------------------------------------------------------------------------------------------------------------------------------------------------------|
| `IncognitoPrivateMode`         | Incognito for Chrome and private mode for Firefox                                                                                                       |
| `AcceptUntrustedCertificates`  | Accept untrusted certificates                                                                                                                           |
| `CaptureNetworkTraffic4Chrome` | Capture newwork log for GA testing                                                                                                                      |
| `ChromeArguments`              | Extra chrome arguments <br/> @ChromeArguments(options = {"--incognito"})                                                                                |
| `FirefoxArguments`             | Extra firefox arguments <br/> @FirefoxArguments(options = {"--private"})                                                                                |
| `HeadlessMode`                 | Headless mode for both firefox and chrome                                                                                                               |
| `RetryCount`                   | Retry count of the test, it will override the config retry count                                                                                        |
| `Device`                       | Custom the screen size <br/> @Device(device = DeviceName.iPhoneX)                                                                                       |
| `ChromeOnly`                   | Override the browser config, run test on chrome only                                                                                                    |
| `FirefoxOnly`                  | Override the browser config, run test on firefox only                                                                                                   |
| `Test`                         | TestNG annotation, test case indication <br/> @Test(groups = {DESKTOP, LOGIN, GA, REGRESSION})                                                          |
| `Author`                       | Author of the test case, it will show up in report                                                                                                      |
| `LocalStorageData`             | LocalStorage data to be specified on CustomLocalStorage <br/> @LocalStorageData(key = "key", value = "value")                                           |
| `CustomLocalStorage`           | LocalStorage data to load on testing page <br/> @CustomLocalStorage(path = "path/to/config.properties", data = {@LocalStorageData()}, loadDefault=true) |
| `TestRailTestCase`             | To indicate the test case on TestRail <br/> @TestRailTestCase(id = id, testRailUrl="url for the case")                                                  |
| `LaunchBrowser`                | Whether to launch browser, set false for API only test case <br/> @LaunchBrowser(status = true)                                                         |
| `CustomProxy`                  | Whether to launch browser, set false for API only test case <br/> @CustomProxy(factory = `Class of Proxy Factory`, name = `name of the proxy`)          |
| `SkipGlobalChromeOptions`      | Skip using GLOBAL_CHROME_OPTIONS in config.properties                                                                                                   |

### Use Logging Function
```java
// To initialize the test logger
TestLogger logger = new TestLogger();
```
| Function Name                    | Description                                            |
|----------------------------------|--------------------------------------------------------|
| `logInfo(message)`               | Log info to report                                     |
| `logInfoWithScreenshot(message)` | Log info to report with screenshot of current page     |
| `logPass(message)`               | Log a test pass for one step                           |
| `logPassWithScreenshot(message)` | Log a test pass for one step with screenshot           |
| `logFail(message)`               | With screenshot by default, will NOT stop current test |
| `logFatalError(message)`         | With screenshot by default, will STOP current test     |
| `String captureScreen()`         | Returning the file path of the screenshot              |

## Changelog
*4.3.8*
- **[Bug Fix]**
  - Fixed GA4 tracking url regex
*4.3.7*
- **[Enhancement]**
  - Added GA4 tracking needed model
  - Update web driver manager to 5.6.4

*4.3.5*
- **[Bug Fix]**
  - Added back groovy-xml for RestAssured

*4.3.5*
- **[Enhancement]**
  - Updated with multiple projects support
- **[Dependency Update]**
  - Upgraded `org.seleniumhq.selenium` 4.10.0
  - Upgraded `org.projectlombok` 1.18.30

*4.3.4*
- **[Bug Fix]**
- Fixed logback library conflicts

*4.3.2*
- **[Bug Fix]**
  - Fix web driver manager initialization in TestExecutor

*4.3.1*
- **[Enhancement]**
  - Update the version of web driver manager bonigarcia to 5.5.2

*4.3.0*
- **[Enhancement]**
  - Implemented Sprint boot
  - Simplified TestExecutor logic
- **[Dependency Update]**
  - Upgraded `logback-classic` 1.2.9
  - Upgraded `rest-assured` 4.5.1
  - Added `spring-boot-starter`
  - Added `spring-boot-starter-test`
  
*4.2.10*
- **[Enhancement]**
  - Added GLOBAL_CHROME_OPTIONS in config.properties
  - Added @SkipGlobalChromeOptions for skip using GLOBAL_CHROME_OPTION config file properties
  
*4.2.9*
- **[Bug Fix]**
  - Add chromeOptions to fix webdriver 403 forbidden error

*4.2.8*
- **[Dependency Update]**
  - Updated `guava` 31.1-jre
  - Updated `selenium-java` 4.0.0
  
*4.2.7*
- **[Enhancement]**
  - Implemented logic to preload default Chrome extension
  - Integrated with browsermob proxy
- **[Dependency Update]**
  - added `browsermob-core`2.1.5
    
*4.2.6*
- **[Bug Fix]**
  - Fixed TestRail API Update: get all test run test cases with paging

*4.2.5*
- **[Bug Fix]**
  - Fixed TestRail API Update: get all test cases with paging
  
*4.2.4*
- **[Enhancement]**
  - Implemented Chartbeat requests inspection for data tracking

*4.2.3*
- **[Bug Fix]**
  - Bug fix for TestRail upgrade v7.2.1.3027(fixed on TestRail test case filter logic)
  
*4.2.2*
- **[Bug Fix]**
  - Bug fix for TestRail upgrade v7.2.1.3027(fixed on getTestCases and getTestRunTests)
  
*4.2.1*
- **[Bug Fix]**
  - Bug fix for TestRail upgrade v7.2.1.3027(json response updated for test runs)
  
*4.2.0*
- **[ENHANCEMENTS]**
  - Support RestAssured API test
  - Support logging for Json data  
  - Added annotation @LaunchBrowser to control whether to launch browser
  
*4.1.3*
- **[Bug Fix]**
  - TestRail Attachment Id changed from Int to String

*4.1.2*
- **[ENHANCEMENTS]**
  - Support TestRail rerun test cases on specific status
  
*4.1.1*
- **[DEPENDENCY UPDATES]**
    - Fixed `testNG` version 6.14.3
  
*4.1.0*
- **[ENHANCEMENTS]**
    - Integrated with TestRail
    - Implemented logback for logging
    - Cleanup unused codes
- **[DEPENDENCY UPDATES]**
    - added `retrofit`2.9.0.
    - added `converter-gson` 2.9.0
    - added `lombok` 1.18.16
    - added `slf4j-api` 1.7.30
    - added `logback-classic` 1.2.3  
    - upgraded `gson` to 2.8.6
