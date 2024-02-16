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
	 * Device UUID must be appended
	 */
	public static final String ADDRESS_PREFIX_RECORD_LOG = "me.vinceh121.gmcserver.RECORD_LOG.";

	public LoggingManager(final GMCServer srv) {
		super(srv);
	}

	public InsertRecordAction insertRecord() {
		return new InsertRecordAction(this.srv);
	}

	/**
	 * Inserts a record. This will (optionnally) check for alerts, process proxying,
	 * publish it to the event bus.
	 *
	 * Alerting and proxying can be differed.
	 *
	 * Rethrows exceptions thrown by {@code CheckAlertAction},
	 * {@code ProcessDeviceProxiesAction}
	 */
	public class InsertRecordAction extends AbstractAction<Void> {
		private Record record;
		private Device device;
		private User user;
		private boolean insertInDb = true, checkAlert = true, processProxy = true, publishToEventBus = true,
				differAlert = true, differProxy = true, setLocationFromDevice;

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
					this.srv.getDatabaseManager()
						.update("INSERT INTO records VALUES (" + Record.sqlFields() + ")")
						.mapFrom(Record.class)
						.execute(this.record)
						.onSuccess(e -> p.complete())
						.onFailure(promise::fail);
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

			if (this.processProxy && this.device.getProxiesSettings() != null) {
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

			if (this.setLocationFromDevice && this.device.getLocation() != null && this.record.getLocation() == null) {
				this.record.setLocation(this.device.getLocation());
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

		public boolean isDifferAlert() {
			return differAlert;
		}

		public InsertRecordAction setDifferAlert(boolean differAlert) {
			this.differAlert = differAlert;
			return this;
		}

		public boolean isDifferProxy() {
			return differProxy;
		}

		public InsertRecordAction setDifferProxy(boolean differProxy) {
			this.differProxy = differProxy;
			return this;
		}

		public boolean isSetLocationFromDevice() {
			return setLocationFromDevice;
		}

		public InsertRecordAction setLocationFromDevice(boolean setLocationFromDevice) {
			this.setLocationFromDevice = setLocationFromDevice;
			return this;
		}
	}
}
