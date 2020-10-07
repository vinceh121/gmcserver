package me.vinceh121.gmcserver.managers;

import java.security.SecureRandom;
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
import me.vinceh121.gmcserver.DatabaseManager;
import me.vinceh121.gmcserver.GMCServer;
import me.vinceh121.gmcserver.actions.AbstractAction;
import me.vinceh121.gmcserver.entities.Device;
import me.vinceh121.gmcserver.entities.DeviceStats;
import me.vinceh121.gmcserver.entities.Record;
import me.vinceh121.gmcserver.entities.User;

public class DeviceManager extends AbstractManager {
	private static final Random DEVICE_RNG = new SecureRandom();

	public DeviceManager(final GMCServer srv) {
		super(srv);
	}

	private Point jsonArrToPoint(final JsonArray arr) { // standard lat lon in array -> lon lat for mongo
		final Position pos = new Position(arr.getDouble(1), arr.getDouble(0));
		final Point point = new Point(pos);
		return point;
	}

	private List<Bson> getStatsAggregation(final String field, final ObjectId devId) {
		return Arrays.asList(Aggregates.match(Filters.eq("deviceId", devId)),
				Aggregates.group(new BsonNull(), Accumulators.avg("avg", "$" + field),
						Accumulators.min("min", "$" + field), Accumulators.max("max", "$" + field),
						Accumulators.stdDevPop("stdDev", "$" + field)));
	}

	public DeviceStatsAction deviceStats() {
		return new DeviceStatsAction(this.srv);
	}

	public DeviceTimelineAction deviceTimeline() {
		return new DeviceTimelineAction(this.srv);
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

	public class DeviceStatsAction extends AbstractAction<DeviceStats> {
		private String field;
		private ObjectId devId;

		public DeviceStatsAction(final GMCServer srv) {
			super(srv);
		}

		@Override
		protected void executeSync(final Promise<DeviceStats> promise) {
			final DeviceStats stats = this.srv.getManager(DatabaseManager.class)
					.getCollection(Record.class)
					.aggregate(DeviceManager.this.getStatsAggregation(this.field, this.devId), DeviceStats.class)
					.first();
			if (stats != null) {
				stats.setDevice(this.devId);
				stats.setField(this.field);
				promise.complete(stats);
			} else {
				promise.fail("Could not get stats");
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
	}

	public class DeviceTimelineAction extends AbstractAction<Iterable<Record>> {
		private User requester;
		private Device dev;
		private Date start, end;
		private boolean full;

		public DeviceTimelineAction(final GMCServer srv) {
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

			final FindIterable<Record> it
					= this.srv.getManager(DatabaseManager.class).getCollection(Record.class).find(Filters.and(filters));
			it.sort(Sorts.descending("date"));
			it.limit(Integer.parseInt(this.srv.getConfig().getProperty("device.public-timeline-limit")));

			if (this.full && this.requester != null && this.requester.getId().equals(this.dev.getOwner())) {
				it.limit(0);
			}

			promise.complete(it);
		}

		public User getRequester() {
			return this.requester;
		}

		public DeviceTimelineAction setRequester(final User requester) {
			this.requester = requester;
			return this;
		}

		public Device getDev() {
			return this.dev;
		}

		public DeviceTimelineAction setDev(final Device dev) {
			this.dev = dev;
			return this;
		}

		public Date getStart() {
			return this.start;
		}

		public DeviceTimelineAction setStart(final Date start) {
			this.start = start;
			return this;
		}

		public Date getEnd() {
			return this.end;
		}

		public DeviceTimelineAction setEnd(final Date end) {
			this.end = end;
			return this;
		}

		public boolean isFull() {
			return this.full;
		}

		public DeviceTimelineAction setFull(final boolean full) {
			this.full = full;
			return this;
		}

	}

	public class UpdateDeviceAction extends AbstractAction<Long> {
		private Device device;
		private String name, model;
		private JsonArray arrLocation;

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
				updates.add(Updates.set("location", DeviceManager.this.jsonArrToPoint(this.arrLocation)));
			}

			final UpdateResult res = this.srv.getManager(DatabaseManager.class)
					.getCollection(Device.class)
					.updateOne(Filters.eq(this.device.getId()), Updates.combine(updates));

			if (res.wasAcknowledged()) {
				promise.complete(res.getModifiedCount());
			} else {
				promise.fail("Failed to save changes");
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

	}

	public class GetDeviceAction extends AbstractAction<Device> {
		private ObjectId id;

		public GetDeviceAction(final GMCServer srv) {
			super(srv);
		}

		@Override
		protected void executeSync(final Promise<Device> promise) {
			final Device dev = this.srv.getManager(DatabaseManager.class)
					.getCollection(Device.class)
					.find(Filters.eq(this.id))
					.first();

			if (dev == null) {
				promise.fail("Device not found");
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

	public class DeleteDeviceAction extends AbstractAction<Void> {
		private ObjectId deviceId;
		private User user;
		private boolean delete;

		public DeleteDeviceAction(final GMCServer srv) {
			super(srv);
		}

		@Override
		protected void executeSync(final Promise<Void> promise) {
			final Device dev = this.srv.getManager(DatabaseManager.class)
					.getCollection(Device.class)
					.find(Filters.eq(this.deviceId))
					.first();

			if (dev == null) {
				promise.fail("Device not found");
				return;
			}

			if (!this.user.getId().equals(dev.getId()) || this.user.isAdmin()) {
				promise.fail("Not owner of device");
				return;
			}

			if (!this.delete) {
				this.srv.getManager(DatabaseManager.class)
						.getCollection(Device.class)
						.updateOne(Filters.eq(dev.getId()), Updates.set("disabled", true));
			} else {
				this.srv.getManager(DatabaseManager.class)
						.getCollection(Record.class)
						.deleteMany(Filters.eq("deviceId", dev.getId()));
				this.srv.getManager(DatabaseManager.class)
						.getCollection(Device.class)
						.deleteOne(Filters.eq(dev.getId()));
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

		public User getUser() {
			return this.user;
		}

		public DeleteDeviceAction setUser(final User user) {
			this.user = user;
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

	public class CreateDeviceAction extends AbstractAction<Device> {
		private boolean ignoreDeviceLimit, insertInDb = true, generateGmcId = true;
		private User user;
		private JsonArray arrLocation;
		private String name;

		public CreateDeviceAction(final GMCServer srv) {
			super(srv);
		}

		@Override
		protected void executeSync(final Promise<Device> promise) {
			if (this.name == null) {
				promise.fail("Parameter name missing");
				return;
			}

			final int deviceLimit;
			if (this.user.getDeviceLimit() != -1) {
				deviceLimit = this.user.getDeviceLimit();
			} else {
				deviceLimit = Integer.parseInt(this.srv.getConfig().getProperty("device.user-limit"));
			}

			if (deviceLimit <= this.srv.getManager(DatabaseManager.class)
					.getCollection(Device.class)
					.countDocuments(Filters.eq("ownerId", this.user.getId()))) {
				promise.fail("Device limit reached");
				return;
			}

			final Point location;
			if (this.arrLocation != null && this.arrLocation.size() == 2) {
				location = DeviceManager.this.jsonArrToPoint(this.arrLocation);
			} else if (this.arrLocation != null && this.arrLocation.size() != 2) {
				promise.fail("Invalid location");
				location = null;
			} else {
				location = null;
			}

			final Device dev = new Device();
			dev.setOwner(this.user.getId());
			dev.setName(this.name);
			if (this.generateGmcId) {
				dev.setGmcId(Math.abs(DeviceManager.DEVICE_RNG.nextLong()));
			}
			dev.setLocation(location);

			promise.complete(dev);

			if (this.insertInDb) {
				this.srv.getManager(DatabaseManager.class).getCollection(Device.class).insertOne(dev);
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

	}
}
