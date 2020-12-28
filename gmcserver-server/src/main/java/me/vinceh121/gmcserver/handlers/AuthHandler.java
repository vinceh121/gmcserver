package me.vinceh121.gmcserver.handlers;

import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import me.vinceh121.gmcserver.GMCServer;
import me.vinceh121.gmcserver.entities.User;
import me.vinceh121.gmcserver.managers.UserManager.VerifyTokenAction;
import xyz.bowser65.tokenize.Token;

public class AuthHandler implements Handler<RoutingContext> {
	public static final String TOKEN_KEY = "me.vinceh121.gmcserver.token";
	public static final String USER_KEY = "me.vinceh121.gmcserver.user";
	private final GMCServer srv;

	public AuthHandler(final GMCServer srv) {
		this.srv = srv;
	}

	@Override
	public void handle(final RoutingContext ctx) {
		final String auth = ctx.request().getHeader("Authorization");
		if (auth == null) {
			ctx.next(); // non strict auth
			return;
		}

		final VerifyTokenAction action = this.srv.getUserManager().verifyToken().setTokenString(auth);

		action.execute().onComplete(res -> {

			if (res.failed()) {
				ctx.response()
						.setStatusCode(401)
						.end(new JsonObject().put("status", 401).put("msg", res.cause().getMessage()).toBuffer());
				return;
			}

			final Token token = res.result();
			if (token != null) {
				final User user = (User) token.getAccount();
				ctx.put(AuthHandler.TOKEN_KEY, token);
				ctx.put(AuthHandler.USER_KEY, user);
			}
			ctx.next();
		});

	}

}
