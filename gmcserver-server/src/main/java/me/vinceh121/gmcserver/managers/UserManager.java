package me.vinceh121.gmcserver.managers;

import java.security.SecureRandom;
import java.security.SignatureException;
import java.util.regex.Pattern;

import org.bson.types.ObjectId;

import com.mongodb.client.model.Filters;

import io.vertx.core.Promise;
import me.vinceh121.gmcserver.GMCServer;
import me.vinceh121.gmcserver.actions.AbstractAction;
import me.vinceh121.gmcserver.entities.User;
import xyz.bowser65.tokenize.IAccount;
import xyz.bowser65.tokenize.Token;

public class UserManager extends AbstractManager {
	private static final SecureRandom USER_RANDOM = new SecureRandom();
	public static final Pattern USERNAME_REGEX = Pattern.compile("[a-zA-Z]{2,}[a-zA-Z0-9]{2,}"),
			EMAIL_REGEX = Pattern.compile("^[a-zA-Z0-9.!#$%&'*+/=?^_`{|}~-]"
					+ "+@((\\[[0-9]{1,3}\\.[0-9]{1,3}\\"
					+ ".[0-9]{1,3}\\.[0-9]{1,3}])|(([a-"
					+ "zA-Z\\-0-9]+\\.)+[a-zA-Z]{2,}))$");

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
				promise.fail("Failed to get user");
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

	public class VerifyTokenAction extends AbstractAction<Token> {
		private String tokenString;

		private VerifyTokenAction(final GMCServer srv) {
			super(srv);
		}

		@Override
		protected void executeSync(final Promise<Token> promise) {
			if (this.tokenString == null) {
				promise.fail("Token not specified");
				return;
			}

			final Token token;
			try {
				token = this.srv.getTokenize().validateToken(this.tokenString, this::fetchAccount);
			} catch (final SignatureException e) {
				promise.fail("Couldn't validate token");
				return;
			}

			if (token == null) {
				promise.fail("Invalid token");
				return;
			}

			// if ("mfa".equals(token.getPrefix())) { // XXX need to think about that
			// promise.fail("MFA auth not complete");
			// return;
			// }

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

	public class GenerateTokenAction extends AbstractAction<Token> {
		private User user;

		public GenerateTokenAction(final GMCServer srv) {
			super(srv);
		}

		@Override
		protected void executeSync(final Promise<Token> promise) {
			final Token token;
			if (this.user.isMfa()) {
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
	}

	public class CreateUserAction extends AbstractAction<User> {
		private String username, password, email;
		private boolean admin, generateGmcId, insertInDb = true;
		private final boolean checkUsernameAvailable = true;
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
				user.setGmcId(Math.abs(UserManager.USER_RANDOM.nextLong()));
			}
			user.setGmcId(this.gmcId);

			if (this.checkUsernameAvailable && this.srv.getDatabaseManager()
					.getCollection(User.class)
					.find(Filters.eq("username", this.username))
					.first() != null) {
				promise.fail("Username taken");
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
}
