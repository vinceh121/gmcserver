package me.vinceh121.gmcserver.entities;

import org.bson.codecs.pojo.annotations.BsonIgnore;

import com.fasterxml.jackson.annotation.JsonIgnore;

import me.vinceh121.gmcserver.mfa.MFAKey;
import xyz.bowser65.tokenize.IAccount;

/**
 * MFA:
 * When settings up MFA, the key will be set, however the boolean mfa will be
 * false, once the confirmation password has been sent, it will be true and MFA
 * setup is complete
 *
 */
public class User extends AbstractEntity implements IAccount {
	private String username, password;
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
		return admin;
	}

	public void setAdmin(boolean admin) {
		this.admin = admin;
	}

	public MFAKey getMfaKey() {
		return mfaKey;
	}

	public void setMfaKey(MFAKey key) {
		this.mfaKey = key;
	}

	public boolean isMfa() {
		return mfa;
	}

	public void setMfa(boolean mfa) {
		this.mfa = mfa;
	}

	@Override
	@JsonIgnore
	@BsonIgnore
	public String getTokenId() {
		return this.getId().toHexString();
	}

	@Override
	public long tokensValidSince() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public String toString() {
		return this.getUsername() + " (" + this.getId().toString() + ")";
	}

}
