package me.vinceh121.gmcserver.proxy;

import java.util.Map;

import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;
import me.vinceh121.gmcserver.GMCServer;
import me.vinceh121.gmcserver.entities.Record;

public abstract class AbstractProxy {
	protected final GMCServer srv;

	public AbstractProxy(final GMCServer srv) {
		this.srv = srv;
	}
	
	public abstract Future<Void> validateSettings(final JsonObject obj);

	public abstract Future<Void> proxyRecord(final Record r, final Map<String, Object> proxySettings);
}
