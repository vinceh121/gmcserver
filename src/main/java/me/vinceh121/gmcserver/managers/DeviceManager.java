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

	public DeviceManager(GMCServer srv) {
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
		return new DeviceStatsAction(srv);
	}

	public DeviceTimelineAction deviceTimeline() {
		return new DeviceTimelineAction(srv);
	}

	public UpdateDeviceAction updateDevice() {
		return new UpdateDeviceAction(srv);
	}

	public GetDeviceAction getDevice() {
		return new GetDeviceAction(srv);
	}

	public DeleteDeviceAction deleteDevice() {
		return new DeleteDeviceAction(srv);
	}

	public CreateDeviceAction createDevice() {
		return new CreateDeviceAction(srv);
	}

	public class DeviceStatsAction extends AbstractAction<DeviceStats> {
		private String field;
		private ObjectId devId;

		public DeviceStatsAction(GMCServer srv) {
			super(srv);
		}

		@Override
		protected void executeSync(Promise<DeviceStats> promise) {
			final DeviceStats stats = this.srv.getManager(DatabaseManager.class)
					.getCollection(Record.class)
					.aggregate(getStatsAggregation(field, devId), DeviceStats.class)
					.first();
			if (stats != null) {
				stats.setDevice(devId);
				stats.setField(field);
				promise.complete(stats);
			} else
				promise.fail("Could not get stats");
		}

		public String getField() {
			return field;
		}

		public DeviceStatsAction setField(String field) {
			this.field = field;
			return this;
		}

		public ObjectId getDevId() {
			return devId;
		}

		public DeviceStatsAction setDevId(ObjectId devId) {
			this.devId = devId;
			return this;
		}
	}

	public class DeviceTimelineAction extends AbstractAction<List<Record>> {
		private User requester;
		private Device dev;
		private Date start, end;
		private boolean full;

		public DeviceTimelineAction(GMCServer srv) {
			super(srv);
		}

		@Override
		protected void executeSync(Promise<List<Record>> promise) {

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

			if (full && requester != null && requester.getId().equals(dev.getOwner())) {
				it.limit(0);
			}

			final List<Record> records = new Vector<>();
			it.forEach(r -> records.add(r));

			promise.complete(records);
		}

		public User getRequester() {
			return requester;
		}

		public DeviceTimelineAction setRequester(User requester) {
			this.requester = requester;
			return this;
		}

		public Device getDev() {
			return dev;
		}

		public DeviceTimelineAction setDev(Device dev) {
			this.dev = dev;
			return this;
		}

		public Date getStart() {
			return start;
		}

		public DeviceTimelineAction setStart(Date start) {
			this.start = start;
			return this;
		}

		public Date getEnd() {
			return end;
		}

		public DeviceTimelineAction setEnd(Date end) {
			this.end = end;
			return this;
		}

		public boolean isFull() {
			return full;
		}

		public DeviceTimelineAction setFull(boolean full) {
			this.full = full;
			return this;
		}

	}

	public class UpdateDeviceAction extends AbstractAction<Long> {
		private Device device;
		private String name, model;
		private JsonArray arrLocation;

		public UpdateDeviceAction(GMCServer srv) {
			super(srv);
		}

		@Override
		protected void executeSync(Promise<Long> promise) {
			final List<Bson> updates = new Vector<>();

			if (name != null) {
				updates.add(Updates.set("name", name));
			}

			if (model != null) {
				updates.add(Updates.set("model", model));
			}

			if (arrLocation != null) {
				updates.add(Updates.set("location", jsonArrToPoint(arrLocation)));
			}

			final UpdateResult res = this.srv.getManager(DatabaseManager.class)
					.getCollection(Device.class)
					.updateOne(Filters.eq(device.getId()), Updates.combine(updates));

			if (res.wasAcknowledged())
				promise.complete(res.getModifiedCount());
			else
				promise.fail("Failed to save changes");
		}

		public Device getDevice() {
			return device;
		}

		public UpdateDeviceAction setDevice(Device device) {
			this.device = device;
			return this;
		}

		public String getName() {
			return name;
		}

		public UpdateDeviceAction setName(String name) {
			this.name = name;
			return this;
		}

		public String getModel() {
			return model;
		}

		public UpdateDeviceAction setModel(String model) {
			this.model = model;
			return this;
		}

		public JsonArray getArrLocation() {
			return arrLocation;
		}

		public UpdateDeviceAction setArrLocation(JsonArray arrLocation) {
			this.arrLocation = arrLocation;
			return this;
		}

	}

	public class GetDeviceAction extends AbstractAction<Device> {
		private ObjectId id;

		public GetDeviceAction(GMCServer srv) {
			super(srv);
		}

		@Override
		protected void executeSync(Promise<Device> promise) {
			final Device dev = this.srv.getManager(DatabaseManager.class)
					.getCollection(Device.class)
					.find(Filters.eq(id))
					.first();

			if (dev == null) {
				promise.fail("Device not found");
				return;
			}

			promise.complete(dev);
		}

		public ObjectId getId() {
			return id;
		}

		public GetDeviceAction setId(ObjectId id) {
			this.id = id;
			return this;
		}
	}

	public class DeleteDeviceAction extends AbstractAction<Void> {
		private ObjectId deviceId;
		private User user;
		private boolean delete;

		public DeleteDeviceAction(GMCServer srv) {
			super(srv);
		}

		@Override
		protected void executeSync(Promise<Void> promise) {
			final Device dev = this.srv.getManager(DatabaseManager.class)
					.getCollection(Device.class)
					.find(Filters.eq(deviceId))
					.first();

			if (dev == null) {
				promise.fail("Device not found");
				return;
			}

			if (!user.getId().equals(dev.getId()) || user.isAdmin()) {
				promise.fail("Not owner of device");
				return;
			}

			if (!delete) {
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
			return deviceId;
		}

		public DeleteDeviceAction setDeviceId(ObjectId deviceId) {
			this.deviceId = deviceId;
			return this;
		}

		public User getUser() {
			return user;
		}

		public DeleteDeviceAction setUser(User user) {
			this.user = user;
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

	public class CreateDeviceAction extends AbstractAction<Device> {
		private boolean ignoreDeviceLimit, insertInDb = true, generateGmcId = true;
		private User user;
		private JsonArray arrLocation;
		private String name;

		public CreateDeviceAction(GMCServer srv) {
			super(srv);
		}

		@Override
		protected void executeSync(Promise<Device> promise) {
			if (name == null) {
				promise.fail("Parameter name missing");
				return;
			}

			final int deviceLimit;
			if (user.getDeviceLimit() != -1) {
				deviceLimit = user.getDeviceLimit();
			} else {
				deviceLimit = Integer.parseInt(this.srv.getConfig().getProperty("device.user-limit"));
			}

			if (deviceLimit <= this.srv.getManager(DatabaseManager.class)
					.getCollection(Device.class)
					.countDocuments(Filters.eq("ownerId", user.getId()))) {
				promise.fail("Device limit reached");
				return;
			}

			final Point location;
			if (arrLocation != null && arrLocation.size() == 2) {
				location = jsonArrToPoint(arrLocation);
			} else if (arrLocation != null && arrLocation.size() != 2) {
				promise.fail("Invalid location");
				location = null;
			} else {
				location = null;
			}

			final Device dev = new Device();
			dev.setOwner(user.getId());
			dev.setName(name);
			if (generateGmcId)
				dev.setGmcId(Math.abs(DEVICE_RNG.nextLong()));
			dev.setLocation(location);

			promise.complete(dev);

			if (insertInDb) {
				this.srv.getManager(DatabaseManager.class).getCollection(Device.class).insertOne(dev);
			}
		}

		public boolean isInsertInDb() {
			return insertInDb;
		}

		public CreateDeviceAction setInsertInDb(boolean insertInDb) {
			this.insertInDb = insertInDb;
			return this;
		}

		public User getUser() {
			return user;
		}

		public CreateDeviceAction setUser(User owner) {
			this.user = owner;
			return this;
		}

		public boolean isIgnoreDeviceLimit() {
			return ignoreDeviceLimit;
		}

		public CreateDeviceAction setIgnoreDeviceLimit(boolean ignoreDeviceLimit) {
			this.ignoreDeviceLimit = ignoreDeviceLimit;
			return this;
		}

		public boolean isGenerateGmcId() {
			return generateGmcId;
		}

		public CreateDeviceAction setGenerateGmcId(boolean generateGmcId) {
			this.generateGmcId = generateGmcId;
			return this;
		}

		public JsonArray getArrLocation() {
			return arrLocation;
		}

		public CreateDeviceAction setArrLocation(JsonArray arrLocation) {
			this.arrLocation = arrLocation;
			return this;
		}

		public String getName() {
			return name;
		}

		public CreateDeviceAction setName(String name) {
			this.name = name;
			return this;
		}

	}
}
