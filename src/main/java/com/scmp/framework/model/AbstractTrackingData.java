package com.scmp.framework.model;

import lombok.Getter;

import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLDecoder;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * AbstractTrackingData - Abstract base class for handling tracking data.
 */
@Getter
public abstract class AbstractTrackingData {
	private String originalUrl = "";
	private Map<String, String> variables = new HashMap<>();

	/**
	 * Constructor to initialize AbstractTrackingData with the original URL.
	 *
	 * @param originalUrl the original tracking URL
	 */
	public AbstractTrackingData(String originalUrl) {
		this.originalUrl = originalUrl;
		this.parse(originalUrl);
	}

	/**
	 * Parses the given URL and extracts the query parameters.
	 *
	 * @param url the URL to parse
	 */
	public void parse(String url) {
		try {
			this.variables = this.splitQuery(new URL(url));
		} catch (Exception e) {
			throw new RuntimeException("Unable to parse URL: " + url, e);
		}
	}

	/**
	 * Splits the query string of the given URL into a map of key-value pairs.
	 *
	 * @param url the URL containing the query string
	 * @return a map of query parameters
	 * @throws UnsupportedEncodingException if the encoding is not supported
	 */
	public Map<String, String> splitQuery(URL url) throws UnsupportedEncodingException {
		Map<String, String> queryPairs = new LinkedHashMap<>();
		String query = url.getQuery();
		String[] pairs = query.split("&");

		for (String pair : pairs) {
			int idx = pair.indexOf("=");
			if (idx < 0) {
				queryPairs.put(URLDecoder.decode(pair, "UTF-8"), "");
			} else {
				queryPairs.put(URLDecoder.decode(pair.substring(0, idx), "UTF-8"), URLDecoder.decode(pair.substring(idx + 1), "UTF-8"));
			}
		}

		return queryPairs;
	}
}