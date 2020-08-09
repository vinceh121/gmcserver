package me.vinceh121.gmcserver.actions;

import io.vertx.core.Future;
import io.vertx.core.Promise;
import me.vinceh121.gmcserver.GMCServer;

public abstract class AbstractAction<T> {
	protected final GMCServer srv;

	public AbstractAction(GMCServer srv) {
		this.srv = srv;
	}

	public Future<T> execute() {
		return Future.future(p -> this.executeSync(p));
	}

	protected abstract void executeSync(final Promise<T> promise);
}
