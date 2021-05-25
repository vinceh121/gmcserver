package me.vinceh121.gmcserver.managers;

import java.util.List;
import java.util.Vector;

import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import me.vinceh121.gmcserver.GMCServer;
import me.vinceh121.gmcserver.actions.AbstractAction;
import me.vinceh121.gmcserver.entities.Device;
import me.vinceh121.gmcserver.entities.Record;
import me.vinceh121.gmcserver.entities.User;

public class LoggingManager extends AbstractManager {
	/**
	 * Hex device ID must be appended
	 */
	public static final String ADDRESS_PREFIX_RECORD_LOG = "me.vinceh121.gmcserver.RECORD_LOG.";

	public LoggingManager(final GMCServer srv) {
		super(srv);
	}

	public InsertRecordAction insertRecord() {
		return new InsertRecordAction(this.srv);
	}

	public class InsertRecordAction extends AbstractAction<Void> {
		private Record record;
		private Device device;
		private User user;
		private boolean insertInDb = true, checkAlert = true, processProxy = true, publishToEventBus = true;

		public InsertRecordAction(final GMCServer srv) {
			super(srv);
		}

		@Override
		protected void executeSync(final Promise<Void> promise) {
			@SuppressWarnings("rawtypes")
			final List<Future> futures = new Vector<>();

			if (insertInDb) {
				futures.add(Future.future(p -> {
					try {
						this.srv.getDatabaseManager().getCollection(Record.class).insertOne(this.record);
						p.complete();
					} catch (final Exception e) {
						p.fail(e);
					}
				}));
			}

			if (checkAlert) {
				futures.add(this.srv.getAlertManager()
					.checkAlert()
					.setDev(this.device)
					.setOwner(this.user)
					.setLatestRecord(this.record)
					.execute());
			}

			if (processProxy) {
				futures.add(this.srv.getProxyManager()
					.processDeviceProxies()
					.setDevice(device)
					.setRecord(this.record)
					.execute());
			}

			if (publishToEventBus) {
				this.srv.getEventBus()
					.publish(LoggingManager.ADDRESS_PREFIX_RECORD_LOG + this.record.getDeviceId(), this.record);
			}

			CompositeFuture.join(futures).onSuccess(c -> promise.complete()).onFailure(promise::fail);
		}

		public Record getRecord() {
			return record;
		}

		public InsertRecordAction setRecord(Record record) {
			this.record = record;
			return this;
		}

		public Device getDevice() {
			return device;
		}

		public InsertRecordAction setDevice(Device device) {
			this.device = device;
			return this;
		}

		public User getUser() {
			return user;
		}

		public InsertRecordAction setUser(User user) {
			this.user = user;
			return this;
		}

		public boolean isInsertInDb() {
			return insertInDb;
		}

		public InsertRecordAction setInsertInDb(boolean insertInDb) {
			this.insertInDb = insertInDb;
			return this;
		}

		public boolean isCheckAlert() {
			return checkAlert;
		}

		public InsertRecordAction setCheckAlert(boolean checkAlert) {
			this.checkAlert = checkAlert;
			return this;
		}

		public boolean isProcessProxy() {
			return processProxy;
		}

		public InsertRecordAction setProcessProxy(boolean processProxy) {
			this.processProxy = processProxy;
			return this;
		}

		public boolean isPublishToEventBus() {
			return publishToEventBus;
		}

		public InsertRecordAction setPublishToEventBus(boolean publishToEventBus) {
			this.publishToEventBus = publishToEventBus;
			return this;
		}
	}

}
