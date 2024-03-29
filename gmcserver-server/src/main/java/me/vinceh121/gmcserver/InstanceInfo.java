/*
 * GMCServer, lightweight service to log, analyze and proxy Geiger counter data.
 * Copyright (C) 2020 Vincent Hyvert
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public for more details.
 *
 * You should have received a copy of the GNU Affero General Public
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
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
		return this.captcha;
	}

	public void setCaptcha(final boolean captcha) {
		this.captcha = captcha;
	}

	public void fromProperties(final Properties map) {
		this.host = map.getProperty("instance.host");
		this.name = map.getProperty("instance.name");
		this.about = map.getProperty("instance.about");
		this.captcha = Boolean.parseBoolean(map.getProperty("captcha.enabled"));
	}
}
