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
import java.util.Map;
import java.util.UUID;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import com.eatthepath.otp.TimeBasedOneTimePasswordGenerator;

import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonObject;
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

	public Future<Boolean> passwordMatches(final UUID id, final int pass) {
		return this.srv.getUserManager().getUser().setId(id).execute().compose(u -> this.passwordMatches(u, pass));
	}

	public Future<Boolean> passwordMatches(final User user, final int pass) {
		return Future.future(promise -> {
			if (!user.isMfa()) {
				throw new IllegalArgumentException("User does not have MFA set");
			}

			try {
				int actualPassword = this.generateOneTimePassword(user.getMfaKey(), Instant.now());
				promise.complete(actualPassword == pass);
			} catch (final InvalidKeyException e) {
				promise.fail(e);
			}
		});
	}

	public Future<MFAKey> setupMFA(final User user) {
		return Future.future(promise -> {
			final MFAKey key = this.generateKey();
			this.srv.getDatabaseManager()
				.update("UPDATE users SET mfakey = #{mfaKey} WHERE id = #{id}")
				.execute(Map.of("id", user.getId(), "mfaKey", JsonObject.mapFrom(key)))
				.onSuccess(r -> {
					user.setMfaKey(key);
					promise.complete(key);
				})
				.onFailure(promise::fail);
		});
	}

	public Future<Boolean> completeMfaSetup(final User user, final int pass) {
		return Future.future(promise -> {
			try {
				final int actualPassword = this.generateOneTimePassword(user.getMfaKey(), Instant.now());
				final boolean matches = actualPassword == pass;

				if (matches) {
					this.srv.getDatabaseManager()
						.update("UPDATE users SET mfa = #{mfa} WHERE id = #{id}")
						.execute(Map.of("id", user.getId(), "mfa", matches))
						.onSuccess(r -> {
							user.setMfa(true);
							promise.complete(matches);
						})
						.onFailure(promise::fail);
				}
			} catch (final InvalidKeyException e) {
				promise.fail(e);
			}
		});
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
	 * Throws {@code InvalidKeyException} if the stored key isn't valid. Throws
	 * {@code AuthenticationException} if the confirm code failed to validate.
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
				this.srv.getMfaManager().setupMFA(this.user).onSuccess(key -> {
					promise.complete(key.toURI("GMCServer " + this.user.getUsername()));
				}).onFailure(promise::fail);
			} else { // Complete MFA setup
				this.srv.getMfaManager().completeMfaSetup(this.user, this.pass).onSuccess(matches -> {
					if (matches) {
						promise.complete();
					} else {
						promise.fail(new AuthenticationException("Invalid pass"));
					}
				}).onFailure(promise::fail);
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
	 * Throws {@code InvalidKeyException} if the stored key is invalid. Throws
	 * {@code IllegalArgumentException} if the code failed to validate.
	 */
	public class VerifyCodeAction extends AbstractAction<Void> {
		private User user;
		private int pass;

		private VerifyCodeAction(final GMCServer srv) {
			super(srv);
		}

		@Override
		protected void executeSync(final Promise<Void> promise) {
			this.srv.getMfaManager().passwordMatches(this.user, this.pass).onSuccess(matches -> {
				if (matches) {
					promise.complete();
				} else {
					promise.fail("Invalid pass");
				}
			}).onFailure(promise::fail);
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
					.update("UPDATE users SET mfakey = NONE, mfa = false WHERE id = #{id}")
					.execute(Map.of("id", this.user.getId()))
					.onSuccess(r -> promise.complete())
					.onFailure(promise::fail);
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
