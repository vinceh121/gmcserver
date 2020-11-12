package me.vinceh121.gmcserver.handlers;

import io.vertx.core.Handler;
import io.vertx.ext.web.RoutingContext;

public class APIHandler implements Handler<RoutingContext> {

	@Override
	public void handle(final RoutingContext ctx) {
		ctx.response().putHeader("Content-Type", "application/json");
		ctx.next();
	}

}
