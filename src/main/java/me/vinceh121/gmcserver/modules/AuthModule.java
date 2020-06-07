package me.vinceh121.gmcserver.modules;

import com.mongodb.client.model.Filters;

import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import me.vinceh121.gmcserver.GMCServer;
import me.vinceh121.gmcserver.entities.User;
import xyz.bowser65.tokenize.Token;

public class AuthModule extends AbstractModule {

	public AuthModule(final GMCServer srv) {
		super(srv);
		this.registerRoute(HttpMethod.POST, "/auth/register", this::handleRegister);
		this.registerRoute(HttpMethod.POST, "/auth/login", this::handleLogin);
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

		if (srv.getColUsers().find(Filters.eq("username", username)).first() != null) {
			this.error(ctx, 403, "Username already taken");
			return;
		}

		final User user = new User();
		user.setUsername(username);
		user.setPassword(srv.getArgon().hash(10, 65536, 1, password.toCharArray()));

		srv.getColUsers().insertOne(user);
		ctx.response()
				.end(new JsonObject().put("username", user.getUsername())
						.put("id", user.getId().toHexString())
						.encode());
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

		final User user = this.srv.getColUsers().find(Filters.eq("username", username)).first();

		if (user == null) {
			this.error(ctx, 404, "User not found");
			return;
		}

		if (!this.srv.getArgon().verify(user.getPassword(), obj.getString("password").toCharArray())) {
			this.error(ctx, 403, "Could not verify auth");
			return;
		}

		final Token token = this.srv.getTokenize().generateToken(user);

		ctx.response()
				.end(new JsonObject().put("token", token.toString()).put("id", user.getId().toHexString()).encode());
	}
}
