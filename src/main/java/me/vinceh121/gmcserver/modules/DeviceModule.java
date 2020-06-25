package me.vinceh121.gmcserver.modules;

import org.bson.types.ObjectId;

import com.mongodb.client.FindIterable;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Sorts;
import com.mongodb.client.model.geojson.Point;
import com.mongodb.client.model.geojson.Position;

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
		this.registerStrictAuthedRoute(HttpMethod.POST, "/device", this::handleCreateDevice);
		this.registerAuthedRoute(HttpMethod.GET, "/device/:deviceId", this::handleDevice);
		this.registerAuthedRoute(HttpMethod.GET, "/device/:deviceId/timeline", this::handleDeviceHistory);
	}

	private void handleCreateDevice(final RoutingContext ctx) {

	}

	private void handleDevice(final RoutingContext ctx) {
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

		dev.setLocation(new Point(new Position(43.6044622, 1.4442469)));
		this.srv.getColDevices().replaceOne(Filters.eq(dev.getId()), dev);

		final User user = ctx.get(AuthHandler.USER_KEY);

		boolean own = user != null && user.getId().equals(dev.getOwner());

		ctx.response().end((own ? dev.toJson() : dev.toPublicJson()).put("own", own).toBuffer());
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
		it.sort(Sorts.ascending("date"));
		it.limit(Integer.parseInt(this.srv.getConfig().getProperty("device.public-timeline-limit")));

		final User user = ctx.get(AuthHandler.USER_KEY);
		final String full = ctx.request().getParam("full");
		if ("y".equals(full) && user != null && user.getId().equals(dev.getOwner())) {
			it.limit(0);
		}

		if (user != null && user.getId().equals(dev.getOwner())) {
			it.forEach(r -> arr.add(r.toJson()));
		} else {
			it.forEach(r -> arr.add(r.toPublicJson()));
		}

		ctx.response().end(obj.toBuffer());
	}

}
