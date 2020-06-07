package me.vinceh121.gmcserver.modules;

import org.bson.types.ObjectId;

import com.mongodb.client.FindIterable;
import com.mongodb.client.model.Filters;

import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import me.vinceh121.gmcserver.GMCServer;
import me.vinceh121.gmcserver.entities.Device;
import me.vinceh121.gmcserver.entities.Record;
import me.vinceh121.gmcserver.entities.User;
import me.vinceh121.gmcserver.handlers.AuthHandler;

public class DeviceModule extends AbstractModule {

	public DeviceModule(final GMCServer srv) {
		super(srv);
		this.registerAuthedRoute(HttpMethod.GET, "/device/:deviceId/timeline", this::handleDeviceHistory);
	}

	private void handleDeviceHistory(final RoutingContext ctx) {
		final String rawDeviceId = ctx.pathParam("deviceId");

		final ObjectId deviceId;
		try {
			deviceId = new ObjectId(rawDeviceId);
		} catch (final IllegalArgumentException e) {
			this.error(ctx, 400, "Invalid device ID");
			return;
		}

		final Device dev = this.srv.getColDevices().find(Filters.eq(deviceId)).first();

		if (dev == null) {
			this.error(ctx, 404, "Device not found");
			return;
		}

		final JsonObject obj = new JsonObject();
		final JsonArray arr = new JsonArray();
		obj.put("records", arr);

		final FindIterable<Record> it = this.srv.getColRecords().find(Filters.eq("deviceId", dev.getId()));
		it.limit(50); // TODO make dynamic

		final User user = ctx.get(AuthHandler.USER_KEY);
		final String full = ctx.request().getParam("full");
		if ("y".equals(full) && user != null && user.getId().equals(dev.getOwner())) {
			it.limit(0);
		}

		it.forEach(r -> {
			arr.add(r.getPublicJson());
		});

		ctx.response().end(obj.encode());
	}

}
