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

import java.util.Date;

import org.apache.logging.log4j.message.FormattedMessage;

import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Updates;

import io.vertx.core.Promise;
import me.vinceh121.gmcserver.GMCServer;
import me.vinceh121.gmcserver.actions.AbstractAction;
import me.vinceh121.gmcserver.entities.Device;
import me.vinceh121.gmcserver.entities.Record;
import me.vinceh121.gmcserver.entities.User;
import me.vinceh121.gmcserver.managers.DeviceManager.DeviceStatsAction;
import me.vinceh121.gmcserver.managers.email.Email;

public class AlertManager extends AbstractManager {
	public static final long ALERT_EMAIL_DELAY = 24 * 60 * 60 * 1000; // 1 day

	public AlertManager(final GMCServer srv) {
		super(srv);
	}

	public CheckAlertAction checkAlert() {
		return new CheckAlertAction(this.srv);
	}

	/**
	 * Checks if the device's latest record should throw an alert.
	 * Sends the email if it is required.
	 * 
	 * Returns true if the alter has been throw, false otherwise.
	 * 
	 * Throws {@code IllegalStateException} when device's stats failed to fetch.
	 * 
	 * @see DeviceStatsAction
	 */
	public class CheckAlertAction extends AbstractAction<Boolean> {
		private Device dev;
		private User owner;
		private Record latestRecord;

		public CheckAlertAction(final GMCServer srv) {
			super(srv);
		}

		@Override
		protected void executeSync(final Promise<Boolean> promise) {
			if (!this.owner.isAlertEmails()) {
				promise.complete(false);
				return;
			}
			if (this.dev.isDisabled()
					|| new Date().getTime() - this.dev.getLastEmailAlert().getTime() < AlertManager.ALERT_EMAIL_DELAY
					|| Double.isNaN(this.dev.getStdDevAlertLimit())) {
				promise.complete(false);
				return;
			}
			this.srv.getDeviceManager()
				.deviceStats()
				.setField("cpm")
				.setDevId(this.dev.getId())
				.execute()
				.onSuccess(stats -> {
					if (stats.getStdDev() >= this.dev.getStdDevAlertLimit()) { // too high
						final Email email = new Email();
						email.setTo(this.owner);
						email.setTemplate("device-alert");
						email.setSubject("[ " + this.dev.getName() + " ] Abnormal CPM readings for device");
						email.getContext().put("fieldname", "CPM");
						email.getContext().put("value", this.latestRecord.getCpm());
						email.getContext().put("device", this.dev.toPublicJson());
						this.srv.getEmailManager().sendEmail(email).onSuccess(v -> {
							this.srv.getDatabaseManager()
								.getCollection(Device.class)
								.updateOne(Filters.eq(this.dev.getId()), Updates.set("lastEmailAlert", new Date()));
						});
					}
				})
				.onFailure(t -> {
					AlertManager.this.log.error(new FormattedMessage("Failed to get stats for device {}", this.dev), t);
					promise.fail(new IllegalStateException("Failed to get stats", t));
				});
		}

		public Device getDev() {
			return this.dev;
		}

		public CheckAlertAction setDev(final Device dev) {
			this.dev = dev;
			return this;
		}

		public User getOwner() {
			return this.owner;
		}

		public CheckAlertAction setOwner(final User owner) {
			this.owner = owner;
			return this;
		}

		public Record getLatestRecord() {
			return this.latestRecord;
		}

		public CheckAlertAction setLatestRecord(final Record latestRecord) {
			this.latestRecord = latestRecord;
			return this;
		}
	}
}
