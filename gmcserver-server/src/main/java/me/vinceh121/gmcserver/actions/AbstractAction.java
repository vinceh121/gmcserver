package me.vinceh121.gmcserver.actions;

import io.vertx.core.Future;
import io.vertx.core.Promise;
import me.vinceh121.gmcserver.GMCServer;

public abstract class AbstractAction<T> {
	protected final GMCServer srv;

	public AbstractAction(final GMCServer srv) {
		this.srv = srv;
	}

	public Future<T> execute() {
		return Future.future(this::executeSync);
	}

	protected abstract void executeSync(final Promise<T> promise);
}
