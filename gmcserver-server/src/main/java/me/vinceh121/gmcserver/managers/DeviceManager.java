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
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.pgclient.data.Point;
import io.vertx.sqlclient.Tuple;
import me.vinceh121.gmcserver.GMCServer;
import me.vinceh121.gmcserver.actions.AbstractAction;
import me.vinceh121.gmcserver.entities.Device;
import me.vinceh121.gmcserver.entities.DeviceStats;
import me.vinceh121.gmcserver.entities.Record;
import me.vinceh121.gmcserver.entities.User;
import me.vinceh121.gmcserver.exceptions.EntityNotFoundException;
import me.vinceh121.gmcserver.exceptions.LimitReachedException;

public class DeviceManager extends AbstractManager {
	/**
	 * GQ GMC accept a maximum of 12 chars as User and Device IDs
	 */
	public static final long MAX_GMCID = 999999999999L;
	private static final Random DEVICE_RNG = new SecureRandom();

	public DeviceManager(final GMCServer srv) {
		super(srv);
	}

	private static Point jsonArrToPoint(final JsonArray arr) {
		final Point point = new Point(arr.getDouble(0), arr.getDouble(1));
		return point;
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

	public GetMapAction getMap() {
		return new GetMapAction(this.srv);
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
		private UUID devId;
		private int sampleSize = 1000;
		private Date start, end;

		public DeviceStatsAction(final GMCServer srv) {
			super(srv);
		}

		@Override
		protected void executeSync(final Promise<DeviceStats> promise) {
			final DeviceStats stats = new DeviceStats(); // TODO

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

		public UUID getDevId() {
			return this.devId;
		}

		public DeviceStatsAction setDevId(final UUID devId) {
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
			Map<String, Object> parameters = new HashMap<>();
			parameters.put("deviceId", this.dev.getId());
			StringBuilder query = new StringBuilder("SELECT * FROM records WHERE deviceId=#{deviceId}");

			if (this.start != null) {
				query.append(" AND date > #{start}");
				parameters.put("start", this.start);
			}

			if (this.end != null) {
				query.append(" AND date < #{end}");
				parameters.put("end", this.end);
			}

			query.append(" ORDER BY date");
			query.append(" LIMIT ");

			if (this.full) {
				query.append("ALL");
			} else {
				// TODO only return nths records when limit is hit
				// int parse is a sanity-check
				query.append(Integer.parseInt(this.srv.getConfig().getProperty("device.public-timeline-limit")));
			}

			this.srv.getDatabaseManager()
				.query(query.toString())
				.mapTo(Record.class)
				.execute(parameters)
				.onSuccess(promise::complete)
				.onFailure(promise::fail);
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
	public class UpdateDeviceAction extends AbstractAction<Void> {
		private UUID deviceId;
		private String name, model;
		private Point location;
		private JsonObject proxiesSettings;

		public UpdateDeviceAction(final GMCServer srv) {
			super(srv);
		}

		@Override
		protected void executeSync(final Promise<Void> promise) {
			@SuppressWarnings("rawtypes") // bad vertx, CompositeFuture.all doesn't use proper type args
			List<Future> updates = new ArrayList<>(3);

			if (this.name != null) {
				updates.add(this.srv.getDatabaseManager()
					.getPool()
					.preparedQuery("UPDATE devices SET name=$1 WHERE id=$2")
					.execute(Tuple.of(this.name, this.deviceId)));
			}

			if (this.model != null) {
				updates.add(this.srv.getDatabaseManager()
					.getPool()
					.preparedQuery("UPDATE devices SET model=$1 WHERE id=$2")
					.execute(Tuple.of(this.model, this.deviceId)));
			}

			if (this.location != null) {
				updates.add(this.srv.getDatabaseManager()
					.getPool()
					.preparedQuery("UPDATE devices SET location=$1 WHERE id=$2")
					.execute(Tuple.of(this.location, this.deviceId)));
			}

			if (this.proxiesSettings != null) {
				for (final String field : this.proxiesSettings.fieldNames()) {
					if (!this.srv.getProxyManager().getProxies().containsKey(field)) {
						promise.fail(new IllegalArgumentException("Unknown proxy type: '" + field + "'"));
						return;
					}
				}
				updates.add(this.srv.getDatabaseManager()
					.getPool()
					.preparedQuery("UPDATE devices SET proxiesSettings=$1 WHERE id=$2")
					.execute(Tuple.of(this.proxiesSettings, this.deviceId)));
			}

			CompositeFuture.all(updates).onSuccess(f -> promise.complete()).onFailure(promise::fail);
		}

		public UpdateDeviceAction setDevice(final Device device) {
			this.deviceId = device.getId();
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

		public JsonObject getProxiesSettings() {
			return this.proxiesSettings;
		}

		public UpdateDeviceAction setProxiesSettings(final JsonObject proxiesSettings) {
			this.proxiesSettings = proxiesSettings;
			return this;
		}

		public UUID getDeviceId() {
			return deviceId;
		}

		public UpdateDeviceAction setDeviceId(UUID deviceId) {
			this.deviceId = deviceId;
			return this;
		}

		public Point getLocation() {
			return location;
		}

		public UpdateDeviceAction setLocation(Point location) {
			this.location = location;
			return this;
		}
	}

	/**
	 * Fetches a device.
	 * 
	 * Throws a {@code EntityNotFoundException} if the device was not found.
	 */
	public class GetDeviceAction extends AbstractAction<Device> {
		private UUID id;
		private Long gmcId;
		private boolean fetchLastRecord;

		public GetDeviceAction(final GMCServer srv) {
			super(srv);
		}

		@Override
		protected void executeSync(final Promise<Device> promise) {
			if (this.fetchLastRecord) {
				this.aggregateLastRecord(promise);
			} else {
				this.classicFind(promise);
			}
		}

		private void aggregateLastRecord(final Promise<Device> promise) {
			// TODO
			promise.complete(null);
		}

		private void classicFind(final Promise<Device> promise) {
			this.srv.getDatabaseManager()
				.query("SELECT * FROM devices WHERE " + (this.gmcId == null ? "id = #{id}" : "gmcId = #{gmcId}"))
				.mapTo(Device.class)
				.execute(Map.of("id", this.id, "gmcId", this.gmcId))
				.onSuccess(rowSet -> {
					final Device dev = rowSet.iterator().next();

					if (dev == null) {
						promise.fail(new EntityNotFoundException("Device not found"));
						return;
					}

					promise.complete(dev);
				})
				.onFailure(promise::fail);
		}

		public UUID getId() {
			return this.id;
		}

		public GetDeviceAction setId(final UUID id) {
			this.id = id;
			return this;
		}

		public boolean isFetchLastRecord() {
			return fetchLastRecord;
		}

		public GetDeviceAction setFetchLastRecord(boolean fetchLastRecord) {
			this.fetchLastRecord = fetchLastRecord;
			return this;
		}

		public Long getGmcId() {
			return gmcId;
		}

		public GetDeviceAction setGmcId(Long gmcId) {
			this.gmcId = gmcId;
			return this;
		}
	}

	public class GetMapAction extends AbstractAction<Iterable<Device>> {
		private double swlon;
		private double swlat;
		private double nelon;
		private double nelat;

		public GetMapAction(final GMCServer srv) {
			super(srv);
		}

		@Override
		protected void executeSync(final Promise<Iterable<Device>> promise) {
			this.srv.getDatabaseManager()
				.query("SELECT * FROM devices WHERE box(point(), point()) @> position")
				.mapTo(Device.class)
				.execute(Map.of("swlon", this.swlon, "swlat", this.swlat, "nelon", this.nelon, "nelat", this.nelat))
				.onSuccess(promise::complete)
				.onFailure(promise::fail);
		}

		public double getSwlon() {
			return swlon;
		}

		public GetMapAction setSwlon(double swlon) {
			this.swlon = swlon;
			return this;
		}

		public double getSwlat() {
			return swlat;
		}

		public GetMapAction setSwlat(double swlat) {
			this.swlat = swlat;
			return this;
		}

		public double getNelon() {
			return nelon;
		}

		public GetMapAction setNelon(double nelon) {
			this.nelon = nelon;
			return this;
		}

		public double getNelat() {
			return nelat;
		}

		public GetMapAction setNelat(double nelat) {
			this.nelat = nelat;
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
		private UUID deviceId;
		private boolean delete;

		public DeleteDeviceAction(final GMCServer srv) {
			super(srv);
		}

		@Override
		protected void executeSync(final Promise<Void> promise) {
			if (this.delete) {
				this.srv.getDatabaseManager()
					.update("DELETE FROM devices WHERE id = #{id}")
					.execute(Map.of("id", this.deviceId))
					.onSuccess(res -> {
						if (res.rowCount() == 0) {
							promise.fail(new EntityNotFoundException("Device " + this.deviceId + " not found"));
						} else {
							promise.complete();
						}
					})
					.onFailure(promise::fail);
			} else {
				this.srv.getDatabaseManager()
					.update("UPDATE devices SET disabled = true WHERE id = #{id}")
					.execute(Map.of("id", this.deviceId))
					.onSuccess(res -> {
						if (res.rowCount() == 0) {
							promise.fail(new EntityNotFoundException("Device " + this.deviceId + " not found"));
						} else {
							promise.complete();
						}
					})
					.onFailure(promise::fail);
			}
		}

		public UUID getDeviceId() {
			return this.deviceId;
		}

		public DeleteDeviceAction setDeviceId(final UUID deviceId) {
			this.deviceId = deviceId;
			return this;
		}

		public boolean isDelete() {
			return delete;
		}

		public DeleteDeviceAction setDelete(boolean delete) {
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

			this.srv.getDatabaseManager()
				.query("SELECT COUNT(*) FROM devices WHERE \"owner\" = #{userId}")
				.execute(Map.of("userId", this.user.getId()))
				.onSuccess(rowSet -> {
					final int userDeviceCount = rowSet.iterator().next().getInteger("count");

					if (deviceLimit <= userDeviceCount) {
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
						this.srv.getDatabaseManager()
							.update("INSERT INTO devices VALUES")
							.mapFrom(Device.class)
							.execute(dev)
							.onSuccess(rs -> promise.complete(dev))
							.onFailure(promise::fail);
					}
				})
				.onFailure(promise::fail);
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
