package me.vinceh121.gmcserver.managers;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Vector;

import org.bson.BsonNull;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.bson.types.ObjectId;

import com.mongodb.client.model.Accumulators;
import com.mongodb.client.model.Aggregates;
import com.mongodb.client.model.BsonField;
import com.mongodb.client.model.Filters;

import io.vertx.core.Promise;
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

	public class GetCalendarAction extends AbstractAction<DeviceCalendar> {
		private ObjectId deviceId;

		public GetCalendarAction(final GMCServer srv) {
			super(srv);
		}

		@Override
		protected void executeSync(final Promise<DeviceCalendar> promise) {
			final DeviceCalendar cal = this.srv.getDatabaseManager()
					.getCollection(DeviceCalendar.class)
					.find(Filters.eq("deviceId", this.deviceId))
					.first();
			promise.complete(cal);
			if (cal == null) {
				DeviceCalendarManager.this.calculateCalendar().setDeviceId(this.deviceId).execute();
			}
		}

		public ObjectId getDeviceId() {
			return this.deviceId;
		}

		public GetCalendarAction setDeviceId(final ObjectId deviceId) {
			this.deviceId = deviceId;
			return this;
		}
	}

	public class CalculateCalendarAction extends AbstractAction<DeviceCalendar> {
		private ObjectId deviceId;

		public CalculateCalendarAction(final GMCServer srv) {
			super(srv);
		}

		@Override
		protected void executeSync(final Promise<DeviceCalendar> promise) {
			final ZoneId zone = ZoneId.systemDefault();
			final List<Document> recs = new Vector<>();
			final DeviceCalendar cal = new DeviceCalendar();
			cal.setDeviceId(this.deviceId);
			cal.setLastCalculationDate(new Date());
			cal.setRecs(recs);
			cal.setInProgress(true);
			this.srv.getDatabaseManager().getCollection(DeviceCalendar.class).insertOne(cal);

			final Date[] arrMinMax = this.getDeviceDateBounds(this.deviceId);
			final LocalDate startDate = LocalDate.ofInstant(arrMinMax[0].toInstant(), zone);
			final LocalDate endDate = LocalDate.ofInstant(arrMinMax[1].toInstant(), zone);
			for (LocalDate currentDay = startDate; currentDay.isBefore(endDate); currentDay = currentDay.plusDays(1)) {
				final LocalDateTime curDayTime = currentDay.atStartOfDay();
				final Date curDate = new Date(curDayTime.toEpochSecond(zone.getRules().getOffset(curDayTime)) * 1000);
				final Document rec = this.srv.getDatabaseManager()
						.getCollection(Record.class)
						.aggregate(
								DeviceCalendarManager.getAveragePipeline(this.deviceId, curDate,
										new Date(curDayTime.plusDays(1)
												.toEpochSecond(zone.getRules().getOffset(curDayTime)) * 1000)),
								Document.class)
						.first();
				if (rec != null) {
					rec.put("date", curDate);
					recs.add(rec);
				}
			}
			cal.setInProgress(false);
			this.srv.getDatabaseManager().getCollection(DeviceCalendar.class).replaceOne(Filters.eq(cal.getId()), cal);
			promise.complete(cal);
		}

		public ObjectId getDeviceId() {
			return this.deviceId;
		}

		public CalculateCalendarAction setDeviceId(final ObjectId deviceId) {
			this.deviceId = deviceId;
			return this;
		}

		private Date[] getDeviceDateBounds(final ObjectId deviceI) {
			final Document doc = this.srv.getDatabaseManager()
					.getCollection(Record.class)
					.aggregate(Arrays.asList(Aggregates.group(new BsonNull(), Accumulators.min("min", "$date"),
							Accumulators.max("max", "$date"))), Document.class)
					.first();
			return new Date[] { doc.getDate("min"), doc.getDate("max") };
		}
	}

	public static List<Bson> getAveragePipeline(final ObjectId id, final Date lowerBound, final Date upperBound) {
		final List<BsonField> fields = new ArrayList<>(Record.STAT_FIELDS.size());
		for (final String f : Record.STAT_FIELDS) {
			fields.add(Accumulators.avg(f, "$" + f));
		}
		return Arrays.asList(
				Aggregates.match(Filters.and(Filters.eq("deviceId", id),
						Filters.and(Filters.gte("date", lowerBound), Filters.lt("date", upperBound)))),
				Aggregates.group(new BsonNull(), fields));
	}
}
