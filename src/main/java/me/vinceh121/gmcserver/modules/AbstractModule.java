package me.vinceh121.gmcserver.modules;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.vertx.core.Handler;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Route;
import io.vertx.ext.web.RoutingContext;
import me.vinceh121.gmcserver.GMCServer;

public abstract class AbstractModule {
	protected final GMCServer srv;
	protected final Logger log;

	public AbstractModule(final GMCServer srv) {
		this.srv = srv;
		this.log = LoggerFactory.getLogger(this.getClass());
	}

	protected Route registerStrictAuthedRoute(final HttpMethod method, final String path,
			final Handler<RoutingContext> handler) {
		return this.registerAuthedRoute(method, path, this.srv.getStrictAuthHandler()).handler(handler);
	}

	protected Route registerAuthedRoute(final HttpMethod method, final String path,
			final Handler<RoutingContext> handler) {
		return this.registerRoute(method, path, this.srv.getAuthHandler()).handler(handler);
	}

	protected Route registerRoute(final HttpMethod method, final String path, final Handler<RoutingContext> handler) {
		return this.srv.getRouter()
				.route(method, path)
				.handler(this.srv.getApiHandler())
				.handler(srv.getBodyHandler())
				.handler(handler)
				.enable();
	}

	protected void error(final RoutingContext ctx, final int status, final String desc) {
		this.error(ctx, status, desc, null);
	}

	protected void error(final RoutingContext ctx, final int status, final String desc, final JsonObject extra) {
		final JsonObject obj = new JsonObject();
		obj.put("status", status);
		obj.put("description", desc);
		obj.put("extras", extra);
		ctx.response().setStatusCode(status).end(obj.encode());
	}
}
