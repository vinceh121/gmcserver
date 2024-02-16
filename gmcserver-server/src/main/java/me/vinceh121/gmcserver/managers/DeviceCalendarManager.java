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

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Date;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonArray;
import io.vertx.sqlclient.Row;
import me.vinceh121.gmcserver.GMCServer;
import me.vinceh121.gmcserver.actions.AbstractAction;
import me.vinceh121.gmcserver.entities.DeviceCalendar;
import me.vinceh121.gmcserver.entities.Record;

public class DeviceCalendarManager extends AbstractManager {
	public DeviceCalendarManager(final GMCServer srv) {
		super(srv);
	}

	public GetCalendarAction getCalendar() {
		return new GetCalendarAction(this.srv);
	}

	public CalculateCalendarAction calculateCalendar() {
		return new CalculateCalendarAction(this.srv);
	}

	/**
	 * Fetches the device's calendar and returns it. If this device doesn't have a
	 * calendar, returns {@code null} and then starts asynchronously calendar
	 * calculation.
	 */
	public class GetCalendarAction extends AbstractAction<DeviceCalendar> {
		private UUID deviceId;

		public GetCalendarAction(final GMCServer srv) {
			super(srv);
		}

		@Override
		protected void executeSync(final Promise<DeviceCalendar> promise) {
			this.srv.getDatabaseManager()
				.query("SELECT * FROM calendar WHERE deviceId = #{deviceId}")
				.mapTo(DeviceCalendar.class)
				.execute(Map.of("deviceId", this.deviceId))
				.onSuccess(rowSet -> {
					final DeviceCalendar cal = rowSet.iterator().next();

					promise.complete(cal);

					if (cal == null) {
						DeviceCalendarManager.this.calculateCalendar().setDeviceId(this.deviceId).execute();
					}
				})
				.onFailure(promise::fail);
		}

		public UUID getDeviceId() {
			return this.deviceId;
		}

		public GetCalendarAction setDeviceId(final UUID deviceId) {
			this.deviceId = deviceId;
			return this;
		}
	}

	/**
	 * Calculates a device's calendar and returns it.
	 */
	public class CalculateCalendarAction extends AbstractAction<DeviceCalendar> {
		private UUID deviceId;

		public CalculateCalendarAction(final GMCServer srv) {
			super(srv);
		}

		@Override
		protected void executeSync(final Promise<DeviceCalendar> promise) {
			final ZoneId zone = ZoneId.of("GMT");
			final JsonArray recs = new JsonArray();
			final DeviceCalendar cal = new DeviceCalendar();
			cal.setDeviceId(this.deviceId);
			cal.setCreatedAt(new Date());
			cal.setRecs(recs);
			cal.setInProgress(true);

			this.srv.getDatabaseManager()
				.update("INSERT INTO calendar VALUES")
				.mapFrom(DeviceCalendar.class)
				.execute(cal)
				.onSuccess(r -> {
					this.getDeviceDateBounds(this.deviceId).onSuccess(arrMinMax -> {
						final LocalDate startDate = LocalDate.ofInstant(arrMinMax[0].toInstant(), zone);
						final LocalDate endDate = LocalDate.ofInstant(arrMinMax[1].toInstant(), zone);
						this.recurseCompileDay(cal, zone, startDate, endDate)
							.onSuccess(promise::complete)
							.onFailure(promise::fail);
					}).onFailure(promise::fail);
				})
				.onFailure(promise::fail);
		}

		private Future<DeviceCalendar> recurseCompileDay(final DeviceCalendar cal, final ZoneId zone,
				final LocalDate currentDay, final LocalDate endDate) {
			return Future.future(promise -> {
				final LocalDateTime curDayTime = currentDay.atStartOfDay();
				final Date curDate = new Date(curDayTime.toEpochSecond(zone.getRules().getOffset(curDayTime)) * 1000);
				final Date endAvgDate = new Date(
						curDayTime.plusDays(1).toEpochSecond(zone.getRules().getOffset(curDayTime)) * 1000);

				this.srv.getDatabaseManager()
					.query("SELECT " + String.join(", ",
							Record.STAT_FIELDS.stream().map(f -> "avg(" + f + ") as " + f).collect(Collectors.toList()))
							+ " FROM records WHERE date > #{start} AND date < #{end}")
					.mapTo(Record.class)
					.execute(Map.of("start", curDate, "end", endAvgDate))
					.onSuccess(rowSet -> {
						final Record rec = rowSet.iterator().next();

						if (rec != null) {
							rec.setDate(curDate);
							cal.getRecs().add(rec);
						}

						final LocalDate nextDay = currentDay.plusDays(1);
						if (nextDay.equals(endDate)) {
							cal.setInProgress(false);
							this.srv.getDatabaseManager()
								.update("UPDATE calendar SET")
								.mapFrom(DeviceCalendar.class)
								.execute(cal)
								.onSuccess(r -> {
									promise.complete(cal);
								})
								.onFailure(promise::fail);
						} else {
							this.recurseCompileDay(cal, zone, nextDay, endDate)
								.onSuccess(promise::complete)
								.onFailure(promise::fail);
						}
					});
			});
		}

		public UUID getDeviceId() {
			return this.deviceId;
		}

		public CalculateCalendarAction setDeviceId(final UUID deviceId) {
			this.deviceId = deviceId;
			return this;
		}

		private Future<Date[]> getDeviceDateBounds(final UUID id) {
			return Future.future(promise -> {
				this.srv.getDatabaseManager()
					.query("SELECT min(date), max(date) FROM records WHERE deviceId = #{deviceId}")
					.execute(Map.of("deviceId", id))
					.onSuccess(rowSet -> {
						final Row row = rowSet.iterator().next();
						promise.complete(
								new Date[] { new Date(row.getLocalDateTime("min").toEpochSecond(ZoneOffset.UTC) * 1000),
										new Date(row.getLocalDateTime("max").toEpochSecond(ZoneOffset.UTC) * 1000) });
					})
					.onFailure(promise::fail);
			});
		}
	}
}
