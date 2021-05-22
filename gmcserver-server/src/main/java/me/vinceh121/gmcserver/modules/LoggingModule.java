package me.vinceh121.gmcserver.modules;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import com.mongodb.client.model.Filters;
import com.mongodb.client.model.geojson.Point;
import com.mongodb.client.model.geojson.Position;

import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import me.vinceh121.gmcserver.GMCServer;
import me.vinceh121.gmcserver.entities.Device;
import me.vinceh121.gmcserver.entities.Record;
import me.vinceh121.gmcserver.entities.Record.Builder;
import me.vinceh121.gmcserver.entities.User;

public class LoggingModule extends AbstractModule {
	private static final DateFormat DATE_FORMAT_ISO_8601 = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSX");
	public static final String ERROR_OK = "OK.ERR0";
	public static final String ERROR_SYNTAX = "The syntax of one of the logging parameters is incorrect";
	public static final String ERROR_USER_ID = "Invalid user ID.ERR1";
	public static final String ERROR_DEVICE_ID = "Invalid device ID.ERR2";
	public static final String ERROR_DEVICE_NOT_OWNED = "User does not own device";

	private final boolean logIp, behindReverseProxy;

	public LoggingModule(final GMCServer srv) {
		super(srv);
		this.logIp = Boolean.parseBoolean(this.srv.getConfig().getProperty("geiger.log-ip"));
		this.behindReverseProxy = Boolean.parseBoolean(this.srv.getConfig().getProperty("geiger.behindReverseProxy"));
		this.registerRoute(HttpMethod.GET, "/log2", this::handleGmcLog2);
		this.registerRoute(HttpMethod.GET, "/log", this::handleGmcClassicLog);
		this.registerRoute(HttpMethod.GET, "/radmon.php", this::handleRadmon);
		this.registerRoute(HttpMethod.POST, "/measurements.json", this::handleSafecast);
	}

	private void handleGmcLog2(final RoutingContext ctx) {
		// AID: user id
		// GID: device id
		// CPM: current counts per minute
		// ACPM: current average of counts per minute
		// uSV: calculated absorbed radiation
		// co2: CO2 content in air
		// hcho: Formaldehyde
		// tmp: temperature
		// ap: ?
		// hmdt: humidity
		// accy: accuracy?
		// type: connection type? 'gprs'
		// sat: ?
		// lat
		// lon
		// alt: altitude: https://gis.stackexchange.com/a/233326
		final long gmcUserId;
		try {
			gmcUserId = Long.parseLong(ctx.request().getParam("AID"));
		} catch (final NumberFormatException e) {
			this.gmcError(ctx, 400, LoggingModule.ERROR_USER_ID);
			return;
		}

		final long gmcDeviceId;
		try {
			gmcDeviceId = Long.parseLong(ctx.request().getParam("GID"));
		} catch (final NumberFormatException e) {
			this.gmcError(ctx, 400, LoggingModule.ERROR_DEVICE_ID);
			return;
		}

		final User user = this.srv.getDatabaseManager()
			.getCollection(User.class)
			.find(Filters.eq("gmcId", gmcUserId))
			.first();
		if (user == null) {
			this.gmcError(ctx, 404, LoggingModule.ERROR_USER_ID);
			return;
		}

		final Device device = this.srv.getDatabaseManager()
			.getCollection(Device.class)
			.find(Filters.eq("gmcId", gmcDeviceId))
			.first();
		if (device == null) {
			this.gmcError(ctx, 404, LoggingModule.ERROR_DEVICE_ID);
			return;
		}

		if (device.getOwner() == null) {
			this.log.error("Device with no owner: {}", device.getId());
			this.gmcError(ctx, 403, LoggingModule.ERROR_DEVICE_NOT_OWNED);
			return;
		}

		if (!user.getId().equals(device.getOwner())) {
			this.gmcError(ctx, 403, LoggingModule.ERROR_DEVICE_NOT_OWNED);
			return;
		}

		final Builder build = new Record.Builder(ctx.request().params());
		build.buildParameters().buildPosition().withCurrentDate().withDevice(device.getId());

		final Record rec = build.build();

		this.setRecordIp(ctx, rec);

		this.log.debug("Inserting record {}", rec);
		this.srv.getLoggingManager()
			.insertRecord()
			.setDevice(device)
			.setUser(user)
			.setRecord(rec)
			.execute()
			.onSuccess(v -> {
				this.gmcError(ctx, 200, LoggingModule.ERROR_OK);
			})
			.onFailure(t -> {
				this.gmcError(ctx, 500, t.getMessage() + ".ERR9999");
			});
	}

	private void handleGmcClassicLog(final RoutingContext ctx) {
		// /log.asp?id=UserAccountID+GeigerCounterID+CPM+ACPM+uSV
		// ACPM and uSV optional
		final String rawParams = ctx.request().getParam("id");

		if (rawParams == null) {
			this.gmcError(ctx, 400, LoggingModule.ERROR_SYNTAX);
			return;
		}

		final String[] splitParams = rawParams.split(" "); // netty parses the +

		if (splitParams.length < 3 || splitParams.length > 5) {
			this.gmcError(ctx, 400, LoggingModule.ERROR_SYNTAX);
			return;
		}

		final long userGmcId;
		try {
			userGmcId = Long.parseLong(splitParams[0]);
		} catch (final NumberFormatException e) {
			this.gmcError(ctx, 400, LoggingModule.ERROR_SYNTAX);
			return;
		}

		final long deviceGmcId;
		try {
			deviceGmcId = Long.parseLong(splitParams[1]);
		} catch (final NumberFormatException e) {
			this.gmcError(ctx, 400, LoggingModule.ERROR_SYNTAX);
			return;
		}

		final double cpm;
		try {
			cpm = Double.parseDouble(splitParams[2]);
		} catch (final NumberFormatException e) {
			this.gmcError(ctx, 400, LoggingModule.ERROR_SYNTAX);
			return;
		}

		final double acpm;
		if (splitParams.length > 3) {
			try {
				acpm = Double.parseDouble(splitParams[3]);
			} catch (final NumberFormatException e) {
				this.gmcError(ctx, 400, LoggingModule.ERROR_SYNTAX);
				return;
			}
		} else {
			acpm = Double.NaN;
		}

		final double usv;
		if (splitParams.length > 3) {
			try {
				usv = Double.parseDouble(splitParams[4]);
			} catch (final NumberFormatException e) {
				this.gmcError(ctx, 400, LoggingModule.ERROR_SYNTAX);
				return;
			}
		} else {
			usv = Double.NaN;
		}

		final User user = this.srv.getDatabaseManager()
			.getCollection(User.class)
			.find(Filters.eq("gmcId", userGmcId))
			.first();

		if (user == null) {
			this.gmcError(ctx, 404, LoggingModule.ERROR_USER_ID);
			return;
		}

		final Device device = this.srv.getDatabaseManager()
			.getCollection(Device.class)
			.find(Filters.eq("gmcId", deviceGmcId))
			.first();

		if (device == null) {
			this.gmcError(ctx, 404, LoggingModule.ERROR_DEVICE_ID);
			return;
		}

		if (!user.getId().equals(device.getOwner())) {
			this.gmcError(ctx, 403, LoggingModule.ERROR_DEVICE_NOT_OWNED);
			return;
		}

		final Record rec = new Record();
		rec.setDate(new Date());
		rec.setAcpm(acpm);
		rec.setCpm(cpm);
		rec.setDeviceId(device.getId());
		rec.setUsv(usv);

		this.setRecordIp(ctx, rec);

		this.log.debug("Inserting record using old log {}", rec);

		this.srv.getLoggingManager()
			.insertRecord()
			.setDevice(device)
			.setUser(user)
			.setRecord(rec)
			.execute()
			.onSuccess(v -> {
				this.gmcError(ctx, 200, LoggingModule.ERROR_OK);
			})
			.onFailure(t -> {
				this.gmcError(ctx, 500, t.getMessage() + ".ERR9999");
			});
	}

	private void handleRadmon(final RoutingContext ctx) {
		if ("submit".equals(ctx.request().getParam("function"))) {
			this.error(ctx, 400, "Parameter function should be submit");
			return;
		}

		if ("CPM".equals(ctx.request().getParam("unit"))) {
			this.error(ctx, 400, "Parameter unit should be CPM");
		}

		final long gmcUserId;
		try {
			gmcUserId = Long.parseLong(ctx.request().getParam("user"));
		} catch (final NumberFormatException e) {
			this.error(ctx, 400, LoggingModule.ERROR_USER_ID);
			return;
		}

		final long gmcDeviceId;
		try {
			gmcDeviceId = Long.parseLong(ctx.request().getParam("password"));
		} catch (final NumberFormatException e) {
			this.error(ctx, 400, LoggingModule.ERROR_DEVICE_ID);
			return;
		}

		final User user = this.srv.getDatabaseManager()
			.getCollection(User.class)
			.find(Filters.eq("gmcId", gmcUserId))
			.first();
		if (user == null) {
			this.gmcError(ctx, 404, LoggingModule.ERROR_USER_ID);
			return;
		}

		final Device device = this.srv.getDatabaseManager()
			.getCollection(Device.class)
			.find(Filters.eq("gmcId", gmcDeviceId))
			.first();
		if (device == null) {
			this.gmcError(ctx, 404, LoggingModule.ERROR_DEVICE_ID);
			return;
		}

		if (device.getOwner() == null) {
			this.log.error("Device with no owner: {}", device.getId());
			this.gmcError(ctx, 403, LoggingModule.ERROR_DEVICE_NOT_OWNED);
			return;
		}

		if (!user.getId().equals(device.getOwner())) {
			this.gmcError(ctx, 403, LoggingModule.ERROR_DEVICE_NOT_OWNED);
			return;
		}

		if (ctx.request().getParam("value") == null) {
			this.error(ctx, 400, "Invalid value");
			return;
		}

		final double cpm;
		try {
			cpm = Double.parseDouble(ctx.request().getParam("value"));
		} catch (final NumberFormatException e) {
			this.error(ctx, 400, "Invalid value");
			return;
		}

		final Record rec = new Record();
		rec.setCpm(cpm);
		rec.setDeviceId(device.getId());
		rec.setDate(new Date());
		this.setRecordIp(ctx, rec);

		this.srv.getLoggingManager()
			.insertRecord()
			.setDevice(device)
			.setUser(user)
			.setRecord(rec)
			.execute()
			.onSuccess(v -> {
				ctx.response().setStatusCode(200).end("OK<br>");
			})
			.onFailure(t -> {
				this.error(ctx, 500, "Failed to insert record: " + t);
			});
	}

	private void handleSafecast(final RoutingContext ctx) {
		final long gmcUserId;
		try {
			gmcUserId = Long.parseLong(ctx.request().getParam("api_key"));
		} catch (final NumberFormatException e) {
			this.error(ctx, 400, LoggingModule.ERROR_USER_ID);
			return;
		}

		final JsonObject obj = ctx.getBodyAsJson();

		final long gmcDeviceId;
		try {
			gmcDeviceId = obj.getLong("device_id");
		} catch (final ClassCastException e) {
			this.error(ctx, 400, LoggingModule.ERROR_DEVICE_ID);
			return;
		}

		final User user = this.srv.getDatabaseManager()
			.getCollection(User.class)
			.find(Filters.eq("gmcId", gmcUserId))
			.first();
		if (user == null) {
			this.error(ctx, 404, LoggingModule.ERROR_USER_ID);
			return;
		}

		final Device device = this.srv.getDatabaseManager()
			.getCollection(Device.class)
			.find(Filters.eq("gmcId", gmcDeviceId))
			.first();
		if (device == null) {
			this.error(ctx, 404, LoggingModule.ERROR_DEVICE_ID);
			return;
		}

		if (device.getOwner() == null) {
			this.log.error("Device with no owner: {}", device.getId());
			this.error(ctx, 403, LoggingModule.ERROR_DEVICE_NOT_OWNED);
			return;
		}

		if (!user.getId().equals(device.getOwner())) {
			this.error(ctx, 403, LoggingModule.ERROR_DEVICE_NOT_OWNED);
			return;
		}

		if (!"cpm".equals(obj.getString("unit"))) {
			this.error(ctx, 400, "Value of `unit` must be `cpm`");
			return;
		}

		final Record r = new Record();
		this.setRecordIp(ctx, r);
		r.setDeviceId(device.getId());

		try {
			final String rawDate = obj.getString("captured_at");
			final Date date = DATE_FORMAT_ISO_8601.parse(rawDate);
			r.setDate(date);
		} catch (final ClassCastException | ParseException e) {
			this.error(ctx, 400, "Invalid date");
			return;
		}

		try {
			final Double lon = obj.getDouble("longitude");
			final Double lat = obj.getDouble("latitude");
			if (lon == null || lat == null) {
				this.error(ctx, 400, "Location is required");
				return;
			}
			r.setLocation(new Point(new Position(lon, lat)));
		} catch (final ClassCastException e) {
			this.error(ctx, 400, "Invalid location");
			return;
		}

		try {
			final Integer cpm = obj.getInteger("value");
			if (cpm == null) {
				this.error(ctx, 400, "Value is required");
				return;
			}
		} catch (final ClassCastException e) {
			this.error(ctx, 400, "Invalid value");
			return;
		}

		this.srv.getLoggingManager()
			.insertRecord()
			.setRecord(r)
			.setDevice(device)
			.setUser(user)
			.execute()
			.onSuccess(v -> this.error(ctx, 200, ""))
			.onFailure(t -> {
				this.error(ctx, 500, "Failed to insert record: " + t);
			});
	}

	private void setRecordIp(final RoutingContext ctx, final Record r) {
		if (this.logIp) {
			if (this.behindReverseProxy) {
				r.setIp(ctx.request().getHeader("X-Forwarded-For"));
			} else {
				r.setIp(ctx.request().remoteAddress().host());
			}
		}
	}

	protected void gmcError(final RoutingContext ctx, final int status, final String desc) {
		this.gmcError(ctx, status, desc, null);
	}

	protected void gmcError(final RoutingContext ctx, final int status, final String desc, final JsonObject extra) {
		if ("application/json".equals(ctx.getAcceptableContentType())) {
			this.error(ctx, status, desc, extra);
			return;
		}

		ctx.response()
			.setStatusCode(status)
			.putHeader("X-GMC-Extras", extra == null ? null : extra.encode())
			.putHeader("Content-Type", "text/plain")
			.end(desc);
	}
}
