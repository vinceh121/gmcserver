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
import me.vinceh121.gmcserver.DatabaseManager;
import me.vinceh121.gmcserver.GMCServer;
import me.vinceh121.gmcserver.actions.AbstractAction;
import me.vinceh121.gmcserver.entities.User;
import me.vinceh121.gmcserver.managers.AbstractManager;

public class MFAManager extends AbstractManager {
	private GMCServer srv;
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
				this.srv.getManager(DatabaseManager.class).getCollection(User.class).find(Filters.eq(id)).first(),
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
		this.srv.getManager(DatabaseManager.class)
				.getCollection(User.class)
				.updateOne(Filters.eq(user.getId()), Updates.set("mfaKey", key));
		user.setMfaKey(key);
		return key;
	}

	public boolean completeMfaSetup(final User user, final int pass) throws InvalidKeyException {
		final int actualPassword = this.generateOneTimePassword(user.getMfaKey(), Instant.now());
		final boolean matches = actualPassword == pass;
		if (matches) {
			this.srv.getManager(DatabaseManager.class)
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
		return this.generateOneTimePassword(this.mfaKeyToSecretKey(key), timestamp);
	}

	public int generateOneTimePassword(final Key key, final Instant timestamp) throws InvalidKeyException {
		return this.generator.generateOneTimePassword(key, timestamp);
	}

	public SecretKey mfaKeyToSecretKey(final MFAKey key) {
		final SecretKey secKey = new SecretKeySpec(key.getKey(), key.getAlgorithm());
		return secKey;
	}

	public SetupMFAAction setupMFA() {
		return new SetupMFAAction(srv);
	}

	public VerifyCodeAction verifyCode() {
		return new VerifyCodeAction(srv);
	}

	public class SetupMFAAction extends AbstractAction<String> {
		private User user;
		private int pass;

		public SetupMFAAction(GMCServer srv) {
			super(srv);
		}

		@Override
		protected void executeSync(final Promise<String> promise) {
			if (user.getMfaKey() == null) { // MFA not setup at all
				final MFAKey key = this.srv.getManager(MFAManager.class).setupMFA(user);
				promise.complete(key.toURI("GMCServer " + user.getUsername()));
			} else { // Complete MFA setup
				boolean matches;
				try {
					matches = this.srv.getManager(MFAManager.class).completeMfaSetup(user, pass);
				} catch (final InvalidKeyException e) {
					promise.fail("Invalid MFA key");
					return;
				}
				if (matches) {
					promise.complete();
				} else {
					promise.fail("Invalid pass");
				}
			}
		}

		public User getUser() {
			return user;
		}

		public SetupMFAAction setUser(User user) {
			this.user = user;
			return this;
		}

		public int getPass() {
			return pass;
		}

		public SetupMFAAction setPass(int pass) {
			this.pass = pass;
			return this;
		}

	}

	public class VerifyCodeAction extends AbstractAction<Void> {
		private User user;
		private int pass;

		private VerifyCodeAction(GMCServer srv) {
			super(srv);
		}

		@Override
		protected void executeSync(Promise<Void> promise) {
			try {
				if (this.srv.getManager(MFAManager.class).passwordMatches(user, pass)) {
					promise.complete();
				}
			} catch (final InvalidKeyException e) {
				promise.fail("Invalid MFA key");
			} catch (final IllegalArgumentException e) {
				promise.fail("User does not have MFA set");
			}
		}

		public User getUser() {
			return user;
		}

		public VerifyCodeAction setUser(User user) {
			this.user = user;
			return this;
		}

		public int getPass() {
			return pass;
		}

		public VerifyCodeAction setPass(int pass) {
			this.pass = pass;
			return this;
		}
	}
}
