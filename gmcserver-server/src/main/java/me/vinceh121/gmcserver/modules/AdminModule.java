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
package me.vinceh121.gmcserver.modules;

import org.bson.types.ObjectId;

import io.vertx.core.http.HttpMethod;
import io.vertx.ext.web.RoutingContext;
import me.vinceh121.gmcserver.GMCServer;
import me.vinceh121.gmcserver.entities.User;
import me.vinceh121.gmcserver.handlers.AuthHandler;
import me.vinceh121.gmcserver.managers.email.Email;

public class AdminModule extends AbstractModule {

	public AdminModule(final GMCServer srv) {
		super(srv);
		this.registerStrictAuthedRoute(HttpMethod.GET, "/admin/testmail/:to/:template", this::handleTestEmail);
	}

	private void handleTestEmail(final RoutingContext ctx) {
		final User self = ctx.get(AuthHandler.USER_KEY);
		if (!self.isAdmin()) {
			this.error(ctx, 403, "You're not an admin");
			return;
		}

		final String to = ctx.pathParam("to");
		if (to == null) {
			this.error(ctx, 400, "Missing :to");
			return;
		}

		final String template = ctx.pathParam("template");
		if (template == null) {
			this.error(ctx, 400, "Missing :template");
			return;
		}

		final Email email = new Email();
		this.srv.getUserManager().getUser().setId(new ObjectId(to)).execute().onSuccess(user -> {
			email.setTo(user);
			email.setTemplate(template);
			email.setSubject("GMCServer test email");
			this.srv.getEmailManager().sendEmail(email).onSuccess(v -> {
				this.error(ctx, 200, "Email sent to " + user);
				this.log.info("Sent test email to {}", user);
			}).onFailure(t -> {
				this.error(ctx, 500, "Failed to send email: " + t);
				this.log.error("Failed to send test email", t);
			});
		}).onFailure(t -> {
			this.error(ctx, 404, "User not found");
		});
	}
}
