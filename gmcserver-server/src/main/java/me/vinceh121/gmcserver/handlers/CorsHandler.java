/*
 * GMCServer, lightweight service to log, analyze and proxy Geiger counter data.
 * Copyright (C) 2020 Vincent Hyvert
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
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
