package me.vinceh121.gmcserver.handlers;

import io.vertx.core.Handler;
import io.vertx.ext.web.RoutingContext;
import xyz.bowser65.tokenize.Token;

public class StrictAuthHandler implements Handler<RoutingContext> {

	@Override
	public void handle(final RoutingContext ctx) {
		final Token token = ctx.get(AuthHandler.TOKEN_KEY);

		if (token == null) {
			ctx.response().setStatusCode(403).end();
		} else if ("mfa".equals(token.getPrefix())) {
			ctx.response().setStatusCode(403).end("MFA auth not complete");
			return;
		} else {
			ctx.next();
		}
	}

}
