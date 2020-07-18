package me.vinceh121.gmcserver.modules;

import java.security.SecureRandom;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Random;
import java.util.Vector;

import org.bson.conversions.Bson;
import org.bson.types.ObjectId;

import com.mongodb.client.FindIterable;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Sorts;
import com.mongodb.client.model.Updates;
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
	private final Random rng = new SecureRandom();

	public DeviceModule(final GMCServer srv) {
		super(srv);
		this.registerStrictAuthedRoute(HttpMethod.POST, "/device", this::handleCreateDevice);
		this.registerStrictAuthedRoute(HttpMethod.DELETE, "/device/:deviceId", this::handleRemoveDevice);
		this.registerStrictAuthedRoute(HttpMethod.PUT, "/device/:deviceId", this::handleUpdateDevice);
		this.registerAuthedRoute(HttpMethod.GET, "/device/:deviceId", this::handleDevice);
		this.registerAuthedRoute(HttpMethod.GET, "/device/:deviceId/timeline", this::handleDeviceHistory);
	}

	private void handleCreateDevice(final RoutingContext ctx) {
		final JsonObject obj = ctx.getBodyAsJson();

		final User user = ctx.get(AuthHandler.USER_KEY);
		final int deviceLimit;
		if (user.getDeviceLimit() != -1) {
			deviceLimit = user.getDeviceLimit();
		} else {
			deviceLimit = Integer.parseInt(this.srv.getConfig().getProperty("device.user-limit"));
		}

		if (deviceLimit == 0) {
			this.error(ctx, 403, "Device limit reached");
			return;
		}

		final JsonArray arrLoc = obj.getJsonArray("position");
		final Point location;
		if (arrLoc != null && arrLoc.size() == 2) {
			location = this.jsonArrToPoint(arrLoc);
		} else if (arrLoc != null && arrLoc.size() != 2) {
			this.error(ctx, 400, "Invalid location");
			return;
		} else {
			location = null;
		}

		final String deviceName = obj.getString("name");
		if (deviceName == null) {
			this.error(ctx, 400, "Missing parameter name");
			return;
		}

		final String deviceModel = obj.getString("model");

		final long gmcId = this.rng.nextLong();

		final Device dev = new Device();
		dev.setOwner(user.getId());
		dev.setName(deviceName);
		dev.setModel(deviceModel);
		dev.setGmcId(gmcId);
		dev.setLocation(location);

		ctx.response().end(dev.toJson().toBuffer());
	}

	private void handleRemoveDevice(final RoutingContext ctx) {
		final String rawDeviceId = ctx.pathParam("deviceId");

		final ObjectId deviceId;
		try {
			deviceId = new ObjectId(rawDeviceId);
		} catch (final IllegalArgumentException e) {
			this.error(ctx, 400, "Invalid device ID");
			return;
		}

		final Device dev = this.srv.getDatabaseManager().getCollection(Device.class).find(Filters.eq(deviceId)).first();

		if (dev == null) {
			this.error(ctx, 404, "Device not found");
			return;
		}

		final User user = ctx.get(AuthHandler.USER_KEY);

		if (!user.getId().equals(dev.getId()) || user.isAdmin()) {
			this.error(ctx, 403, "Not owner of device");
			return;
		}

		final JsonObject obj = ctx.getBodyAsJson();

		final boolean delete = obj.getBoolean("delete");

		if (!delete) {
			this.srv.getDatabaseManager()
					.getCollection(Device.class)
					.updateOne(Filters.eq(dev.getId()), Updates.set("disabled", true));
		} else {
			this.srv.getDatabaseManager().getCollection(Record.class).deleteMany(Filters.eq("deviceId", dev.getId()));
			this.srv.getDatabaseManager().getCollection(Device.class).deleteOne(Filters.eq(dev.getId()));
		}

		ctx.response().end(new JsonObject().put("delete", delete).toBuffer());
	}

	private void handleUpdateDevice(final RoutingContext ctx) {
		final String rawDeviceId = ctx.pathParam("deviceId");

		final ObjectId deviceId;
		try {
			deviceId = new ObjectId(rawDeviceId);
		} catch (final IllegalArgumentException e) {
			this.error(ctx, 400, "Invalid device ID");
			return;
		}

		final Device dev = this.srv.getDatabaseManager().getCollection(Device.class).find(Filters.eq(deviceId)).first();

		if (dev == null) {
			this.error(ctx, 404, "Device not found");
			return;
		}

		final User user = ctx.get(AuthHandler.USER_KEY);

		if (!user.getId().equals(dev.getOwner())) {
			this.error(ctx, 403, "Not owner of the device");
			return;
		}

		final JsonObject obj = ctx.getBodyAsJson();

		final List<Bson> updates = new Vector<>();

		final String name = obj.getString("name");
		if (name != null) {
			updates.add(Updates.set("name", name));
		}

		final String model = obj.getString("model");
		if (model != null) {
			updates.add(Updates.set("model", model));
		}

		final JsonArray location = obj.getJsonArray("location");
		if (location != null) {
			updates.add(Updates.set("location", this.jsonArrToPoint(location)));
		}

		this.srv.getDatabaseManager()
				.getCollection(Device.class)
				.updateOne(Filters.eq(dev.getId()), Updates.combine(updates));

		ctx.response().end(new JsonObject().put("changed", updates.size()).toBuffer());
	}

	private Point jsonArrToPoint(final JsonArray arr) { // standard lat lon in array -> lon lat for mongo
		final Position pos = new Position(arr.getDouble(1), arr.getDouble(0));
		final Point point = new Point(pos);
		return point;
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

		final Device dev = this.srv.getDatabaseManager().getCollection(Device.class).find(Filters.eq(deviceId)).first();

		if (dev == null) {
			this.error(ctx, 404, "Device not found");
			return;
		}

		final User user = ctx.get(AuthHandler.USER_KEY);

		final boolean own = user != null && user.getId().equals(dev.getOwner());

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

		final Device dev = this.srv.getDatabaseManager().getCollection(Device.class).find(Filters.eq(deviceId)).first();

		if (dev == null) {
			this.error(ctx, 404, "Device not found");
			return;
		}

		final JsonObject obj = new JsonObject();
		final JsonArray arr = new JsonArray();
		obj.put("records", arr);

		final Date start, end;

		if (ctx.request().params().contains("start")) {
			try {
				start = new Date(Long.parseLong(ctx.request().getParam("start")));
			} catch (final NumberFormatException e) {
				this.error(ctx, 400, "Format error in start date");
				return;
			}
		} else {
			start = null;
		}

		if (ctx.request().params().contains("end")) {
			try {
				end = new Date(Long.parseLong(ctx.request().getParam("end")));
			} catch (final NumberFormatException e) {
				this.error(ctx, 400, "Format error in end date");
				return;
			}
		} else {
			end = null;
		}

		final Collection<Bson> filters = new Vector<>();

		filters.add(Filters.eq("deviceId", dev.getId()));

		if (start != null) {
			filters.add(Filters.gte("date", start));
		}

		if (end != null) {
			filters.add(Filters.lte("date", end));
		}

		final FindIterable<Record> it
				= this.srv.getDatabaseManager().getCollection(Record.class).find(Filters.and(filters));
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
