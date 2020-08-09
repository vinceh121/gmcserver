package me.vinceh121.gmcserver.managers;

import java.security.SecureRandom;
import java.util.Random;

import org.bson.types.ObjectId;

import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Updates;
import com.mongodb.client.model.geojson.Point;
import com.mongodb.client.model.geojson.Position;

import io.vertx.core.Promise;
import io.vertx.core.json.JsonArray;
import me.vinceh121.gmcserver.DatabaseManager;
import me.vinceh121.gmcserver.GMCServer;
import me.vinceh121.gmcserver.actions.AbstractAction;
import me.vinceh121.gmcserver.entities.Device;
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

	public DeleteDeviceAction deleteDevice() {
		return new DeleteDeviceAction(srv);
	}

	public CreateDeviceAction createDevice() {
		return new CreateDeviceAction(srv);
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
