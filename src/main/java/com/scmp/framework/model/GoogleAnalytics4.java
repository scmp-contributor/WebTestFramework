package com.scmp.framework.model;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * GoogleAnalytics4 - Class for handling Google Analytics 4 tracking data.
 */
public class GoogleAnalytics4 extends AbstractTrackingData {

	private Map<String, String> parameters;

	/**
	 * Constructor to initialize GoogleAnalytics4 with original data and query string.
	 *
	 * @param original the original tracking data
	 * @param query    the query string containing parameters
	 */
	public GoogleAnalytics4(String original, String query) {
		super(original);
		this.parameters = new HashMap<>();

		String[] parameters = query.split("&");

		Arrays.stream(parameters).forEach(parameter -> {
			String[] keyValue = parameter.split("=");

			// Handle empty case
			if (keyValue.length == 1) {
				this.parameters.put(keyValue[0], "");
			} else {
				this.parameters.put(keyValue[0], keyValue[1]);
			}
		});
	}

	/**
	 * Constructor to initialize GoogleAnalytics4 with original data.
	 *
	 * @param original the original tracking data
	 */
	public GoogleAnalytics4(String original) {
		super(original);
	}

	/**
	 * Retrieves the event name from the parameters.
	 *
	 * @return the event name
	 */
	public String getEventName() {
		if (parameters != null) {
			return parameters.get(GoogleAnalytics4Parameter.EVENT_NAME.toString());
		} else {
			return this.getValue(GoogleAnalytics4Parameter.EVENT_NAME);
		}
	}

	/**
	 * Retrieves event data for a given key.
	 *
	 * @param key the key for the event data
	 * @return the event data
	 */
	public String getEventData(String key) {
		if (parameters != null) {
			return parameters.getOrDefault(key, null);
		} else {
			return this.getValue(key);
		}
	}

	/**
	 * Retrieves the document location.
	 *
	 * @return the document location
	 */
	public String getDocumentLocation() {
		return this.getValue(GoogleAnalytics4Parameter.DOCUMENT_LOCATION);
	}

	/**
	 * Retrieves the value for a given GoogleAnalytics4Parameter.
	 *
	 * @param parameter the GoogleAnalytics4Parameter
	 * @return the value
	 */
	public String getValue(GoogleAnalytics4Parameter parameter) {
		return this.getVariables().get(parameter.toString());
	}

	/**
	 * Retrieves the value for a given parameter.
	 *
	 * @param parameter the parameter
	 * @return the value
	 */
	public String getValue(String parameter) {
		return this.getVariables().get(parameter);
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}

		if (!(o instanceof GoogleAnalytics4)) {
			return false;
		}

		GoogleAnalytics4 that = (GoogleAnalytics4) o;

		boolean isQueryEqual = this.getVariables().entrySet().stream().allMatch(entry -> {
			String key = entry.getKey();
			String value = entry.getValue();
			return value.equals(that.getVariables().get(key));
		});

		AtomicBoolean isParametersEqual = new AtomicBoolean(true);

		if (this.parameters != null && that.parameters != null) {
			if (that.parameters.size() == this.parameters.size()) {
				this.parameters.keySet().forEach(key -> {
					if (!that.parameters.containsKey(key) || !that.parameters.get(key).equals(this.parameters.get(key))) {
						isParametersEqual.set(false);
					}
				});
			} else {
				isParametersEqual.set(false);
			}
		} else isParametersEqual.set(this.parameters == null && that.parameters == null);

		return isQueryEqual && isParametersEqual.get();
	}
}