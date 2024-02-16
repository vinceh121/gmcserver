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
package me.vinceh121.gmcserver.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;

import io.vertx.core.json.JsonObject;
import me.vinceh121.gmcserver.mfa.MFAKey;
import xyz.bowser65.tokenize.IAccount;

/**
 * MFA: When setting up MFA, the key will be set, however the boolean mfa will
 * be false, once the confirmation password has been sent, it will be true and
 * MFA setup is complete
 *
 */
public class User extends AbstractEntity implements IAccount {
	private String username, password, email;
	/**
	 * -1 = instance default
	 */
	private int deviceLimit = -1;
	private long gmcId;
	private boolean admin, mfa, alertEmails;
	private MFAKey mfaKey;

	public long getGmcId() {
		return this.gmcId;
	}

	public void setGmcId(final long gmcId) {
		this.gmcId = gmcId;
	}

	public String getUsername() {
		return this.username;
	}

	public void setUsername(final String username) {
		this.username = username;
	}

	public String getPassword() {
		return this.password;
	}

	public void setPassword(final String password) {
		this.password = password;
	}

	public int getDeviceLimit() {
		return this.deviceLimit;
	}

	public void setDeviceLimit(final int deviceLimit) {
		this.deviceLimit = deviceLimit;
	}

	public boolean isAdmin() {
		return this.admin;
	}

	public void setAdmin(final boolean admin) {
		this.admin = admin;
	}

	public MFAKey getMfaKey() {
		return this.mfaKey;
	}

	public void setMfaKey(final MFAKey key) {
		this.mfaKey = key;
	}

	public boolean isMfa() {
		return this.mfa;
	}

	public void setMfa(final boolean mfa) {
		this.mfa = mfa;
	}

	public String getEmail() {
		return this.email;
	}

	public void setEmail(final String email) {
		this.email = email;
	}

	public boolean isAlertEmails() {
		return alertEmails;
	}

	public void setAlertEmails(final boolean alertEmails) {
		this.alertEmails = alertEmails;
	}

	@Override
	@JsonIgnore
	public String getTokenId() {
		return this.getId().toString();
	}

	@Override
	@JsonIgnore
	public long tokensValidSince() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public JsonObject toJson() {
		final JsonObject obj = super.toJson();
		obj.remove("mfaKey");
		obj.remove("password");
		return obj;
	}

	@Override
	public JsonObject toPublicJson() {
		final JsonObject obj = super.toPublicJson();
		obj.remove("mfa");
		obj.remove("gmcId");
		obj.remove("email");
		obj.remove("deviceLimit");
		obj.remove("alertEmails");
		return obj;
	}

	@Override
	public String toString() {
		return this.getUsername() + " (" + this.getId().toString() + ")";
	}

	public static String sqlFields() {
		return AbstractEntity.sqlFields(User.class);
	}
}
