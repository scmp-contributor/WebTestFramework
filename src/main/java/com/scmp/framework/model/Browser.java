package com.scmp.framework.model;

public enum Browser {
	RANDOM("RANDOM"),
	CHROME("CHROME"),
	FIREFOX("FIREFOX");

	private final String name;

	Browser(String name) {
		this.name = name;
	}

	@Override
	public String toString() {
		return this.name;
	}
}
