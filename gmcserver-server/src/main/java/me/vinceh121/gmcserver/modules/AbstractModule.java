/*
 * GMCServer, lightweight service to log, analyze and proxy Geiger counter data.
 * Copyright (C) 2020 Vincent Hyvert
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public for more details.
 *
 * You should have received a copy of the GNU Affero General Public
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package me.vinceh121.gmcserver.modules;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import io.vertx.core.Handler;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Route;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import me.vinceh121.gmcserver.GMCServer;

public abstract class AbstractModule {
	protected final GMCServer srv;
	protected final Logger log;

	public AbstractModule(final GMCServer srv) {
		this.srv = srv;
		this.log = LogManager.getLogger(this.getClass());
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
		return this.registerRoute(this.srv.getApiRouter(), method, path, handler);
	}

	protected Route registerRoute(final Router router, final HttpMethod method, final String path,
			final Handler<RoutingContext> handler) {
		return router.route(method, path)
			.handler(this.srv.getApiHandler())
			.handler(this.srv.getBodyHandler())
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
		ctx.response().setStatusCode(status).end(obj.toBuffer());
	}
}
