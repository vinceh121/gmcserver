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
package me.vinceh121.gmcserver.auth;

import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Updates;
import io.vertx.core.Future;
import me.vinceh121.gmcserver.GMCServer;
import me.vinceh121.gmcserver.entities.User;
import me.vinceh121.gmcserver.exceptions.AuthenticationException;
import me.vinceh121.gmcserver.exceptions.EntityNotFoundException;
import me.vinceh121.gmcserver.managers.UserManager.CreateUserAction;
import me.vinceh121.gmcserver.managers.email.Email;

import java.util.UUID;

public class InternalAuthenticator extends AbstractAuthenticator {

	public InternalAuthenticator(final GMCServer srv) {
		super(srv);
	}

	/**
	 * @throws EntityNotFoundException if the user what not found
	 * @throws IllegalStateException   if the user's account is disabled
	 * @throws AuthenticationException if the password failed to validate
	 */
	@Override
	public Future<User> login(final String username, final String password) {
		return Future.future(promise -> {
			final User user = this.srv.getDatabaseManager()
					.getCollection(User.class)
					.find(Filters.or(Filters.eq("username", username), Filters.eq("email", username)))
					.first();

			if (user == null) {
				promise.fail(new EntityNotFoundException("User not found"));
				return;
			}

			if (user.getPassword() == null) {
				promise.fail(new IllegalStateException("User account disabled"));
				return;
			}

			if (!this.srv.getArgon().verify(user.getPassword(), password.toCharArray())) {
				promise.fail(new AuthenticationException("Invalid password"));
				return;
			}
			promise.complete(user);
		});
	}

	/**
	 * @throws See {@link CreateUserAction}
	 */
	@Override
	public Future<User> register(final String username, final String email, final String password) {
		return Future.future(promise -> {
			final CreateUserAction action = this.srv.getUserManager()
					.createUser()
					.setUsername(username)
					.setPassword(password)
					.setEmail(email);
			action.execute().onSuccess(promise::complete).onFailure(promise::fail);
		});
	}

	public Future<Void> sendPasswordResetLink(final User user) {
		return Future.future(promise -> {
			if (user == null) {
				promise.fail(new EntityNotFoundException("User not found"));
				return;
			}

			final UUID id = UUID.randomUUID();

			this.srv.getDatabaseManager().getCollection(User.class)
					.updateOne(Filters.eq(user.getId()),
							Updates.set("passwordResetToken", id.toString())
					);

			final Email email = new Email();
			email.setTo(user);
			email.setTemplate("password-reset");
			email.setSubject("Password reset");
			email.getContext().put("token", id.toString());

			this.srv.getEmailManager().sendEmail(email)
					.onSuccess(promise::complete)
					.onFailure(promise::fail);
		});
	}
}
