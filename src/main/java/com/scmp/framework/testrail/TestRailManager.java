package com.scmp.framework.testrail;

import com.scmp.framework.context.FrameworkConfigs;
import com.scmp.framework.testrail.models.*;
import com.scmp.framework.testrail.models.requests.AddTestResultRequest;
import com.scmp.framework.testrail.models.requests.AddTestRunRequest;
import okhttp3.*;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

/**
 * TestRailManager - Manages the interaction with TestRail for various operations such as fetching test runs, test cases, and uploading results.
 */
@Component
public class TestRailManager {
	private static final Logger frameworkLogger = LoggerFactory.getLogger(TestRailManager.class);
	private Retrofit retrofit;
	private final FrameworkConfigs configs;

	@Autowired
	public TestRailManager(FrameworkConfigs configs) {
		this.configs = configs;
		if (this.configs.isTestRailUploadTestResult()) {
			this.init();
		}
	}

	/**
	 * Initialize the TestRail connection.
	 */
	public void init() {
		frameworkLogger.info("Initializing TestRailManager...");

		String baseUrl = configs.getTestRailServer();
		String userName = configs.getTestRailUserName();
		String apiKey = configs.getTestRailAPIKey();

		if (baseUrl == null || baseUrl.isEmpty() || userName == null || userName.isEmpty() || apiKey == null || apiKey.isEmpty()) {
			throw new IllegalArgumentException(
					String.format("IllegalArgument found: BaseUrl=[%s], UserName=[%s], APIKey=[%s]", baseUrl, userName, apiKey));
		}

		if (!baseUrl.endsWith("/")) {
			baseUrl += "/";
		}

		OkHttpClient client = new OkHttpClient.Builder()
				.addInterceptor(new BasicAuthInterceptor(userName, apiKey))
				.writeTimeout(30, TimeUnit.SECONDS)
				.build();

		retrofit = new Retrofit.Builder()
				.baseUrl(baseUrl)
				.client(client)
				.addConverterFactory(GsonConverterFactory.create())
				.build();

		String inProgressId = configs.getTestRailStatusInProgressId();
		if (inProgressId != null && Pattern.compile("[0-9]+").matcher(inProgressId).matches()) {
			TestRailStatus.IN_PROGRESS = Integer.parseInt(inProgressId);
		} else {
			// Default use TestRailStatus.Retest for TestRailStatus.IN_PROGRESS
			TestRailStatus.IN_PROGRESS = TestRailStatus.Retest;
		}

		frameworkLogger.info("TestRailManager Initialized.");
	}

	/**
	 * Fetch test runs created after a specific timestamp.
	 *
	 * @param projectId the project ID
	 * @param timestamp the timestamp to filter test runs
	 * @return the test run result
	 * @throws IOException if an I/O error occurs
	 */
	public TestRunResult getTestRuns(String projectId, String timestamp) throws IOException {
		String CustomQuery = String.format(TestRailService.GET_TEST_RUNS_API, projectId);
		TestRailService service = retrofit.create(TestRailService.class);

		Map<String, String> data = new LinkedHashMap<>();
		data.put(CustomQuery, "");
		data.put("created_after", timestamp);

		retrofit2.Response<TestRunResult> response = service.getTestRuns(data).execute();
		if (!response.isSuccessful()) {
			frameworkLogger.error("Request getTestRuns Failed with Error Code: {}, Error Body: {}", response.code(), response.errorBody().string());
			frameworkLogger.error("ProjectId: {}, Timestamp: {}", projectId, timestamp);
		}

		return response.body();
	}

	/**
	 * Fetch automated test cases with pagination support.
	 *
	 * @param projectId the project ID
	 * @param offset    the offset for pagination
	 * @return the test case result
	 * @throws IOException if an I/O error occurs
	 */
	public TestCaseResult getAutomatedTestCases(String projectId, Integer offset) throws IOException {
		String CustomQuery = String.format(TestRailService.GET_TEST_CASES_API, projectId);
		TestRailService service = retrofit.create(TestRailService.class);

		Map<String, String> data = new LinkedHashMap<>();
		data.put(CustomQuery, "");
		data.put(TestCase.TYPE_ID, TestCase.TYPE_AUTOMATED);
		data.put("offset", offset.toString());

		retrofit2.Response<TestCaseResult> response = service.getTestCases(data).execute();
		if (!response.isSuccessful()) {
			frameworkLogger.error("Request getTestCases Failed with Error Code: {}, Error Body: {}", response.code(), response.errorBody().string());
			frameworkLogger.error("ProjectId: {}", projectId);
		}

		return response.body();
	}

	/**
	 * Fetch all automated test cases for a project.
	 *
	 * @param projectId the project ID
	 * @return a list of test run tests
	 * @throws IOException if an I/O error occurs
	 */
	public List<TestRunTest> getAllAutomatedTestCases(String projectId) throws IOException {
		List<TestRunTest> allResults = new ArrayList<>();
		int offset = 0;
		boolean withNextPage = true;
		while (withNextPage) {
			TestCaseResult result = this.getAutomatedTestCases(projectId, offset);
			if (!result.getTestRunTestList().isEmpty()) {
				allResults.addAll(result.getTestRunTestList());
			}

			if (result.getPagingLinks().getNext() != null) {
				offset += result.getSize();
			} else {
				withNextPage = false;
			}
		}

		return allResults;
	}

	/**
	 * Fetch test results for a specific test case.
	 *
	 * @param runId      the test run ID
	 * @param testcaseId the test case ID
	 * @return a list of test results
	 * @throws IOException if an I/O error occurs
	 */
	public List<TestResult> getTestResultsForTestCase(int runId, int testcaseId) throws IOException {
		String CustomQuery = String.format(TestRailService.GET_TEST_RESULTS_FOR_TEST_CASE_API, runId, testcaseId);
		TestRailService service = retrofit.create(TestRailService.class);

		Map<String, String> data = new LinkedHashMap<>();
		data.put(CustomQuery, "");

		retrofit2.Response<List<TestResult>> response = service.getTestResultsForTestCase(data).execute();
		if (!response.isSuccessful()) {
			frameworkLogger.error("Request getTestCases Failed with Error Code: {}, Error Body: {}", response.code(), response.errorBody().string());
			frameworkLogger.error("RunId: {}, TestCaseId: {}", runId, testcaseId);
		}

		return response.body();
	}

	/**
	 * Fetch a specific test run.
	 *
	 * @param testRunId the test run ID
	 * @return the test run
	 * @throws IOException if an I/O error occurs
	 */
	public TestRun getTestRun(String testRunId) throws IOException {
		String CustomQuery = String.format(TestRailService.GET_TEST_RUN_API, testRunId);
		TestRailService service = retrofit.create(TestRailService.class);

		Map<String, String> data = new LinkedHashMap<>();
		data.put(CustomQuery, "");

		retrofit2.Response<TestRun> response = service.getTestRun(data).execute();
		if (!response.isSuccessful()) {
			frameworkLogger.error("Request getTestRun Failed with Error Code: {}, Error Body: {}", response.code(), response.errorBody().string());
			frameworkLogger.error("RunId: {}", testRunId);
		}

		return response.body();
	}

	/**
	 * Fetch test run tests with pagination support.
	 *
	 * @param testRunId          the test run ID
	 * @param statusFilterString the status filter string
	 * @param offset             the offset for pagination
	 * @return the test run test result
	 * @throws IOException if an I/O error occurs
	 */
	public TestRunTestResult getTestRunTests(int testRunId, String statusFilterString, Integer offset) throws IOException {
		String CustomQuery = String.format(TestRailService.GET_TESTS_API, testRunId);
		TestRailService service = retrofit.create(TestRailService.class);

		Map<String, String> data = new LinkedHashMap<>();
		data.put(CustomQuery, "");
		data.put("status_id", statusFilterString);
		data.put("offset", offset.toString());

		retrofit2.Response<TestRunTestResult> response = service.getTestRunTests(data).execute();
		if (!response.isSuccessful()) {
			frameworkLogger.error("Request getTestRunTests Failed with Error Code: {}, Error Body: {}", response.code(), response.errorBody().string());
			frameworkLogger.error("RunId: {}, Status: {}", testRunId, statusFilterString);
		}

		return response.body();
	}

	/**
	 * Fetch all test run tests for a specific test run.
	 *
	 * @param testRunId          the test run ID
	 * @param statusFilterString the status filter string
	 * @return a list of test run tests
	 * @throws IOException if an I/O error occurs
	 */
	public List<TestRunTest> getAllTestRunTests(int testRunId, String statusFilterString) throws IOException {
		List<TestRunTest> allResults = new ArrayList<>();
		int offset = 0;
		boolean withNextPage = true;
		while (withNextPage) {
			TestRunTestResult result = this.getTestRunTests(testRunId, statusFilterString, offset);
			if (!result.getTestRunTestList().isEmpty()) {
				allResults.addAll(result.getTestRunTestList());
			}

			if (result.getPagingLinks().getNext() != null) {
				offset += result.getSize();
			} else {
				withNextPage = false;
			}
		}

		return allResults;
	}

	/**
	 * Add a new test run.
	 *
	 * @param projectId         the project ID
	 * @param testRunName       the name of the test run
	 * @param includeTestCaseIds the list of test case IDs to include
	 * @return the created test run
	 * @throws IOException if an I/O error occurs
	 */
	public TestRun addTestRun(String projectId, String testRunName, List<Integer> includeTestCaseIds) throws IOException {
		if (includeTestCaseIds == null) {
			includeTestCaseIds = new ArrayList<>();
		}

		AddTestRunRequest request = new AddTestRunRequest(testRunName, includeTestCaseIds.isEmpty(), includeTestCaseIds);

		String CustomQuery = String.format(TestRailService.ADD_TEST_RUN_API, projectId);
		TestRailService service = retrofit.create(TestRailService.class);

		Map<String, String> data = new LinkedHashMap<>();
		data.put(CustomQuery, "");
		TestRun testRun = null;

		retrofit2.Response<TestRun> response = service.addTestRun(data, request).execute();
		if (!response.isSuccessful()) {
			frameworkLogger.error("Request addTestRun Failed with Error Code: {}, Error Body: {}", response.code(), response.errorBody().string());
			frameworkLogger.error("ProjectId: {}, TestRunName: {}, includeTestCaseIds: {}", projectId, testRunName, includeTestCaseIds);
		} else {
			testRun = response.body();
			testRun.setTestCaseIds(includeTestCaseIds);
		}

		return testRun;
	}

	/**
	 * Update an existing test run.
	 *
	 * @param testRun the test run to update
	 * @return the updated test run
	 * @throws IOException if an I/O error occurs
	 */
	public TestRun updateTestRun(TestRun testRun) throws IOException {
		AddTestRunRequest request = new AddTestRunRequest(testRun.getName(), testRun.getIncludeAll(), testRun.getTestCaseIds());

		String CustomQuery = String.format(TestRailService.UPDATE_TEST_RUN_API, testRun.getId());
		TestRailService service = retrofit.create(TestRailService.class);

		Map<String, String> data = new LinkedHashMap<>();
		data.put(CustomQuery, "");

		TestRun updatedTestRun = null;
		retrofit2.Response<TestRun> response = service.updateTestRun(data, request).execute();
		if (!response.isSuccessful()) {
			frameworkLogger.error("Request updateTestRun Failed with Error Code: {}, Error Body: {}", response.code(), response.errorBody().string());
			frameworkLogger.error("RunName: {}", request.getName());
		} else {
			updatedTestRun = response.body();
			updatedTestRun.setTestCaseIds(testRun.getTestCaseIds());
		}

		return updatedTestRun;
	}

	/**
	 * Add a test result for a specific test case.
	 *
	 * @param testRunId  the test run ID
	 * @param testCaseId the test case ID
	 * @param request    the request containing test result details
	 * @return the created test result
	 * @throws IOException if an I/O error occurs
	 */
	public TestResult addTestResult(Integer testRunId, Integer testCaseId, AddTestResultRequest request) throws IOException {
		String CustomQuery = String.format(TestRailService.ADD_RESULT_FOR_TEST_CASE_API, testRunId, testCaseId);
		TestRailService service = retrofit.create(TestRailService.class);

		Map<String, String> data = new LinkedHashMap<>();
		data.put(CustomQuery, "");

		retrofit2.Response<TestResult> response = service.addResultForTestCase(data, request).execute();
		if (!response.isSuccessful()) {
			frameworkLogger.error("Request addResultForTestCase Failed with Error Code: {}, Error Body: {}", response.code(), response.errorBody().string());
			frameworkLogger.error("RunId: {}, TestCaseId: {}", testRunId, testCaseId);
		}

		return response.body();
	}

	/**
	 * Add an attachment to a specific test run.
	 *
	 * @param testRunId the test run ID
	 * @param imagePath the path of the image to upload
	 * @return the created attachment
	 * @throws IOException if an I/O error occurs
	 */
	public Attachment addAttachmentToTestRun(Integer testRunId, String imagePath) throws IOException {
		String CustomQuery = String.format(TestRailService.ADD_ATTACHMENT_FOR_TEST_RUN_API, testRunId);
		TestRailService service = retrofit.create(TestRailService.class);

		Map<String, String> data = new LinkedHashMap<>();
		data.put(CustomQuery, "");

		File file = new File(imagePath);
		RequestBody requestFile = RequestBody.create(MediaType.parse("multipart/form-data"), file);
		MultipartBody.Part imageToUpload = MultipartBody.Part.createFormData("attachment", file.getName(), requestFile);

		retrofit2.Response<Attachment> response = service.addAttachment(data, imageToUpload).execute();
		if (!response.isSuccessful()) {
			frameworkLogger.error("Request addAttachment Failed with Error Code: {}, Error Body: {}", response.code(), response.errorBody().string());
			frameworkLogger.error("RunId: {}", testRunId);
		}

		return response.body();
	}

	/**
	 * Add an attachment to a specific test result.
	 *
	 * @param testResultId the test result ID
	 * @param imagePath    the path of the image to upload
	 * @return the created attachment
	 * @throws IOException if an I/O error occurs
	 */
	public Attachment addAttachmentToTestResult(Integer testResultId, String imagePath) throws IOException {
		String CustomQuery = String.format(TestRailService.ADD_ATTACHMENT_FOR_TEST_RESULT_API, testResultId);
		TestRailService service = retrofit.create(TestRailService.class);

		Map<String, String> data = new LinkedHashMap<>();
		data.put(CustomQuery, "");

		File file = new File(imagePath);
		RequestBody requestFile = RequestBody.create(MediaType.parse("multipart/form-data"), file);
		MultipartBody.Part imageToUpload = MultipartBody.Part.createFormData("attachment", file.getName(), requestFile);

		retrofit2.Response<Attachment> response = service.addAttachment(data, imageToUpload).execute();
		if (!response.isSuccessful()) {
			frameworkLogger.error("Request addAttachment Failed with Error Code: {}, Error Body: {}", response.code(), response.errorBody().string());
			frameworkLogger.error("TestResultId: {}, Image: {}", testResultId, imagePath);
		}

		return response.body();
	}
}

/**
 * BasicAuthInterceptor - Intercepts HTTP requests to add basic authentication headers.
 */
class BasicAuthInterceptor implements Interceptor {
	private final String credentials;

	public BasicAuthInterceptor(String user, String password) {
		this.credentials = Credentials.basic(user, password);
	}

	@NotNull
	@Override
	public Response intercept(Chain chain) throws IOException {
		Request request = chain.request();
		Request authenticatedRequest = request.newBuilder().header("Authorization", credentials).build();
		return chain.proceed(authenticatedRequest);
	}
}