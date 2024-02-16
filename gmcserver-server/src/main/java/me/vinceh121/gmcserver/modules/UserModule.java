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

import java.util.Map;
import java.util.UUID;

import org.apache.logging.log4j.message.FormattedMessage;

import io.vertx.core.Future;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import me.vinceh121.gmcserver.GMCServer;
import me.vinceh121.gmcserver.entities.Device;
import me.vinceh121.gmcserver.entities.User;
import me.vinceh121.gmcserver.exceptions.AuthenticationException;
import me.vinceh121.gmcserver.handlers.AuthHandler;
import me.vinceh121.gmcserver.managers.UserManager;

public class UserModule extends AbstractModule {

	public UserModule(final GMCServer srv) {
		super(srv);
		this.registerAuthedRoute(HttpMethod.GET, "/user/me", this::handleMe);
		this.registerAuthedRoute(HttpMethod.GET, "/user/:id", this::handleUser);
		this.registerStrictAuthedRoute(HttpMethod.PUT, "/user/me", this::handleUpdateMe);
		this.registerStrictAuthedRoute(HttpMethod.DELETE, "/user/me", this::handleDeleteMe);
	}

	private void handleMe(final RoutingContext ctx) {
		final User user = ctx.get(AuthHandler.USER_KEY);
		if (user != null) {
			ctx.reroute(HttpMethod.GET, "/api/v1/user/" + user.getId().toString());
		} else {
			this.error(ctx, 404, "Not logged in");
		}
	}

	private void handleUser(final RoutingContext ctx) {
		final User authUser = ctx.get(AuthHandler.USER_KEY);

		final UUID requestedId;
		try {
			requestedId = UUID.fromString(ctx.pathParam("id"));
		} catch (final IllegalArgumentException e) {
			this.error(ctx, 400, "Invalid ID");
			return;
		}

		Future.succeededFuture().compose(v -> {
			final User user;
			if (authUser != null && requestedId.equals(authUser.getId())) {
				return Future.succeededFuture(authUser);
			} else {
				return this.srv.getUserManager().getUser().setId(requestedId).execute();
			}

		}).onSuccess(user -> {
			if (user == null) {
				this.error(ctx, 404, "User not found");
				return;
			}

			final JsonObject obj;
			if (authUser != null && requestedId.equals(authUser.getId())) {
				obj = user.toJson();
				if (user.getDeviceLimit() == -1) {
					obj.put("deviceLimit", Integer.parseInt(this.srv.getConfig().getProperty("device.user-limit")));
				}
			} else {
				obj = user.toPublicJson();
			}

			obj.put("self", authUser != null && requestedId.equals(authUser.getId()));

			this.srv.getDatabaseManager()
				.query("SELECT * FROM devices WHERE user = #{id}")
				.mapTo(Device.class)
				.execute(Map.of("id", user.getId()))
				.onSuccess(rowSet -> {
					final JsonArray devs = new JsonArray();
					rowSet.forEach(d -> devs.add(d.toPublicJson()));

					obj.put("devices", devs);

					ctx.response().end(obj.toBuffer());
				})
				.onFailure(t -> this.error(ctx, 500, t.toString()));
		}).onFailure(t -> this.error(ctx, 500, t.toString()));
	}

	private void handleUpdateMe(final RoutingContext ctx) {
		final User user = ctx.get(AuthHandler.USER_KEY);
		final JsonObject obj = ctx.body().asJsonObject();

		final String username = obj.getString("username");

		if (username != null && (username.length() < 4 || username.length() > 32)) {
			this.error(ctx, 400, "Username is invalid length");
			return;
		}

		if (username != null && !UserManager.USERNAME_REGEX.matcher(username).matches()) {
			this.error(ctx, 400, "Invalid username");
			return;
		}

		final String newPassword = obj.getString("newPassword");
		final String currentPassword = obj.getString("currentPassword");

		if (newPassword != null && (newPassword.length() < 4 || newPassword.length() > 128)) {
			this.error(ctx, 400, "Invalid password length");
			return;
		}

		if (newPassword != null && currentPassword == null) {
			this.error(ctx, 400, "Changing password requires current password to be entered");
			return;
		}

		final String email = obj.getString("email");

		if (email != null && !UserManager.EMAIL_REGEX.matcher(email).matches()) {
			this.error(ctx, 400, "Invalid email");
			return;
		}

		final Boolean alertEmails = obj.getBoolean("alertEmails");

		this.srv.getUserManager()
			.updateUser()
			.setUser(user)
			.setEmail(email)
			.setUsername(username)
			.setCurrentPassword(currentPassword)
			.setNewPassword(newPassword)
			.setAlertEmails(alertEmails)
			.execute()
			.onSuccess(v -> this.error(ctx, 200, "Successfully updated user"))
			.onFailure(t -> {
				if (t instanceof IllegalStateException) {
					this.error(ctx, 409, t.getMessage());
				} else if (t instanceof AuthenticationException) {
					this.error(ctx, 403, t.getMessage());
				} else {
					this.error(ctx, 500, "Failed to update user: " + t.getMessage());
				}
			});
	}

	private void handleDeleteMe(final RoutingContext ctx) {
		final User user = ctx.get(AuthHandler.USER_KEY);
		final JsonObject obj = ctx.body().asJsonObject();

		final String password = obj.getString("password");
		if (password == null) {
			this.error(ctx, 400, "Password missing");
			return;
		}

		this.srv.getUserManager()
			.deleteUser()
			.setConfirmPassword(password)
			.setUser(user)
			.execute()
			.onSuccess(v -> ctx.response().setStatusCode(204).end())
			.onFailure(t -> {
				if (t instanceof AuthenticationException) {
					this.error(ctx, 403, t.getMessage());
				} else {
					this.log.error(new FormattedMessage("Error while deleting account {}", user.getId()), t);
					this.error(ctx, 500, "Error while deleting your account: " + t);
				}
			});
	}
}
