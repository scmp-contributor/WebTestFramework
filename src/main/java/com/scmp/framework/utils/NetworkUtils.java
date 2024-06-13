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
import java.util.stream.Collectors;

public class NetworkUtils {

	public static void clearNetworkTraffic(RemoteWebDriver driver) {
		getAllNetworkTraffic(driver);
	}

	public static List<String> getAllNetworkTraffic(RemoteWebDriver driver) {
		List<String> messages = new ArrayList<>();

		LogEntries logs = driver.manage().logs().get("performance");
		for (LogEntry entry : logs) {
			messages.add(entry.getMessage());
		}

		return messages;
	}

	private static <T> List<T> getTrackingRequests(RemoteWebDriver driver, Class<T> cls, String pattern) {
		List<T> trackingData = Collections.synchronizedList(new ArrayList<>());
		// Create a Pattern object
		Pattern regex = Pattern.compile(pattern);

		LogEntries logs = driver.manage().logs().get("performance");
		logs.getAll()
				.parallelStream()
				.forEach(entry -> {
					JSONObject json = new JSONObject(entry.getMessage());
					JSONObject message = (JSONObject) json.get("message");
					String method = (String) message.get("method");
					if (method.equals("Network.requestWillBeSent")) {
						JSONObject params = (JSONObject) message.get("params");
						JSONObject request = (JSONObject) params.get("request");
						String url = (String) request.get("url");

						Matcher matcher = regex.matcher(url);
						if (matcher.matches()) {
							System.out.println(entry.getMessage());
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

	private static Map<String, List<GoogleAnalytics4>> getGA4TrackingRequests(RemoteWebDriver driver, String pattern) {
		Map<String, List<GoogleAnalytics4>> trackingData = new HashMap<>();
		// Create a Pattern object
		Pattern regex = Pattern.compile(pattern);

		LogEntries logs = driver.manage().logs().get("performance");
		logs.getAll()
				.parallelStream()
				.forEach(entry -> {
					JSONObject json = new JSONObject(entry.getMessage());
					JSONObject message = (JSONObject) json.get("message");
					String method = (String) message.get("method");
					if (method.equals("Network.requestWillBeSent")) {

						JSONObject params = (JSONObject) message.get("params");
						JSONObject request = (JSONObject) params.get("request");
						String url = (String) request.get("url");

						Matcher matcher = regex.matcher(url);
						if (matcher.matches()) {
							boolean hasPostData = request.optBoolean("hasPostData", false);

							try{
								if(hasPostData && request.has("postData")){
									String postData = (String) request.get("postData");

									String[] events = postData.split("\r\n");

									// Get all the events and add them into list
									for(int i = 0; i < events.length; i++){

										GoogleAnalytics4 ga4Data = new GoogleAnalytics4(url, events[i]);
										String en = ga4Data.getEventName();

										List<GoogleAnalytics4> ga4Datas =
												trackingData.computeIfAbsent(en, k -> new ArrayList<>());

										ga4Datas.add(ga4Data);
									}

								} else {
									GoogleAnalytics4 event = new GoogleAnalytics4(url);
									String en = event.getEventName();

									List<GoogleAnalytics4> ga4Datas =
											trackingData.computeIfAbsent(en, k -> new ArrayList<>());

									ga4Datas.add(event);
								}
							}catch (Exception e){
								e.printStackTrace();
							}
						}
					}
				});

		return trackingData;
	}

	public static List<GoogleAnalytics> getGoogleAnalyticsRequests(RemoteWebDriver driver) {
		List<GoogleAnalytics> gaData = Collections.synchronizedList(new ArrayList<>());
		String pattern = "^https://www.google-analytics.com/([a-z]/)?collect\\?.+";
		return getTrackingRequests(driver, GoogleAnalytics.class, pattern);
	}

	public static Map<String, List<GoogleAnalytics4>> getGoogleAnalytics4Requests(RemoteWebDriver driver) {
		List<GoogleAnalytics> gaData = Collections.synchronizedList(new ArrayList<>());
		String pattern = "^https://(analytics.google.com|www.google-analytics.com)/([a-z]/)?collect\\?.+(tid=G-).+";
		return getGA4TrackingRequests(driver, pattern);
	}

	public static List<ChartbeatData> getChartBeatRequests(RemoteWebDriver driver) {
		List<GoogleAnalytics> gaData = Collections.synchronizedList(new ArrayList<>());
		String pattern = "^https://ping.chartbeat.net/ping\\?.+";
		return getTrackingRequests(driver, ChartbeatData.class, pattern);
	}
}
