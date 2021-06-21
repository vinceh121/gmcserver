package me.vinceh121.gmcserver.managers;

import java.util.List;
import java.util.Vector;

import org.apache.logging.log4j.message.FormattedMessage;

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
		private final boolean differAlert = true;
		private final boolean differProxy = true;

		public InsertRecordAction(final GMCServer srv) {
			super(srv);
		}

		@Override
		protected void executeSync(final Promise<Void> promise) {
			@SuppressWarnings("rawtypes")
			final List<Future> joinedFutures = new Vector<>();
			@SuppressWarnings("rawtypes")
			final List<Future> differedFutures = new Vector<>();

			if (this.insertInDb) {
				joinedFutures.add(Future.future(p -> {
					try {
						this.srv.getDatabaseManager().getCollection(Record.class).insertOne(this.record);
						p.complete();
					} catch (final Exception e) {
						p.fail(e);
					}
				}));
			}

			if (this.checkAlert) {
				(this.differAlert ? differedFutures : joinedFutures).add(this.srv.getAlertManager()
					.checkAlert()
					.setDev(this.device)
					.setOwner(this.user)
					.setLatestRecord(this.record)
					.execute());
			}

			if (this.processProxy) {
				(this.differProxy ? differedFutures : joinedFutures).add(this.srv.getProxyManager()
					.processDeviceProxies()
					.setDevice(this.device)
					.setRecord(this.record)
					.execute());
			}

			if (this.publishToEventBus) {
				this.srv.getEventBus()
					.publish(LoggingManager.ADDRESS_PREFIX_RECORD_LOG + this.record.getDeviceId(), this.record);
			}

			CompositeFuture.join(joinedFutures).onSuccess(c -> promise.complete()).onFailure(promise::fail);
			CompositeFuture.join(differedFutures)
				.onFailure(t -> LoggingManager.this.log
					.error(new FormattedMessage("Error while logging record {}", this.record.getId()), t));
		}

		public Record getRecord() {
			return this.record;
		}

		public InsertRecordAction setRecord(final Record record) {
			this.record = record;
			return this;
		}

		public Device getDevice() {
			return this.device;
		}

		public InsertRecordAction setDevice(final Device device) {
			this.device = device;
			return this;
		}

		public User getUser() {
			return this.user;
		}

		public InsertRecordAction setUser(final User user) {
			this.user = user;
			return this;
		}

		public boolean isInsertInDb() {
			return this.insertInDb;
		}

		public InsertRecordAction setInsertInDb(final boolean insertInDb) {
			this.insertInDb = insertInDb;
			return this;
		}

		public boolean isCheckAlert() {
			return this.checkAlert;
		}

		public InsertRecordAction setCheckAlert(final boolean checkAlert) {
			this.checkAlert = checkAlert;
			return this;
		}

		public boolean isProcessProxy() {
			return this.processProxy;
		}

		public InsertRecordAction setProcessProxy(final boolean processProxy) {
			this.processProxy = processProxy;
			return this;
		}

		public boolean isPublishToEventBus() {
			return this.publishToEventBus;
		}

		public InsertRecordAction setPublishToEventBus(final boolean publishToEventBus) {
			this.publishToEventBus = publishToEventBus;
			return this;
		}
	}

}
