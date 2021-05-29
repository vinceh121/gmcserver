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
