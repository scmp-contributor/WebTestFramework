package com.scmp.framework.model;

public enum ChartbeatParameter {
	Host("h"),
	Path("p"),
	Domain("d"),
	Sections("g0"),
	Authors("g1"),
	Page_Title("i"),
	Referring_Url_External("r"),
	Referring_Url_Internal("v"),
	Page_session("t");

	private final String parameter;

	ChartbeatParameter(String parameter) {
		this.parameter = parameter;
	}

	@Override
	public String toString() {
		return this.parameter;
	}
}

