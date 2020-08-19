package me.vinceh121.gmcserver.entities;

import org.bson.codecs.pojo.annotations.BsonIgnore;

import com.fasterxml.jackson.annotation.JsonIgnore;

import io.vertx.core.json.JsonObject;
import me.vinceh121.gmcserver.mfa.MFAKey;
import xyz.bowser65.tokenize.IAccount;

/**
 * MFA:
 * When setting up MFA, the key will be set, however the boolean mfa will be
 * false, once the confirmation password has been sent, it will be true and MFA
 * setup is complete
 *
 */
public class User extends AbstractEntity implements IAccount {
	private String username, password, email;
	/**
	 * -1 = instance default
	 */
	private int deviceLimit = -1;
	private long gmcId;
	private boolean admin;
	private MFAKey mfaKey;
	private boolean mfa;

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
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	@Override
	@JsonIgnore
	@BsonIgnore
	public String getTokenId() {
		return this.getId().toHexString();
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
		return obj;
	}

	@Override
	public String toString() {
		return this.getUsername() + " (" + this.getId().toString() + ")";
	}

}
