package me.vinceh121.gmcserver.modules;

import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import me.vinceh121.gmcserver.GMCServer;
import me.vinceh121.gmcserver.entities.User;
import me.vinceh121.gmcserver.handlers.AuthHandler;
import me.vinceh121.gmcserver.managers.UserManager;
import me.vinceh121.gmcserver.managers.UserManager.CreateUserAction;
import me.vinceh121.gmcserver.managers.UserManager.UserLoginAction;
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

		final CreateUserAction action
				= this.srv.getManager(UserManager.class).createUser().setUsername(username).setPassword(password);
		action.execute().onComplete(res -> {
			if (res.failed()) {
				this.error(ctx, 500, "Failed to create user: " + res.cause().getMessage());
				return;
			}

			final User user = res.result();
			ctx.response()
					.end(new JsonObject().put("username", user.getUsername())
							.put("id", user.getId().toHexString())
							.toBuffer());
		});

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

		final UserLoginAction action
				= this.srv.getManager(UserManager.class).userLogin().setUsername(username).setPassword(password);
		action.execute().onComplete(res -> {
			if (res.failed()) {
				this.error(ctx, 403, "Login failed: " + res.cause().getMessage());
			}

			final Token token = res.result();
			final User user = (User) token.getAccount();

			ctx.response()
					// .setStatusCode(user.isMfa() ? 100 : 200)
					.end(new JsonObject().put("token", token.toString())
							.put("id", user.getId().toString())
							.put("mfa", user.isMfa())
							.toBuffer());
		});
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
		action.execute().onComplete(res -> {
			if (res.succeeded()) {
				final Token token = this.srv.getTokenize().generateToken(user);
				ctx.response()
						.end(new JsonObject().put("token", token.toString())
								.put("id", user.getId().toString())
								.toBuffer());
			} else {
				this.error(ctx, 401, res.cause().getMessage());
			}
		});
	}

	private void handleActivateMfa(final RoutingContext ctx) {
		final User user = ctx.get(AuthHandler.USER_KEY);

		if (user.isMfa()) {
			this.error(ctx, 400, "MFA already setup");
			return;
		}

		final SetupMFAAction action = this.srv.getManager(MFAManager.class).setupMFA().setUser(user);

		if (user.getMfaKey() == null) {
			action.execute().onComplete(res -> {
				ctx.response().end(new JsonObject().put("mfaUri", res.result()).toBuffer());
			});
		} else {
			final Integer pass = ctx.getBodyAsJson().getInteger("pass");
			if (pass == null) {
				this.error(ctx, 400, "Missing pass");
				return;
			}
			action.setPass(pass).execute().onComplete(res -> {
				if (res.failed()) {
					this.error(ctx, 403, res.cause().getMessage());
					return;
				}
				ctx.response().end(new JsonObject().toBuffer());
			});
		}
	}
}
