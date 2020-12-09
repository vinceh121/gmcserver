package me.vinceh121.gmcserver.auth;

import io.vertx.core.Future;
import me.vinceh121.gmcserver.GMCServer;
import me.vinceh121.gmcserver.entities.User;

public abstract class AbstractAuthenticator {
	protected final GMCServer srv;

	public AbstractAuthenticator(final GMCServer srv) {
		this.srv = srv;
	}

	public abstract Future<User> login(final String username, final String password);

	public abstract Future<User> register(final String username, final String email, final String password);
}
