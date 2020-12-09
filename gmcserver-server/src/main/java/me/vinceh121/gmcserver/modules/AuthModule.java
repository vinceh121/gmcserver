package me.vinceh121.gmcserver.modules;

import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import me.vinceh121.gmcserver.GMCServer;
import me.vinceh121.gmcserver.entities.User;
import me.vinceh121.gmcserver.handlers.AuthHandler;
import me.vinceh121.gmcserver.managers.UserManager;
import me.vinceh121.gmcserver.managers.UserManager.GenerateTokenAction;
import me.vinceh121.gmcserver.mfa.MFAManager;
import me.vinceh121.gmcserver.mfa.MFAManager.SetupMFAAction;
import me.vinceh121.gmcserver.mfa.MFAManager.VerifyCodeAction;
import xyz.bowser65.tokenize.Token;

public class AuthModule extends AbstractModule {

	public AuthModule(final GMCServer srv) {
		super(srv);
		this.registerRoute(HttpMethod.POST, "/auth/register", this::handleRegister);
		this.registerRoute(HttpMethod.POST, "/auth/login", this::handleLogin);
		this.registerAuthedRoute(HttpMethod.POST, "/auth/mfa", this::handleSubmitMfa);
		this.registerStrictAuthedRoute(HttpMethod.PUT, "/auth/mfa", this::handleActivateMfa);
	}

	private void handleRegister(final RoutingContext ctx) {
		final JsonObject obj = ctx.getBodyAsJson();

		final String username = obj.getString("username");
		if (username == null) {
			this.error(ctx, 400, "Field username missing");
			return;
		}

		if (username.length() < 4 || username.length() > 32) {
			this.error(ctx, 400, "Username is invalid length");
			return;
		}

		if (!UserManager.USERNAME_REGEX.matcher(username).matches()) {
			this.error(ctx, 400, "Invalid username");
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

		final String email = obj.getString("email");

		if (email == null) {
			this.error(ctx, 400, "Field email missing");
			return;
		}

		if (!UserManager.EMAIL_REGEX.matcher(email).matches()) {
			this.error(ctx, 400, "Invalid email");
			return;
		}

		this.srv.getAuthenticator().register(username, email, password).onSuccess(user -> {
			final GenerateTokenAction action = this.srv.getManager(UserManager.class).userLogin().setUser(user);
			action.execute().onSuccess(token -> {
				ctx.response()
						.end(new JsonObject().put("token", token.toString())
								.put("id", user.getId().toString())
								.toBuffer());
			}).onFailure(t -> this.error(ctx, 500, "Failed to login after creating account: " + t.getMessage()));
		}).onFailure(t -> this.error(ctx, 500, "Failed to create account: " + t.getMessage()));
	}

	private void handleLogin(final RoutingContext ctx) {
		final JsonObject obj = ctx.getBodyAsJson();

		final String username = obj.getString("username");
		final String password = obj.getString("password");

		if (username == null) {
			this.error(ctx, 400, "Field username missing");
			return;
		}

		if (password == null) {
			this.error(ctx, 400, "Field password missing");
			return;
		}

		this.srv.getAuthenticator().login(username, password).onSuccess(user -> {
			final GenerateTokenAction action = this.srv.getManager(UserManager.class).userLogin().setUser(user);
			action.execute().onSuccess(token -> {
				ctx.response()
						// .setStatusCode(user.isMfa() ? 100 : 200)
						.end(new JsonObject().put("token", token.toString())
								.put("id", user.getId().toString())
								.put("mfa", user.isMfa())
								.toBuffer());
			}).onFailure(t -> this.error(ctx, 500, "Login failed: " + t.getMessage()));
		}).onFailure(t -> this.error(ctx, 403, "Authentication failed: " + t.getMessage()));
	}

	private void handleSubmitMfa(final RoutingContext ctx) {
		final JsonObject obj = ctx.getBodyAsJson();

		final Integer pass = obj.getInteger("pass");
		if (pass == null) {
			this.error(ctx, 400, "Missing pass");
			return;
		}

		final Token mfaToken = ctx.get(AuthHandler.TOKEN_KEY);
		if (mfaToken == null) {
			this.error(ctx, 401, "Invalid token");
			return;
		}

		if (mfaToken.getPrefix().equals("mfa")) {
			this.error(ctx, 400, "Token is already valid");
			return;
		}

		final User user = (User) mfaToken.getAccount();

		final VerifyCodeAction action = this.srv.getManager(MFAManager.class).verifyCode().setPass(pass).setUser(user);
		action.execute().onSuccess(res -> {
			final GenerateTokenAction tokenAction = this.srv.getManager(UserManager.class).userLogin().setUser(user);
			tokenAction.execute().onSuccess(token -> {
				ctx.response()
						.end(new JsonObject().put("token", token.toString())
								.put("id", user.getId().toString())
								.toBuffer());
			}).onFailure(t -> this.error(ctx, 500, "Failed to login: " + t.getMessage()));
		}).onFailure(t -> this.error(ctx, 401, "Failed to verify code: " + t.getMessage()));
	}

	private void handleActivateMfa(final RoutingContext ctx) {
		final User user = ctx.get(AuthHandler.USER_KEY);

		if (user.isMfa()) {
			this.error(ctx, 400, "MFA already setup");
			return;
		}

		final SetupMFAAction action = this.srv.getManager(MFAManager.class).setupMFA().setUser(user);

		if (user.getMfaKey() == null) {
			action.execute().onSuccess(res -> {
				ctx.response().end(new JsonObject().put("mfaUri", res).toBuffer());
			}).onFailure(t -> this.error(ctx, 500, "Failed to setup MFA: " + t.getMessage()));
		} else {
			final Integer pass = ctx.getBodyAsJson().getInteger("pass");
			if (pass == null) {
				this.error(ctx, 400, "Missing pass");
				return;
			}
			action.setPass(pass)
					.execute()
					.onSuccess(res -> ctx.response().end(new JsonObject().toBuffer()))
					.onFailure(t -> this.error(ctx, 403, "Failed to confirm MFA setup: " + t.getMessage()));
		}
	}
}
