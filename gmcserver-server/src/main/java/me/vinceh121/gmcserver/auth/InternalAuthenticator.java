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
package me.vinceh121.gmcserver.auth;

import com.mongodb.client.model.Filters;

import io.vertx.core.Future;
import me.vinceh121.gmcserver.GMCServer;
import me.vinceh121.gmcserver.entities.User;
import me.vinceh121.gmcserver.managers.UserManager.CreateUserAction;

public class InternalAuthenticator extends AbstractAuthenticator {

	public InternalAuthenticator(final GMCServer srv) {
		super(srv);
	}

	@Override
	public Future<User> login(final String username, final String password) {
		return Future.future(promise -> {
			final User user = this.srv.getDatabaseManager()
				.getCollection(User.class)
				.find(Filters.or(Filters.eq("username", username), Filters.eq("email", username)))
				.first();

			if (user == null) {
				promise.fail("User not found");
				return;
			}

			if (user.getPassword() == null) {
				promise.fail("User account disabled");
				return;
			}

			if (!this.srv.getArgon().verify(user.getPassword(), password.toCharArray())) {
				promise.fail("Invalid password");
				return;
			}
			promise.complete(user);
		});
	}

	@Override
	public Future<User> register(final String username, final String email, final String password) {
		return Future.future(promise -> {
			final CreateUserAction action = this.srv.getUserManager()
				.createUser()
				.setUsername(username)
				.setPassword(password)
				.setEmail(email);
			action.execute().onSuccess(promise::complete).onFailure(t -> promise.fail("Failed to create user: " + t));
		});
	}
}
