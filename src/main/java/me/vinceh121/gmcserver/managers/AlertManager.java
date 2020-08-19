package me.vinceh121.gmcserver.managers;

import java.util.Date;

import io.vertx.core.Promise;
import me.vinceh121.gmcserver.GMCServer;
import me.vinceh121.gmcserver.actions.AbstractAction;
import me.vinceh121.gmcserver.entities.Device;
import me.vinceh121.gmcserver.entities.DeviceStats;
import me.vinceh121.gmcserver.entities.Record;
import me.vinceh121.gmcserver.entities.User;
import me.vinceh121.gmcserver.managers.email.Email;
import me.vinceh121.gmcserver.managers.email.EmailManager;

public class AlertManager extends AbstractManager {
	public static final long ALERT_EMAIL_DELAY = 24 * 60 * 60 * 1000; // 1 day

	public AlertManager(GMCServer srv) {
		super(srv);
	}

	public CheckAlertAction checkAlert() {
		return new CheckAlertAction(srv);
	}

	public class CheckAlertAction extends AbstractAction<Boolean> {
		private Device dev;
		private User owner;
		private Record latestRecord;

		public CheckAlertAction(final GMCServer srv) {
			super(srv);
		}

		@Override
		protected void executeSync(final Promise<Boolean> promise) {
			if (new Date().getTime() - dev.getLastEmailAlert().getTime() < ALERT_EMAIL_DELAY) {
				promise.complete(false);
				return;
			}
			this.srv.getManager(DeviceManager.class)
					.deviceStats()
					.setField("cpm")
					.setDevId(dev.getId())
					.execute()
					.onComplete(statsRes -> {
						if (statsRes.failed()) {
							log.error("Failed to get stats for device {}", dev);
							promise.fail("Failed to get stats");
							return;
						}
						final DeviceStats stats = statsRes.result();

						final double upperBound = stats.getAvg() + stats.getStdDev();
						// final double lowerBound = stats.getAvg() - stats.getStdDev();

						if (this.latestRecord.getCpm() > upperBound) { // too high
							final Email email = new Email();
							email.setTo(owner);
							email.setTemplate("device-alert");
							email.setSubject("[ " + dev.getName() + " ] Abnormal CPM readings for device");
							email.getContext().put("fieldname", "CPM");
							email.getContext().put("value", latestRecord.getCpm());
							email.getContext().put("device", dev.toPublicJson());
							this.srv.getManager(EmailManager.class).sendEmail(email);
						}
						// else if (this.latestRecord.getCpm()< lowerBound) {} // too low

					});
		}

		public Device getDev() {
			return dev;
		}

		public CheckAlertAction setDev(Device dev) {
			this.dev = dev;
			return this;
		}

		public User getOwner() {
			return owner;
		}

		public CheckAlertAction setOwner(User owner) {
			this.owner = owner;
			return this;
		}

		public Record getLatestRecord() {
			return latestRecord;
		}

		public CheckAlertAction setLatestRecord(Record latestRecord) {
			this.latestRecord = latestRecord;
			return this;
		}
	}
}
