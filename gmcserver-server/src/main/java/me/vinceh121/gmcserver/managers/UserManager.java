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
package me.vinceh121.gmcserver.managers;

import java.security.SecureRandom;
import java.security.SignatureException;
import java.util.List;
import java.util.Vector;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.regex.Pattern;

import org.bson.conversions.Bson;
import org.bson.types.ObjectId;

import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Updates;

import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import me.vinceh121.gmcserver.GMCServer;
import me.vinceh121.gmcserver.actions.AbstractAction;
import me.vinceh121.gmcserver.entities.Device;
import me.vinceh121.gmcserver.entities.User;
import me.vinceh121.gmcserver.exceptions.AuthenticationException;
import me.vinceh121.gmcserver.exceptions.EntityNotFoundException;
import xyz.bowser65.tokenize.IAccount;
import xyz.bowser65.tokenize.Token;

public class UserManager extends AbstractManager {
	private static final SecureRandom USER_RANDOM = new SecureRandom();
	/**
	 * GQ GMC accept a maximum of 16 chars as User and Device IDs
	 */
	public static final long MAX_GMCID = 9999999999999999L;
	public static final Pattern USERNAME_REGEX = Pattern.compile("[a-zA-Z]{2,}[a-zA-Z0-9]{2,}"),
			EMAIL_REGEX = Pattern.compile("^[a-zA-Z0-9.!#$%&'*+/=?^_`{|}~-]" + "+@((\\[[0-9]{1,3}\\.[0-9]{1,3}\\"
					+ ".[0-9]{1,3}\\.[0-9]{1,3}])|(([a-" + "zA-Z\\-0-9]+\\.)+[a-zA-Z]{2,}))$");

	public UserManager(final GMCServer srv) {
		super(srv);
	}

	public GetUserAction getUser() {
		return new GetUserAction(this.srv);
	}

	public VerifyTokenAction verifyToken() {
		return new VerifyTokenAction(this.srv);
	}

	public GenerateTokenAction userLogin() {
		return new GenerateTokenAction(this.srv);
	}

	public CreateUserAction createUser() {
		return new CreateUserAction(this.srv);
	}

	public UpdateUserAction updateUser() {
		return new UpdateUserAction(this.srv);
	}

	public DeleteUserAction deleteUser() {
		return new DeleteUserAction(this.srv);
	}

	/**
	 * Fetches a user.
	 * 
	 * Throws {@code EntityNotFoundException} if the user was not found.
	 */
	public class GetUserAction extends AbstractAction<User> {
		private ObjectId id;

		public GetUserAction(final GMCServer srv) {
			super(srv);
		}

		@Override
		protected void executeSync(final Promise<User> promise) {
			final User user = this.srv.getDatabaseManager().getCollection(User.class).find(Filters.eq(this.id)).first();
			if (user != null) {
				promise.complete(user);
			} else {
				promise.fail(new EntityNotFoundException("Failed to get user"));
			}
		}

		public ObjectId getId() {
			return this.id;
		}

		public GetUserAction setId(final ObjectId id) {
			this.id = id;
			return this;
		}
	}

	/**
	 * Checks the validity of a token.
	 *
	 * Throws {@code IllegalArgumentException} if the token was not specified.
	 * Throws {@code SignatureException} if the token couldn't be validated. Throws
	 * {@code IllegalStateException} if the account doesn't exist or the token
	 * expired.
	 */
	public class VerifyTokenAction extends AbstractAction<Token> {
		private String tokenString;

		private VerifyTokenAction(final GMCServer srv) {
			super(srv);
		}

		@Override
		protected void executeSync(final Promise<Token> promise) {
			if (this.tokenString == null) {
				promise.fail(new IllegalArgumentException("Token not specified"));
				return;
			}

			final Token token;
			try {
				token = this.srv.getTokenize().validateToken(this.tokenString, this::fetchAccount);
			} catch (final SignatureException e) {
				promise.fail(new SignatureException("Couldn't validate token", e));
				return;
			}

			if (token == null) {
				promise.fail(new IllegalStateException("Invalid token"));
				return;
			}

			promise.complete(token);
		}

		public String getTokenString() {
			return this.tokenString;
		}

		public VerifyTokenAction setTokenString(final String tokenString) {
			this.tokenString = tokenString;
			return this;
		}

		private IAccount fetchAccount(final String id) {
			return this.srv.getDatabaseManager().getCollection(User.class).find(Filters.eq(new ObjectId(id))).first();
		}

	}

	/**
	 * Generates a token.
	 */
	public class GenerateTokenAction extends AbstractAction<Token> {
		private User user;
		private boolean mfaPass;

		public GenerateTokenAction(final GMCServer srv) {
			super(srv);
		}

		@Override
		protected void executeSync(final Promise<Token> promise) {
			final Token token;
			if (this.user.isMfa() && !this.mfaPass) {
				token = this.srv.getTokenize().generateToken(this.user, "mfa");
			} else {
				token = this.srv.getTokenize().generateToken(this.user);
			}
			promise.complete(token);
		}

		public User getUser() {
			return this.user;
		}

		public GenerateTokenAction setUser(final User user) {
			this.user = user;
			return this;
		}

		public boolean isMfaPass() {
			return this.mfaPass;
		}

		public GenerateTokenAction setMfaPass(final boolean mfaPass) {
			this.mfaPass = mfaPass;
			return this;
		}
	}

	/**
	 * Creates a user.
	 *
	 * Throws {@code IllegalStateException} if the username or email are already
	 * taken.
	 */
	public class CreateUserAction extends AbstractAction<User> {
		private String username, password, email;
		private boolean admin, generateGmcId = true, insertInDb = true;
		private final boolean checkUsernameAvailable = true, checkEmailAvailable = true;
		private long gmcId;

		private CreateUserAction(final GMCServer srv) {
			super(srv);
		}

		@Override
		protected void executeSync(final Promise<User> promise) {
			final User user = new User();
			user.setUsername(this.username);
			user.setEmail(this.email);
			user.setPassword(
					this.password == null ? null : this.srv.getArgon().hash(10, 65536, 1, this.password.toCharArray()));
			user.setAdmin(this.admin);

			if (this.generateGmcId) {
				user.setGmcId(Math.abs(UserManager.USER_RANDOM.nextLong()) % MAX_GMCID);
			} else if (this.gmcId != 0) {
				user.setGmcId(this.gmcId);
			}

			if (this.checkUsernameAvailable && this.srv.getDatabaseManager()
				.getCollection(User.class)
				.find(Filters.eq("username", this.username))
				.first() != null) {
				promise.fail(new IllegalStateException("Username taken"));
				return;
			}

			if (this.checkEmailAvailable && this.srv.getDatabaseManager()
				.getCollection(User.class)
				.find(Filters.eq("email", this.email))
				.first() != null) {
				promise.fail(new IllegalStateException("Email taken"));
				return;
			}

			promise.complete(user);

			if (this.insertInDb) {
				this.srv.getDatabaseManager().getCollection(User.class).insertOne(user);
			}
		}

		public String getUsername() {
			return this.username;
		}

		public CreateUserAction setUsername(final String username) {
			this.username = username;
			return this;
		}

		public String getPassword() {
			return this.password;
		}

		public CreateUserAction setPassword(final String password) {
			this.password = password;
			return this;
		}

		public boolean isAdmin() {
			return this.admin;
		}

		public CreateUserAction setAdmin(final boolean admin) {
			this.admin = admin;
			return this;
		}

		public boolean isGenerateGmcId() {
			return this.generateGmcId;
		}

		public CreateUserAction setGenerateGmcId(final boolean generateGmcId) {
			this.generateGmcId = generateGmcId;
			return this;
		}

		public boolean isInsertInDb() {
			return this.insertInDb;
		}

		public CreateUserAction setInsertInDb(final boolean insertInDb) {
			this.insertInDb = insertInDb;
			return this;
		}

		public long getGmcId() {
			return this.gmcId;
		}

		public CreateUserAction setGmcId(final long gmcId) {
			this.gmcId = gmcId;
			return this;
		}

		public String getEmail() {
			return this.email;
		}

		public CreateUserAction setEmail(final String email) {
			this.email = email;
			return this;
		}
	}

	/**
	 * Updates a user.
	 *
	 * Requires password confirmation to change password.
	 *
	 * Throws {@code IllegalStateException} if the username or email is already
	 * taken. Throws {@code AuthenticationException} if the user password
	 * confirmation couldn't be validated.
	 */
	public class UpdateUserAction extends AbstractAction<Void> {
		private User user;
		private String username, email, currentPassword, newPassword;
		private Boolean alertEmails;

		public UpdateUserAction(final GMCServer srv) {
			super(srv);
		}

		@Override
		protected void executeSync(final Promise<Void> promise) {
			final List<Bson> updates = new Vector<>();

			if (this.username != null) {
				if (this.srv.getDatabaseManager()
					.getCollection(User.class)
					.find(Filters.eq("username", this.username))
					.first() != null) {
					promise.fail(new IllegalStateException("Username is taken"));
					return;
				}

				updates.add(Updates.set("username", this.username));
			}

			if (this.email != null) {
				if (this.srv.getDatabaseManager()
					.getCollection(User.class)
					.find(Filters.eq("email", this.email))
					.first() != null) {
					promise.fail(new IllegalStateException("Email is taken"));
					return;
				}

				updates.add(Updates.set("email", this.email));
			}

			if (this.newPassword != null && this.currentPassword != null) {
				try {
					final CompletableFuture<User> fut = this.srv.getAuthenticator()
						.login(this.user.getUsername(), this.currentPassword)
						.toCompletionStage() // FIXME this should be proper future handling
						.toCompletableFuture();

					if (fut.get() == null && fut.isCompletedExceptionally()) {
						promise.fail(new AuthenticationException("Failed to authenticate"));
						return;
					}

					updates.add(Updates.set("password",
							this.srv.getArgon().hash(10, 65536, 1, this.newPassword.toCharArray())));
				} catch (final InterruptedException | ExecutionException e) {
					promise.fail(e);
					return;
				}
			}

			if (this.alertEmails != null) {
				updates.add(Updates.set("alertEmails", this.alertEmails));
			}

			this.srv.getDatabaseManager()
				.getCollection(User.class)
				.updateOne(Filters.eq(this.user.getId()), Updates.combine(updates));
			promise.complete();
		}

		public User getUser() {
			return this.user;
		}

		public UpdateUserAction setUser(final User user) {
			this.user = user;
			return this;
		}

		public String getUsername() {
			return this.username;
		}

		public UpdateUserAction setUsername(final String username) {
			this.username = username;
			return this;
		}

		public String getEmail() {
			return this.email;
		}

		public UpdateUserAction setEmail(final String email) {
			this.email = email;
			return this;
		}

		public String getCurrentPassword() {
			return this.currentPassword;
		}

		public UpdateUserAction setCurrentPassword(final String currentPassword) {
			this.currentPassword = currentPassword;
			return this;
		}

		public String getNewPassword() {
			return this.newPassword;
		}

		public UpdateUserAction setNewPassword(final String newPassword) {
			this.newPassword = newPassword;
			return this;
		}

		public Boolean getAlertEmails() {
			return alertEmails;
		}

		public UpdateUserAction setAlertEmails(Boolean alertEmails) {
			this.alertEmails = alertEmails;
			return this;
		}
	}

	/**
	 * Deletes a user. This will delete all of the users's devices and their records
	 * and calendars.
	 * 
	 * Throws {@code AuthenticationException} if the password confirmation failed.
	 */
	public class DeleteUserAction extends AbstractAction<Void> {
		private String confirmPassword;
		private User user;

		public DeleteUserAction(final GMCServer srv) {
			super(srv);
		}

		@Override
		protected void executeSync(final Promise<Void> promise) {
			try {
				final CompletableFuture<User> fut = this.srv.getAuthenticator()
					.login(this.user.getUsername(), this.confirmPassword)
					.toCompletionStage() // FIXME this should be proper future handling
					.toCompletableFuture();

				if (fut.get() == null && fut.isCompletedExceptionally()) {
					promise.fail(new AuthenticationException("Failed to authenticate"));
					return;
				}

			} catch (final InterruptedException | ExecutionException e) {
				promise.fail(e);
				return;
			}

			// If reauth is successful:

			@SuppressWarnings("rawtypes")
			final List<Future> deletes = new Vector<>();

			for (final Device dev : this.srv.getDatabaseManager()
				.getCollection(Device.class)
				.find(Filters.eq("owner", this.user.getId()))) {
				deletes
					.add(this.srv.getDeviceManager().deleteDevice().setDeviceId(dev.getId()).setDelete(true).execute());
			}

			CompositeFuture.all(deletes).onSuccess(fut -> {
				this.srv.getDatabaseManager().getCollection(User.class).deleteOne(Filters.eq(this.user.getId()));
				promise.complete();
			}).onFailure(promise::fail);
		}

		public String getConfirmPassword() {
			return this.confirmPassword;
		}

		public DeleteUserAction setConfirmPassword(final String confirmPassword) {
			this.confirmPassword = confirmPassword;
			return this;
		}

		public User getUser() {
			return this.user;
		}

		public DeleteUserAction setUser(final User user) {
			this.user = user;
			return this;
		}
	}
}
