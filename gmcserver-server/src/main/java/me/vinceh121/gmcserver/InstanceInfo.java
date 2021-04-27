package me.vinceh121.gmcserver;

import java.util.Properties;

public class InstanceInfo {
	private String host, name, about;
	private boolean captcha;

	public String getHost() {
		return this.host;
	}

	public void setHost(final String host) {
		this.host = host;
	}

	public String getName() {
		return this.name;
	}

	public void setName(final String name) {
		this.name = name;
	}

	public String getAbout() {
		return this.about;
	}

	public void setAbout(final String about) {
		this.about = about;
	}

	public boolean isCaptcha() {
		return captcha;
	}

	public void setCaptcha(boolean captcha) {
		this.captcha = captcha;
	}

	public void fromProperties(final Properties map) {
		this.host = map.getProperty("instance.host");
		this.name = map.getProperty("instance.name");
		this.about = map.getProperty("instance.about");
		this.captcha = Boolean.parseBoolean(map.getProperty("captcha.enabled"));
	}
}
