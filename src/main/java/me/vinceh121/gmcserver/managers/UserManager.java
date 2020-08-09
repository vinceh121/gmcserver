package me.vinceh121.gmcserver.managers;

import java.security.SecureRandom;
import java.security.SignatureException;

import org.bson.types.ObjectId;

import com.mongodb.client.model.Filters;

import io.vertx.core.Promise;
import me.vinceh121.gmcserver.DatabaseManager;
import me.vinceh121.gmcserver.GMCServer;
import me.vinceh121.gmcserver.actions.AbstractAction;
import me.vinceh121.gmcserver.entities.User;
import xyz.bowser65.tokenize.IAccount;
import xyz.bowser65.tokenize.Token;

public class UserManager extends AbstractManager {
	private static final SecureRandom USER_RANDOM = new SecureRandom();

	public UserManager(final GMCServer srv) {
		super(srv);
	}

	public VerifyTokenAction verifyToken() {
		return new VerifyTokenAction(this.srv);
	}

	public UserLoginAction userLogin() {
		return new UserLoginAction(this.srv);
	}

	public CreateUserAction createUser() {
		return new CreateUserAction(this.srv);
	}

	public class VerifyTokenAction extends AbstractAction<Token> {
		private String tokenString;

		private VerifyTokenAction(GMCServer srv) {
			super(srv);
		}

		@Override
		protected void executeSync(Promise<Token> promise) {
			final Token token;
			try {
				token = this.srv.getTokenize().validateToken(tokenString, this::fetchAccount);
			} catch (final SignatureException e) {
				promise.fail("Couldn't validate token");
				return;
			}

			if (token == null) {
				promise.fail("Invalid token");
				return;
			}

//			if ("mfa".equals(token.getPrefix())) { // XXX need to think about that
//				promise.fail("MFA auth not complete");
//				return;
//			}
			
			promise.complete(token);
		}

		public String getTokenString() {
			return tokenString;
		}

		public VerifyTokenAction setTokenString(String tokenString) {
			this.tokenString = tokenString;
			return this;
		}

		private IAccount fetchAccount(final String id) {
			return this.srv.getManager(DatabaseManager.class)
					.getCollection(User.class)
					.find(Filters.eq(new ObjectId(id)))
					.first();
		}

	}

	public class UserLoginAction extends AbstractAction<Token> {
		private String username, password;

		public UserLoginAction(GMCServer srv) {
			super(srv);
		}

		@Override
		protected void executeSync(final Promise<Token> promise) {
			final User user = this.srv.getManager(DatabaseManager.class)
					.getCollection(User.class)
					.find(Filters.eq("username", username))
					.first();

			if (user == null) {
				promise.fail("User not found");
				return;
			}

			if (!this.srv.getArgon().verify(user.getPassword(), password.toCharArray())) {
				promise.fail("Could not verify auth");
				return;
			}

			final Token token;
			if (user.isMfa()) {
				token = this.srv.getTokenize().generateToken(user, "mfa");
			} else {
				token = this.srv.getTokenize().generateToken(user);
			}
			promise.complete(token);
		}

		public String getUsername() {
			return username;
		}

		public UserLoginAction setUsername(String username) {
			this.username = username;
			return this;
		}

		public String getPassword() {
			return password;
		}

		public UserLoginAction setPassword(String password) {
			this.password = password;
			return this;
		}
	}

	public class CreateUserAction extends AbstractAction<User> {
		private String username, password;
		private boolean admin, generateGmcId, insertInDb = true, checkUsernameAvailable = true;
		private long gmcId;

		private CreateUserAction(final GMCServer srv) {
			super(srv);
		}

		@Override
		protected void executeSync(final Promise<User> promise) {
			final User user = new User();
			user.setUsername(this.username);
			user.setPassword(this.srv.getArgon().hash(10, 65536, 1, password.toCharArray()));
			user.setAdmin(admin);

			if (generateGmcId) {
				user.setGmcId(Math.abs(USER_RANDOM.nextLong()));
			}
			user.setGmcId(this.gmcId);

			if (checkUsernameAvailable && this.srv.getManager(DatabaseManager.class)
					.getCollection(User.class)
					.find(Filters.eq("username", username))
					.first() != null) {
				promise.fail("Username taken");
				return;
			}

			promise.complete(user);

			if (insertInDb) {
				this.srv.getManager(DatabaseManager.class).getCollection(User.class).insertOne(user);
			}
		}

		public String getUsername() {
			return username;
		}

		public CreateUserAction setUsername(String username) {
			this.username = username;
			return this;
		}

		public String getPassword() {
			return password;
		}

		public CreateUserAction setPassword(String password) {
			this.password = password;
			return this;
		}

		public boolean isAdmin() {
			return admin;
		}

		public CreateUserAction setAdmin(boolean admin) {
			this.admin = admin;
			return this;
		}

		public boolean isGenerateGmcId() {
			return generateGmcId;
		}

		public CreateUserAction setGenerateGmcId(boolean generateGmcId) {
			this.generateGmcId = generateGmcId;
			return this;
		}

		public boolean isInsertInDb() {
			return insertInDb;
		}

		public CreateUserAction setInsertInDb(boolean insertInDb) {
			this.insertInDb = insertInDb;
			return this;
		}

		public long getGmcId() {
			return gmcId;
		}

		public CreateUserAction setGmcId(long gmcId) {
			this.gmcId = gmcId;
			return this;
		}

	}
}
