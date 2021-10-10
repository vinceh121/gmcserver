/*
 * GMCServer, lightweight service to log, analyze and proxy Geiger counter data.
 * Copyright (C) 2020 Vincent Hyvert
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public for more details.
 *
 * You should have received a copy of the GNU Affero General Public
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package me.vinceh121.gmcserver.managers;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Random;
import java.util.Vector;

import org.bson.BsonNull;
import org.bson.conversions.Bson;
import org.bson.types.ObjectId;

import com.mongodb.client.FindIterable;
import com.mongodb.client.model.Accumulators;
import com.mongodb.client.model.Aggregates;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Sorts;
import com.mongodb.client.model.Updates;
import com.mongodb.client.model.geojson.Point;
import com.mongodb.client.model.geojson.Position;
import com.mongodb.client.result.UpdateResult;

import io.vertx.core.Promise;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import me.vinceh121.gmcserver.GMCServer;
import me.vinceh121.gmcserver.actions.AbstractAction;
import me.vinceh121.gmcserver.entities.Device;
import me.vinceh121.gmcserver.entities.DeviceCalendar;
import me.vinceh121.gmcserver.entities.DeviceStats;
import me.vinceh121.gmcserver.entities.Record;
import me.vinceh121.gmcserver.entities.User;
import me.vinceh121.gmcserver.exceptions.EntityNotFoundException;
import me.vinceh121.gmcserver.exceptions.LimitReachedException;

public class DeviceManager extends AbstractManager {
	/**
	 * GQ GMC accept a maximum of 16 chars as User and Device IDs
	 */
	public static final long MAX_GMCID = 9999999999999999L;
	private static final Random DEVICE_RNG = new SecureRandom();

	public DeviceManager(final GMCServer srv) {
		super(srv);
	}

	private static Point jsonArrToPoint(final JsonArray arr) {
		final Position pos = new Position(arr.getDouble(0), arr.getDouble(1));
		final Point point = new Point(pos);
		return point;
	}

	private static List<Bson> getStatsAggregation(final String field, final ObjectId devId, final int limit,
			final Date start, final Date end) {
		List<Bson> filters = new ArrayList<>(3);
		filters.add(Filters.eq("deviceId", devId));
		if (start != null) {
			filters.add(Filters.gte("date", start));
		}

		if (end != null) {
			filters.add(Filters.lte("date", end));
		}
		return Arrays.asList(Aggregates.match(Filters.and(filters)),
				Aggregates.sort(Sorts.descending("date")),
				Aggregates.limit(limit),
				Aggregates.group(new BsonNull(),
						Accumulators.avg("avg", "$" + field),
						Accumulators.min("min", "$" + field),
						Accumulators.max("max", "$" + field),
						Accumulators.stdDevPop("stdDev", "$" + field)));
	}

	public DeviceStatsAction deviceStats() {
		return new DeviceStatsAction(this.srv);
	}

	public DeviceFullTimelineAction deviceFullTimeline() {
		return new DeviceFullTimelineAction(this.srv);
	}

	public UpdateDeviceAction updateDevice() {
		return new UpdateDeviceAction(this.srv);
	}

	public GetDeviceAction getDevice() {
		return new GetDeviceAction(this.srv);
	}

	public DeleteDeviceAction deleteDevice() {
		return new DeleteDeviceAction(this.srv);
	}

	public CreateDeviceAction createDevice() {
		return new CreateDeviceAction(this.srv);
	}

	/**
	 * Calculate's a device's latest stats and returns them.
	 * 
	 * Throws {@code IllegalStateException} when the database answered a null
	 * object.
	 */
	public class DeviceStatsAction extends AbstractAction<DeviceStats> {
		private String field;
		private ObjectId devId;
		private int sampleSize = 500;
		private Date start, end;

		public DeviceStatsAction(final GMCServer srv) {
			super(srv);
		}

		@Override
		protected void executeSync(final Promise<DeviceStats> promise) {
			final DeviceStats stats = this.srv.getDatabaseManager()
				.getCollection(Record.class)
				.aggregate(
						DeviceManager
							.getStatsAggregation(this.field, this.devId, this.sampleSize, this.start, this.end),
						DeviceStats.class)
				.first();
			if (stats != null) {
				stats.setDevice(this.devId);
				stats.setField(this.field);
				stats.setSampleSize(this.sampleSize);
				promise.complete(stats);
			} else {
				promise.fail(new IllegalStateException("Could not get stats"));
			}
		}

		public String getField() {
			return this.field;
		}

		public DeviceStatsAction setField(final String field) {
			this.field = field;
			return this;
		}

		public ObjectId getDevId() {
			return this.devId;
		}

		public DeviceStatsAction setDevId(final ObjectId devId) {
			this.devId = devId;
			return this;
		}

		public int getSampleSize() {
			return this.sampleSize;
		}

		public void setSampleSize(final int sampleSize) {
			this.sampleSize = sampleSize;
		}

		public Date getStart() {
			return start;
		}

		public DeviceStatsAction setStart(Date start) {
			this.start = start;
			return this;
		}

		public Date getEnd() {
			return end;
		}

		public DeviceStatsAction setEnd(Date end) {
			this.end = end;
			return this;
		}
	}

	/**
	 * Returns an iterable that allows streaming a device's timeline, in full or
	 * within given limits.
	 */
	public class DeviceFullTimelineAction extends AbstractAction<Iterable<Record>> {
		private Device dev;
		private Date start, end;
		private boolean full;

		public DeviceFullTimelineAction(final GMCServer srv) {
			super(srv);
		}

		@Override
		protected void executeSync(final Promise<Iterable<Record>> promise) {

			final Collection<Bson> filters = new Vector<>();

			filters.add(Filters.eq("deviceId", this.dev.getId()));

			if (this.start != null) {
				filters.add(Filters.gte("date", this.start));
			}

			if (this.end != null) {
				filters.add(Filters.lte("date", this.end));
			}

			final FindIterable<Record> it = this.srv.getDatabaseManager()
				.getCollection(Record.class)
				.find(Filters.and(filters));
			it.sort(Sorts.descending("date"));
			it.limit(Integer.parseInt(this.srv.getConfig().getProperty("device.public-timeline-limit")));

			if (this.full) {
				it.limit(0);
			}

			promise.complete(it);
		}

		public Device getDev() {
			return this.dev;
		}

		public DeviceFullTimelineAction setDev(final Device dev) {
			this.dev = dev;
			return this;
		}

		public Date getStart() {
			return this.start;
		}

		public DeviceFullTimelineAction setStart(final Date start) {
			this.start = start;
			return this;
		}

		public Date getEnd() {
			return this.end;
		}

		public DeviceFullTimelineAction setEnd(final Date end) {
			this.end = end;
			return this;
		}

		public boolean isFull() {
			return this.full;
		}

		public DeviceFullTimelineAction setFull(final boolean full) {
			this.full = full;
			return this;
		}

	}

	/**
	 * Updates a device.
	 * 
	 * Throws {@code IllegalStateException} if the database didn't acknowledge the
	 * update.
	 */
	public class UpdateDeviceAction extends AbstractAction<Long> {
		private Device device;
		private String name, model;
		private JsonArray arrLocation;
		private JsonObject proxiesSettings;

		public UpdateDeviceAction(final GMCServer srv) {
			super(srv);
		}

		@Override
		protected void executeSync(final Promise<Long> promise) {
			final List<Bson> updates = new Vector<>();

			if (this.name != null) {
				updates.add(Updates.set("name", this.name));
			}

			if (this.model != null) {
				updates.add(Updates.set("model", this.model));
			}

			if (this.arrLocation != null) {
				updates.add(Updates.set("location", DeviceManager.jsonArrToPoint(this.arrLocation)));
			}

			if (this.proxiesSettings != null) {
				for (final String field : this.proxiesSettings.fieldNames()) {
					if (!this.srv.getProxyManager().getProxies().containsKey(field)) {
						promise.fail(new IllegalArgumentException("Unknown proxy type: '" + field + "'"));
						return;
					}
				}
				updates.add(Updates.set("proxiesSettings", this.proxiesSettings.getMap()));
			}

			final UpdateResult res = this.srv.getDatabaseManager()
				.getCollection(Device.class)
				.updateOne(Filters.eq(this.device.getId()), Updates.combine(updates));

			if (res.getMatchedCount() == 0) {
				promise.fail(new EntityNotFoundException("Device not found"));
			} else if (res.wasAcknowledged()) {
				promise.complete(res.getModifiedCount());
			} else {
				promise.fail(new IllegalStateException("Failed to save changes"));
			}
		}

		public Device getDevice() {
			return this.device;
		}

		public UpdateDeviceAction setDevice(final Device device) {
			this.device = device;
			return this;
		}

		public String getName() {
			return this.name;
		}

		public UpdateDeviceAction setName(final String name) {
			this.name = name;
			return this;
		}

		public String getModel() {
			return this.model;
		}

		public UpdateDeviceAction setModel(final String model) {
			this.model = model;
			return this;
		}

		public JsonArray getArrLocation() {
			return this.arrLocation;
		}

		public UpdateDeviceAction setArrLocation(final JsonArray arrLocation) {
			this.arrLocation = arrLocation;
			return this;
		}

		public JsonObject getProxiesSettings() {
			return this.proxiesSettings;
		}

		public UpdateDeviceAction setProxiesSettings(final JsonObject proxiesSettings) {
			this.proxiesSettings = proxiesSettings;
			return this;
		}

	}

	/**
	 * Fetches a device.
	 * 
	 * Throws a {@code EntityNotFoundException} if the device was not found.
	 */
	public class GetDeviceAction extends AbstractAction<Device> {
		private ObjectId id;

		public GetDeviceAction(final GMCServer srv) {
			super(srv);
		}

		@Override
		protected void executeSync(final Promise<Device> promise) {
			final Device dev = this.srv.getDatabaseManager()
				.getCollection(Device.class)
				.find(Filters.eq(this.id))
				.first();

			if (dev == null) {
				promise.fail(new EntityNotFoundException("Device not found"));
				return;
			}

			promise.complete(dev);
		}

		public ObjectId getId() {
			return this.id;
		}

		public GetDeviceAction setId(final ObjectId id) {
			this.id = id;
			return this;
		}
	}

	/**
	 * Disables or deletes a device. (see boolean #delete) If #delete is true, the
	 * device's calendar and records will be deleted too.
	 *
	 * Throws a {@code EntityNotFoundException} if the device doesn't exist
	 */
	public class DeleteDeviceAction extends AbstractAction<Void> {
		private ObjectId deviceId;
		private boolean delete;

		public DeleteDeviceAction(final GMCServer srv) {
			super(srv);
		}

		@Override
		protected void executeSync(final Promise<Void> promise) {
			final Device dev = this.srv.getDatabaseManager()
				.getCollection(Device.class)
				.find(Filters.eq(this.deviceId))
				.first();

			if (dev == null) {
				promise.fail(new EntityNotFoundException("Device not found"));
				return;
			}

			if (!this.delete) {
				this.srv.getDatabaseManager()
					.getCollection(Device.class)
					.updateOne(Filters.eq(dev.getId()), Updates.set("disabled", true));
			} else {
				this.srv.getDatabaseManager()
					.getCollection(Record.class)
					.deleteMany(Filters.eq("deviceId", dev.getId()));
				this.srv.getDatabaseManager()
					.getCollection(DeviceCalendar.class)
					.deleteMany(Filters.eq("deviceId", dev.getId()));
				this.srv.getDatabaseManager().getCollection(Device.class).deleteOne(Filters.eq(dev.getId()));
			}

			promise.complete();
		}

		public ObjectId getDeviceId() {
			return this.deviceId;
		}

		public DeleteDeviceAction setDeviceId(final ObjectId deviceId) {
			this.deviceId = deviceId;
			return this;
		}

		public boolean isDelete() {
			return this.delete;
		}

		public DeleteDeviceAction setDelete(final boolean delete) {
			this.delete = delete;
			return this;
		}
	}

	/**
	 * Creates a device.
	 *
	 * Throws {@code IllegalArgumentException} if one of the arguments is incorrect.
	 * Throws {@code LimitReachedException} if the user has reached his device
	 * limit.
	 */
	public class CreateDeviceAction extends AbstractAction<Device> {
		private boolean ignoreDeviceLimit, insertInDb = true, generateGmcId = true, disabled;
		private User user;
		private JsonArray arrLocation;
		private String name, model, importedFrom;

		public CreateDeviceAction(final GMCServer srv) {
			super(srv);
		}

		@Override
		protected void executeSync(final Promise<Device> promise) {
			if (this.name == null) {
				promise.fail(new IllegalArgumentException("Parameter name missing"));
				return;
			}

			final int deviceLimit;
			if (this.user.getDeviceLimit() != -1) {
				deviceLimit = this.user.getDeviceLimit();
			} else {
				deviceLimit = Integer.parseInt(this.srv.getConfig().getProperty("device.user-limit"));
			}

			if (deviceLimit <= this.srv.getDatabaseManager()
				.getCollection(Device.class)
				.countDocuments(Filters.eq("ownerId", this.user.getId()))) {
				promise.fail(new LimitReachedException("Device limit reached"));
				return;
			}

			final Point location;
			if (this.arrLocation != null && this.arrLocation.size() == 2) {
				location = DeviceManager.jsonArrToPoint(this.arrLocation);
			} else if (this.arrLocation != null && this.arrLocation.size() != 2) {
				promise.fail(new IllegalArgumentException("Invalid location"));
				return;
			} else {
				location = null;
			}

			final Device dev = new Device();
			dev.setOwner(this.user.getId());
			dev.setName(this.name);
			dev.setModel(this.model);
			dev.setImportedFrom(this.importedFrom);
			dev.setDisabled(this.disabled);
			if (this.generateGmcId) {
				dev.setGmcId(Math.abs(DeviceManager.DEVICE_RNG.nextLong()) % MAX_GMCID);
			}
			dev.setLocation(location);

			promise.complete(dev);

			if (this.insertInDb) {
				this.srv.getDatabaseManager().getCollection(Device.class).insertOne(dev);
			}
		}

		public boolean isInsertInDb() {
			return this.insertInDb;
		}

		public CreateDeviceAction setInsertInDb(final boolean insertInDb) {
			this.insertInDb = insertInDb;
			return this;
		}

		public User getUser() {
			return this.user;
		}

		public CreateDeviceAction setUser(final User owner) {
			this.user = owner;
			return this;
		}

		public boolean isIgnoreDeviceLimit() {
			return this.ignoreDeviceLimit;
		}

		public CreateDeviceAction setIgnoreDeviceLimit(final boolean ignoreDeviceLimit) {
			this.ignoreDeviceLimit = ignoreDeviceLimit;
			return this;
		}

		public boolean isGenerateGmcId() {
			return this.generateGmcId;
		}

		public CreateDeviceAction setGenerateGmcId(final boolean generateGmcId) {
			this.generateGmcId = generateGmcId;
			return this;
		}

		public JsonArray getArrLocation() {
			return this.arrLocation;
		}

		public CreateDeviceAction setArrLocation(final JsonArray arrLocation) {
			this.arrLocation = arrLocation;
			return this;
		}

		public String getName() {
			return this.name;
		}

		public CreateDeviceAction setName(final String name) {
			this.name = name;
			return this;
		}

		public String getModel() {
			return this.model;
		}

		public CreateDeviceAction setModel(final String model) {
			this.model = model;
			return this;
		}

		public String getImportedFrom() {
			return importedFrom;
		}

		public CreateDeviceAction setImportedFrom(String importedFrom) {
			this.importedFrom = importedFrom;
			return this;
		}

		public boolean isDisabled() {
			return disabled;
		}

		public CreateDeviceAction setDisabled(boolean disabled) {
			this.disabled = disabled;
			return this;
		}
	}
}
