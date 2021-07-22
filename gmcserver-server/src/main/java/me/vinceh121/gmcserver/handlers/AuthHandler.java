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
package me.vinceh121.gmcserver.handlers;

import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import me.vinceh121.gmcserver.GMCServer;
import me.vinceh121.gmcserver.entities.User;
import me.vinceh121.gmcserver.managers.UserManager.VerifyTokenAction;

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

		action.execute().onSuccess(token -> {
			if (token != null) {
				final User user = (User) token.getAccount();
				ctx.put(AuthHandler.TOKEN_KEY, token);
				ctx.put(AuthHandler.USER_KEY, user);
			}
			ctx.next();
		}).onFailure(t -> {
			ctx.response()
				.setStatusCode(401)
				.end(new JsonObject().put("status", 401).put("msg", t.getMessage()).toBuffer());
		});

	}

}
