package me.vinceh121.gmcserver;

import java.util.Properties;

public class InstanceInfo {
	private String host, name, about;

	public String getHost() {
		return host;
	}

	public void setHost(String host) {
		this.host = host;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getAbout() {
		return about;
	}

	public void setAbout(String about) {
		this.about = about;
	}

	public void fromProperties(final Properties map) {
		this.host = map.getProperty("instance.host");
		this.name = map.getProperty("instance.name");
		this.about = map.getProperty("instance.about");
	}
}
