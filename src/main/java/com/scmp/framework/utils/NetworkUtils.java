package com.scmp.framework.utils;

import com.scmp.framework.model.ChartbeatData;
import com.scmp.framework.model.GoogleAnalytics;
import com.scmp.framework.model.GoogleAnalytics4;
import org.json.JSONObject;
import org.openqa.selenium.logging.LogEntries;
import org.openqa.selenium.logging.LogEntry;
import org.openqa.selenium.remote.RemoteWebDriver;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * NetworkUtils - Utility class for handling network traffic and extracting tracking requests.
 */
public class NetworkUtils {

	/**
	 * Clears the network traffic logs.
	 *
	 * @param driver the RemoteWebDriver instance
	 */
	public static void clearNetworkTraffic(RemoteWebDriver driver) {
		getAllNetworkTraffic(driver);
	}

	/**
	 * Retrieves all network traffic logs.
	 *
	 * @param driver the RemoteWebDriver instance
	 * @return a list of network traffic log messages
	 */
	public static List<String> getAllNetworkTraffic(RemoteWebDriver driver) {
		List<String> messages = new ArrayList<>();

		// Get performance logs from the driver
		LogEntries logs = driver.manage().logs().get("performance");
		for (LogEntry entry : logs) {
			messages.add(entry.getMessage());
		}

		return messages;
	}

	/**
	 * Retrieves tracking requests based on a specified pattern.
	 *
	 * @param driver  the RemoteWebDriver instance
	 * @param cls     the class type of the tracking data
	 * @param pattern the regex pattern to match URLs
	 * @param <T>     the type of tracking data
	 * @return a list of tracking data
	 */
	private static <T> List<T> getTrackingRequests(RemoteWebDriver driver, Class<T> cls, String pattern) {
		List<T> trackingData = Collections.synchronizedList(new ArrayList<>());
		Pattern regex = Pattern.compile(pattern);

		// Get performance logs from the driver
		LogEntries logs = driver.manage().logs().get("performance");
		logs.getAll()
				.parallelStream()
				.forEach(entry -> {
					JSONObject json = new JSONObject(entry.getMessage());
					JSONObject message = json.getJSONObject("message");
					String method = message.getString("method");
					if (method.equals("Network.requestWillBeSent")) {
						JSONObject params = message.getJSONObject("params");
						JSONObject request = params.getJSONObject("request");
						String url = request.getString("url");

						Matcher matcher = regex.matcher(url);
						if (matcher.matches()) {
							try {
								trackingData.add(cls.getConstructor(String.class).newInstance(url));
							} catch (Exception e) {
								e.printStackTrace();
							}
						}
					}
				});

		return trackingData;
	}

	/**
	 * Retrieves Google Analytics 4 tracking requests based on a specified pattern.
	 *
	 * @param driver  the RemoteWebDriver instance
	 * @param pattern the regex pattern to match URLs
	 * @return a map of event names to lists of Google Analytics 4 tracking data
	 */
	private static Map<String, List<GoogleAnalytics4>> getGA4TrackingRequests(RemoteWebDriver driver, String pattern) {
		Map<String, List<GoogleAnalytics4>> trackingData = new HashMap<>();
		Pattern regex = Pattern.compile(pattern);

		// Get performance logs from the driver
		LogEntries logs = driver.manage().logs().get("performance");
		logs.getAll()
				.parallelStream()
				.forEach(entry -> {
					JSONObject json = new JSONObject(entry.getMessage());
					JSONObject message = json.getJSONObject("message");
					String method = message.getString("method");
					if (method.equals("Network.requestWillBeSent")) {
						JSONObject params = message.getJSONObject("params");
						JSONObject request = params.getJSONObject("request");
						String url = request.getString("url");

						Matcher matcher = regex.matcher(url);
						if (matcher.matches()) {
							boolean hasPostData = request.optBoolean("hasPostData", false);

							try {
								if (hasPostData && request.has("postData")) {
									String postData = request.getString("postData");
									String[] events = postData.split("\r\n");

									// Get all the events and add them into list
									for (String event : events) {
										GoogleAnalytics4 ga4Data = new GoogleAnalytics4(url, event);
										String en = ga4Data.getEventName();

										List<GoogleAnalytics4> ga4Datas =
												trackingData.computeIfAbsent(en, k -> new ArrayList<>());

										ga4Datas.add(ga4Data);
									}
								} else {
									GoogleAnalytics4 event = new GoogleAnalytics4(url);
									String en = event.getEventName();

									List<GoogleAnalytics4> ga4Datas = trackingData.computeIfAbsent(en, k -> new ArrayList<>());
									ga4Datas.add(event);
								}
							} catch (Exception e) {
								e.printStackTrace();
							}
						}
					}
				});

		return trackingData;
	}

	/**
	 * Retrieves Google Analytics tracking requests.
	 *
	 * @param driver the RemoteWebDriver instance
	 * @return a list of Google Analytics tracking data
	 */
	public static List<GoogleAnalytics> getGoogleAnalyticsRequests(RemoteWebDriver driver) {
		String pattern = "^https://www.google-analytics.com/([a-z]/)?collect\\?.+";
		return getTrackingRequests(driver, GoogleAnalytics.class, pattern);
	}

	/**
	 * Retrieves Google Analytics 4 tracking requests.
	 *
	 * @param driver the RemoteWebDriver instance
	 * @return a map of event names to lists of Google Analytics 4 tracking data
	 */
	public static Map<String, List<GoogleAnalytics4>> getGoogleAnalytics4Requests(RemoteWebDriver driver) {
		String pattern = "^https://(analytics.google.com|www.google-analytics.com)/([a-z]/)?collect\\?.+(tid=G-).+";
		return getGA4TrackingRequests(driver, pattern);
	}

	/**
	 * Retrieves Chartbeat tracking requests.
	 *
	 * @param driver the RemoteWebDriver instance
	 * @return a list of Chartbeat tracking data
	 */
	public static List<ChartbeatData> getChartBeatRequests(RemoteWebDriver driver) {
		String pattern = "^https://ping.chartbeat.net/ping\\?.+";
		return getTrackingRequests(driver, ChartbeatData.class, pattern);
	}
}