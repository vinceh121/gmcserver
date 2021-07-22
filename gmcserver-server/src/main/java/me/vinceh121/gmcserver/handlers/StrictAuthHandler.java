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
		} else {
			ctx.next();
		}
	}

}
