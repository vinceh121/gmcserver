package me.vinceh121.gmcserver.modules;

import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import me.vinceh121.gmcserver.GMCServer;

public class InstanceModule extends AbstractModule {

	public InstanceModule(final GMCServer srv) {
		super(srv);
		this.registerRoute(HttpMethod.GET, "/instance/info", this::handleInstanceInfo);
	}

	private void handleInstanceInfo(final RoutingContext ctx) {
		ctx.response().end(JsonObject.mapFrom(this.srv.getInstanceInfo()).toBuffer());
	}

}
