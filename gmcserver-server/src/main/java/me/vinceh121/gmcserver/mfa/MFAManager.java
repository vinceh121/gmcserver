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
package me.vinceh121.gmcserver.mfa;

import java.security.InvalidKeyException;
import java.security.Key;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.bson.types.ObjectId;

import com.eatthepath.otp.TimeBasedOneTimePasswordGenerator;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Updates;

import io.vertx.core.Promise;
import me.vinceh121.gmcserver.GMCServer;
import me.vinceh121.gmcserver.actions.AbstractAction;
import me.vinceh121.gmcserver.entities.User;
import me.vinceh121.gmcserver.exceptions.AuthenticationException;
import me.vinceh121.gmcserver.managers.AbstractManager;

public class MFAManager extends AbstractManager {
	private final TimeBasedOneTimePasswordGenerator generator;
	private final KeyGenerator keyGen;
	private final int passwordLength, keySize;
	private final Duration timeStep;
	private final String algorithm;

	public MFAManager(final GMCServer srv) {
		super(srv);
		this.passwordLength = Integer.parseInt(srv.getConfig().getProperty("totp.length"));
		this.timeStep = Duration.ofSeconds(Long.parseLong(srv.getConfig().getProperty("totp.duration")));
		this.algorithm = srv.getConfig().getProperty("totp.algo");
		this.keySize = Integer.parseInt(srv.getConfig().getProperty("totp.keysize"));
		try {
			this.generator = new TimeBasedOneTimePasswordGenerator(this.timeStep, this.passwordLength, this.algorithm);
			this.keyGen = KeyGenerator.getInstance(this.algorithm);
			this.keyGen.init(this.keySize);
		} catch (final NoSuchAlgorithmException e) {
			throw new RuntimeException("Could not find matching TOTP algorithm", e);
		}
	}

	public boolean passwordMatches(final ObjectId id, final int pass) throws InvalidKeyException {
		return this.passwordMatches(
				this.srv.getDatabaseManager().getCollection(User.class).find(Filters.eq(id)).first(),
				pass);
	}

	public boolean passwordMatches(final User user, final int pass) throws InvalidKeyException {
		if (!user.isMfa()) {
			throw new IllegalArgumentException("User does not have MFA set");
		}

		final int actualPassword = this.generateOneTimePassword(user.getMfaKey(), Instant.now());
		return actualPassword == pass;
	}

	public MFAKey setupMFA(final User user) {
		final MFAKey key = this.generateKey();
		this.srv.getDatabaseManager()
			.getCollection(User.class)
			.updateOne(Filters.eq(user.getId()), Updates.set("mfaKey", key));
		user.setMfaKey(key);
		return key;
	}

	public boolean completeMfaSetup(final User user, final int pass) throws InvalidKeyException {
		final int actualPassword = this.generateOneTimePassword(user.getMfaKey(), Instant.now());
		final boolean matches = actualPassword == pass;
		if (matches) {
			this.srv.getDatabaseManager()
				.getCollection(User.class)
				.updateOne(Filters.eq(user.getId()), Updates.set("mfa", true));
			user.setMfa(true);
		}
		return matches;
	}

	public MFAKey generateKey() {
		final SecretKey key = this.keyGen.generateKey();
		final MFAKey mfa = new MFAKey();
		mfa.setAlgorithm(this.algorithm);
		mfa.setDigits(this.passwordLength);
		mfa.setKey(key.getEncoded());
		mfa.setPeriod((int) this.timeStep.getSeconds());
		return mfa;
	}

	public int generateOneTimePassword(final MFAKey key, final Instant timestamp) throws InvalidKeyException {
		return this.generateOneTimePassword(MFAManager.mfaKeyToSecretKey(key), timestamp);
	}

	public int generateOneTimePassword(final Key key, final Instant timestamp) throws InvalidKeyException {
		return this.generator.generateOneTimePassword(key, timestamp);
	}

	public static SecretKey mfaKeyToSecretKey(final MFAKey key) {
		final SecretKey secKey = new SecretKeySpec(key.getKey(), key.getAlgorithm());
		return secKey;
	}

	public SetupMFAAction setupMFA() {
		return new SetupMFAAction(this.srv);
	}

	public VerifyCodeAction verifyCode() {
		return new VerifyCodeAction(this.srv);
	}

	public DisableMFAAction disableMfa() {
		return new DisableMFAAction(this.srv);
	}

	/**
	 * Process an MFA setup step.
	 * 
	 * Throws {@code InvalidKeyException} if the stored key isn't valid.
	 * Throws {@code AuthenticationException} if the confirm code failed to validate.
	 */
	public class SetupMFAAction extends AbstractAction<String> {
		private User user;
		private int pass;

		public SetupMFAAction(final GMCServer srv) {
			super(srv);
		}

		@Override
		protected void executeSync(final Promise<String> promise) {
			if (this.user.getMfaKey() == null) { // MFA not setup at all
				final MFAKey key = this.srv.getMfaManager().setupMFA(this.user);
				promise.complete(key.toURI("GMCServer " + this.user.getUsername()));
			} else { // Complete MFA setup
				boolean matches;
				try {
					matches = this.srv.getMfaManager().completeMfaSetup(this.user, this.pass);
				} catch (final InvalidKeyException e) {
					promise.fail(new InvalidKeyException("Invalid MFA key", e));
					return;
				}
				if (matches) {
					promise.complete();
				} else {
					promise.fail(new AuthenticationException("Invalid pass"));
				}
			}
		}

		public User getUser() {
			return this.user;
		}

		public SetupMFAAction setUser(final User user) {
			this.user = user;
			return this;
		}

		public int getPass() {
			return this.pass;
		}

		public SetupMFAAction setPass(final int pass) {
			this.pass = pass;
			return this;
		}

	}

	/**
	 * Verifies the validity of an MFA code.
	 *
	 * Throws {@code InvalidKeyException} if the stored key is invalid.
	 * Throws {@code IllegalArgumentException} if the code failed to validate.
	 */
	public class VerifyCodeAction extends AbstractAction<Void> {
		private User user;
		private int pass;

		private VerifyCodeAction(final GMCServer srv) {
			super(srv);
		}

		@Override
		protected void executeSync(final Promise<Void> promise) {
			try {
				if (this.srv.getMfaManager().passwordMatches(this.user, this.pass)) {
					promise.complete();
				}
			} catch (final InvalidKeyException e) {
				promise.fail("Invalid MFA key");
			} catch (final IllegalArgumentException e) {
				promise.fail("User does not have MFA set");
			}
		}

		public User getUser() {
			return this.user;
		}

		public VerifyCodeAction setUser(final User user) {
			this.user = user;
			return this;
		}

		public int getPass() {
			return this.pass;
		}

		public VerifyCodeAction setPass(final int pass) {
			this.pass = pass;
			return this;
		}
	}

	public class DisableMFAAction extends AbstractAction<Void> {
		private User user;
		private int code;

		public DisableMFAAction(final GMCServer srv) {
			super(srv);
		}

		@Override
		protected void executeSync(final Promise<Void> promise) {
			MFAManager.this.verifyCode().setPass(this.code).setUser(this.user).execute().onSuccess(v -> {
				this.srv.getDatabaseManager()
					.getCollection(User.class)
					.updateOne(Filters.eq(this.user.getId()),
							Updates.combine(Updates.unset("mfaKey"), Updates.set("mfa", false)));
				promise.complete();
			}).onFailure(promise::fail);
		}

		public User getUser() {
			return this.user;
		}

		public DisableMFAAction setUser(final User user) {
			this.user = user;
			return this;
		}

		public int getCode() {
			return this.code;
		}

		public DisableMFAAction setCode(final int code) {
			this.code = code;
			return this;
		}
	}
}
