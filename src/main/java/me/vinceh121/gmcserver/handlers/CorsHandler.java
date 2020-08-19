package me.vinceh121.gmcserver.handlers;

import java.util.Arrays;
import java.util.Collection;

import io.vertx.core.Handler;
import io.vertx.core.http.HttpMethod;
import io.vertx.ext.web.RoutingContext;

public class CorsHandler implements Handler<RoutingContext> {
	private final Collection<String> corsHeaders = Arrays.asList("Content-Type, Authorization"),
			cordsMethods = Arrays.asList("POST", "GET", "OPTIONS", "PUT");
	private final String webHost;

	public CorsHandler(final String webHost) {
		this.webHost = webHost;
	}

	@Override
	public void handle(final RoutingContext ctx) {
		ctx.response().putHeader("Access-Control-Allow-Origin", this.webHost);
		ctx.response().putHeader("Access-Control-Allow-Methods", String.join(", ", this.cordsMethods));
		ctx.response().putHeader("Access-Control-Allow-Headers", String.join(", ", this.corsHeaders));

		if (ctx.request().method().equals(HttpMethod.OPTIONS)) {
			ctx.response().setStatusCode(204).end();
		} else {
			ctx.next();
		}
	}

}
