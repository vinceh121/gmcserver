package me.vinceh121.gmcserver.modules;

import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Vector;

import org.bson.BsonNull;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.bson.types.ObjectId;

import com.mongodb.client.FindIterable;
import com.mongodb.client.model.Accumulators;
import com.mongodb.client.model.Aggregates;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Sorts;
import com.mongodb.client.model.Updates;

import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import me.vinceh121.gmcserver.DatabaseManager;
import me.vinceh121.gmcserver.GMCServer;
import me.vinceh121.gmcserver.entities.Device;
import me.vinceh121.gmcserver.entities.Record;
import me.vinceh121.gmcserver.entities.User;
import me.vinceh121.gmcserver.handlers.AuthHandler;
import me.vinceh121.gmcserver.managers.DeviceManager;
import me.vinceh121.gmcserver.managers.DeviceManager.CreateDeviceAction;
import me.vinceh121.gmcserver.managers.DeviceManager.DeleteDeviceAction;

public class DeviceModule extends AbstractModule {

	public DeviceModule(final GMCServer srv) {
		super(srv);
		this.registerStrictAuthedRoute(HttpMethod.POST, "/device", this::handleCreateDevice);
		this.registerStrictAuthedRoute(HttpMethod.DELETE, "/device/:deviceId", this::handleRemoveDevice);
		this.registerStrictAuthedRoute(HttpMethod.PUT, "/device/:deviceId", this::handleUpdateDevice);
		this.registerAuthedRoute(HttpMethod.GET, "/device/:deviceId", this::handleDevice);
		this.registerAuthedRoute(HttpMethod.GET, "/device/:deviceId/timeline", this::handleDeviceHistory);
		this.registerRoute(HttpMethod.GET, "/device/:deviceId/stats/:field", this::handleStats);
	}

	private void handleCreateDevice(final RoutingContext ctx) {
		final JsonObject obj = ctx.getBodyAsJson();

		final User user = ctx.get(AuthHandler.USER_KEY);

		final String name = obj.getString("name");
		if (name == null) {
			this.error(ctx, 400, "Missing parameter name");
			return;
		}

		final JsonArray arrLoc = obj.getJsonArray("position");
		if (arrLoc == null) {
			this.error(ctx, 400, "Missing parameter position");
			return;
		}

		final CreateDeviceAction action = this.srv.getManager(DeviceManager.class)
				.createDevice()
				.setUser(user)
				.setArrLocation(arrLoc)
				.setName(name);
		action.execute().onComplete(res -> {
			if (res.failed()) {
				this.error(ctx, 400, res.cause().getMessage());
				return;
			}
			ctx.response().end(res.result().toJson().toBuffer());
		});
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

		final User user = ctx.get(AuthHandler.USER_KEY);

		final JsonObject obj = ctx.getBodyAsJson();
		final boolean delete = obj.getBoolean("delete");

		final DeleteDeviceAction action = this.srv.getManager(DeviceManager.class)
				.deleteDevice()
				.setDelete(delete)
				.setDeviceId(deviceId)
				.setUser(user);

		action.execute().onComplete(res -> {
			if (res.failed()) {
				this.error(ctx, 400, res.cause().getMessage());
				return;
			}

			ctx.response().end(new JsonObject().put("delete", delete).toBuffer());
		});
	}

	private void handleUpdateDevice(final RoutingContext ctx) {
		final String rawDeviceId = ctx.pathParam("deviceId"); // TODO make action somehow

		final ObjectId deviceId;
		try {
			deviceId = new ObjectId(rawDeviceId);
		} catch (final IllegalArgumentException e) {
			this.error(ctx, 400, "Invalid device ID");
			return;
		}

		final Device dev = this.srv.getManager(DatabaseManager.class)
				.getCollection(Device.class)
				.find(Filters.eq(deviceId))
				.first();

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

//		final JsonArray location = obj.getJsonArray("location"); // XXX to include but i want it to compile in the meantime
//		if (location != null) {
//			updates.add(Updates.set("location", this.jsonArrToPoint(location)));
//		}

		final Boolean disabled = obj.getBoolean("disabled");
		if (disabled != null) {
			updates.add(Updates.set("disabled", disabled.booleanValue()));
		}

		this.srv.getManager(DatabaseManager.class)
				.getCollection(Device.class)
				.updateOne(Filters.eq(dev.getId()), Updates.combine(updates));

		ctx.response().end(new JsonObject().put("changed", updates.size()).toBuffer());
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

		final Device dev = this.srv.getManager(DatabaseManager.class)
				.getCollection(Device.class)
				.find(Filters.eq(deviceId))
				.first();

		if (dev == null) {
			this.error(ctx, 404, "Device not found");
			return;
		}

		final User owner = this.srv.getManager(DatabaseManager.class)
				.getCollection(User.class)
				.find(Filters.eq(dev.getOwner()))
				.first();

		final User user = ctx.get(AuthHandler.USER_KEY);

		final boolean own = user != null && user.getId().equals(dev.getOwner());

		final JsonObject obj = (own ? dev.toJson() : dev.toPublicJson());
		obj.put("own", own);
		obj.put("owner", owner.toPublicJson());

		ctx.response().end(obj.toBuffer());
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

		final Device dev = this.srv.getManager(DatabaseManager.class)
				.getCollection(Device.class)
				.find(Filters.eq(deviceId))
				.first();

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
				= this.srv.getManager(DatabaseManager.class).getCollection(Record.class).find(Filters.and(filters));
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

	private void handleStats(final RoutingContext ctx) {
		final String rawDevId = ctx.pathParam("deviceId");

		final ObjectId devId;
		try {
			devId = new ObjectId(rawDevId);
		} catch (final IllegalArgumentException e) {
			this.error(ctx, 400, "Invalid ID");
			return;
		}

		final Device dev = this.srv.getManager(DatabaseManager.class)
				.getCollection(Device.class)
				.find(Filters.eq(devId))
				.first();

		if (dev == null) {
			this.error(ctx, 404, "Device not found");
			return;
		}

		final String field = ctx.pathParam("field");
		if (!Record.STAT_FIELDS.contains(field)) {
			this.error(ctx, 400, "Invalid field");
			return;
		}

		final Document doc = this.srv.getManager(DatabaseManager.class)
				.getCollection(Record.class)
				.aggregate(this.getStatsAggregation(field, devId), Document.class)
				.first();

		final JsonObject obj = new JsonObject(doc);
		obj.remove("_id");
		obj.put("device", dev.getId().toHexString());
		obj.put("field", field);

		ctx.response().end(obj.toBuffer());
	}

	private List<Bson> getStatsAggregation(final String field, final ObjectId devId) {
		return Arrays.asList(Aggregates.match(Filters.eq("deviceId", devId)),
				Aggregates.group(new BsonNull(), Accumulators.avg("avg", "$" + field),
						Accumulators.min("min", "$" + field), Accumulators.max("max", "$" + field),
						Accumulators.stdDevPop("stdDev", "$" + field)));
	}
}
