package me.vinceh121.gmcserver.modules;

import java.security.InvalidKeyException;
import java.security.SignatureException;

import org.bson.types.ObjectId;

import com.mongodb.client.model.Filters;

import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import me.vinceh121.gmcserver.DatabaseManager;
import me.vinceh121.gmcserver.GMCServer;
import me.vinceh121.gmcserver.entities.User;
import me.vinceh121.gmcserver.handlers.AuthHandler;
import me.vinceh121.gmcserver.mfa.MFAKey;
import me.vinceh121.gmcserver.mfa.MFAManager;
import xyz.bowser65.tokenize.IAccount;
import xyz.bowser65.tokenize.Token;

public class AuthModule extends AbstractModule {

	public AuthModule(final GMCServer srv) {
		super(srv);
		this.registerRoute(HttpMethod.POST, "/auth/register", this::handleRegister);
		this.registerRoute(HttpMethod.POST, "/auth/login", this::handleLogin);
		this.registerRoute(HttpMethod.POST, "/auth/mfa", this::handleSubmitMfa);
		this.registerStrictAuthedRoute(HttpMethod.PUT, "/auth/mfa", this::handleActivateMfa);
	}

	private void handleRegister(final RoutingContext ctx) {
		final JsonObject obj = ctx.getBodyAsJson();

		final String username = obj.getString("username");
		if (username == null) {
			this.error(ctx, 400, "Field username missing");
			return;
		}

		if (username.length() < 4 || username.length() > 128) {
			this.error(ctx, 400, "Username is invalid length");
			return;
		}

		final String password = obj.getString("password");

		if (password == null) {
			this.error(ctx, 400, "Field password missing");
			return;
		}

		if (password.length() < 4 || password.length() > 128) {
			this.error(ctx, 400, "Invalid password length");
			return;
		}

		if (this.srv.getManager(DatabaseManager.class)
				.getCollection(User.class)
				.find(Filters.eq("username", username))
				.first() != null) {
			this.error(ctx, 403, "Username already taken");
			return;
		}

		final User user = new User();
		user.setUsername(username);
		user.setPassword(this.srv.getArgon().hash(10, 65536, 1, password.toCharArray()));

		this.srv.getManager(DatabaseManager.class).getCollection(User.class).insertOne(user);
		ctx.response()
				.end(new JsonObject().put("username", user.getUsername())
						.put("id", user.getId().toHexString())
						.toBuffer());
	}

	private void handleLogin(final RoutingContext ctx) {
		final JsonObject obj = ctx.getBodyAsJson();

		final String username = obj.getString("username");

		if (username == null) {
			this.error(ctx, 400, "Field username missing");
			return;
		}

		if (!obj.containsKey("password")) {
			this.error(ctx, 400, "Field password missing");
			return;
		}

		final User user = this.srv.getManager(DatabaseManager.class)
				.getCollection(User.class)
				.find(Filters.eq("username", username))
				.first();

		if (user == null) {
			this.error(ctx, 404, "User not found");
			return;
		}

		if (!this.srv.getArgon().verify(user.getPassword(), obj.getString("password").toCharArray())) {
			this.error(ctx, 403, "Could not verify auth");
			return;
		}

		final Token token;
		if (user.isMfa()) {
			token = this.srv.getTokenize().generateToken(user, "mfa");
		} else {
			token = this.srv.getTokenize().generateToken(user);
		}

		ctx.response()
				// .setStatusCode(user.isMfa() ? 100 : 200)
				.end(new JsonObject().put("token", token.toString())
						.put("id", user.getId().toString())
						.put("mfa", user.isMfa())
						.toBuffer());
	}

	private void handleSubmitMfa(final RoutingContext ctx) {
		final JsonObject obj = ctx.getBodyAsJson();

		final String rawToken = ctx.request().getHeader("Authorization");

		if (rawToken == null) {
			this.error(ctx, 400, "Missing token");
			return;
		}

		final Token mfaToken;
		try {
			mfaToken = this.srv.getTokenize().validateToken(rawToken, this::fetchAccount);
		} catch (final SignatureException e) {
			this.error(ctx, 401, "Invalid token");
			return;
		}

		if (mfaToken == null) {
			this.error(ctx, 401, "Invalid token");
			return;
		}

		final User user = (User) mfaToken.getAccount();

		final boolean match;
		try {
			match = this.srv.getManager(MFAManager.class).passwordMatches(user, obj.getInteger("pass"));
		} catch (final InvalidKeyException e) {
			this.log.error("Invalid MFA key for user " + user.toString(), e);
			this.error(ctx, 500, "Invalid MFA key");
			return;
		} catch (final IllegalArgumentException e) {
			this.error(ctx, 400, "User does not have MFA set");
			return;
		}

		if (match) {
			final Token token = this.srv.getTokenize().generateToken(user);
			ctx.response()
					.end(new JsonObject().put("token", token.toString()).put("id", user.getId().toString()).toBuffer());
		} else {
			this.error(ctx, 401, "Invalid MFA password");
		}
	}

	private void handleActivateMfa(final RoutingContext ctx) {
		final User user = ctx.get(AuthHandler.USER_KEY);

		if (user.isMfa()) {
			this.error(ctx, 400, "MFA already setup");
			return;
		}

		if (user.getMfaKey() == null) { // MFA not setup at all
			final MFAKey key = this.srv.getManager(MFAManager.class).setupMFA(user);
			ctx.response().end(new JsonObject().put("keyUri", key.toURI("GMCServer " + user.getUsername())).toBuffer());
		} else { // Complete MFA setup
			final JsonObject obj = ctx.getBodyAsJson();
			boolean matches;
			try {
				matches = this.srv.getManager(MFAManager.class).completeMfaSetup(user, obj.getInteger("pass"));
			} catch (final InvalidKeyException e) {
				this.log.error("Invalid MFA key for user " + user.toString(), e);
				this.error(ctx, 500, "Invalid MFA key");
				return;
			}
			if (matches) {
				this.error(ctx, 200, "MFA now setup");
			} else {
				this.error(ctx, 401, "Invalid MFA password");
			}
		}
	}

	private IAccount fetchAccount(final String id) {
		return this.srv.getManager(DatabaseManager.class)
				.getCollection(User.class)
				.find(Filters.eq(new ObjectId(id)))
				.first();
	}
}
