package me.vinceh121.gmcserver.modules;

import java.util.Date;

import com.mongodb.client.model.Filters;

import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import me.vinceh121.gmcserver.GMCServer;
import me.vinceh121.gmcserver.entities.Device;
import me.vinceh121.gmcserver.entities.Record;
import me.vinceh121.gmcserver.entities.Record.Builder;
import me.vinceh121.gmcserver.entities.User;

public class LoggingModule extends AbstractModule {
	/**
	 * Hex device ID must be appended
	 */
	public static final String ADDRESS_PREFIX_RECORD_LOG = "me.vinceh121.gmcserver.RECORD_LOG.";

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
		this.registerRoute(HttpMethod.GET, "/log2", this::handleLog2);
		this.registerRoute(HttpMethod.GET, "/log", this::handleClassicLog);
	}

	private void handleLog2(final RoutingContext ctx) {
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
			this.error(ctx, 400, LoggingModule.ERROR_USER_ID);
			return;
		}

		final long gmcDeviceId;
		try {
			gmcDeviceId = Long.parseLong(ctx.request().getParam("GID"));
		} catch (final NumberFormatException e) {
			this.error(ctx, 400, LoggingModule.ERROR_DEVICE_ID);
			return;
		}

		final User user
				= this.srv.getDatabaseManager().getCollection(User.class).find(Filters.eq("gmcId", gmcUserId)).first();
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

		final Builder build = new Record.Builder(ctx.request().params());
		build.buildParameters().buildPosition().withCurrentDate().withDevice(device.getId());

		if (this.logIp) {
			if (this.behindReverseProxy) {
				build.withIp(ctx.request().getHeader("X-Forwarded-For"));
			} else {
				build.withIp(ctx.request().remoteAddress().host());
			}
		}

		final Record rec = build.build();

		this.log.debug("Inserting record {}", rec);

		this.srv.getDatabaseManager().getCollection(Record.class).insertOne(rec);
		this.error(ctx, 200, LoggingModule.ERROR_OK);

		this.publishRecord(rec);

		this.srv.getAlertManager()
				.checkAlert()
				.setDev(device)
				.setOwner(user)
				.setLatestRecord(rec)
				.execute()
				.onFailure(t -> this.log.error("Failed to check alert email", t));
	}

	private void handleClassicLog(final RoutingContext ctx) {
		// /log.asp?id=UserAccountID+GeigerCounterID+CPM+ACPM+uSV
		// ACPM and uSV optional
		final String rawParams = ctx.request().getParam("id");

		if (rawParams == null) {
			this.error(ctx, 400, LoggingModule.ERROR_SYNTAX);
			return;
		}

		final String[] splitParams = rawParams.split(" "); // netty parses the +

		if (splitParams.length < 3 || splitParams.length > 5) {
			this.error(ctx, 400, LoggingModule.ERROR_SYNTAX);
			return;
		}

		final long userGmcId;
		try {
			userGmcId = Long.parseLong(splitParams[0]);
		} catch (final NumberFormatException e) {
			this.error(ctx, 400, LoggingModule.ERROR_SYNTAX);
			return;
		}

		final long deviceGmcId;
		try {
			deviceGmcId = Long.parseLong(splitParams[1]);
		} catch (final NumberFormatException e) {
			this.error(ctx, 400, LoggingModule.ERROR_SYNTAX);
			return;
		}

		final double cpm;
		try {
			cpm = Double.parseDouble(splitParams[2]);
		} catch (final NumberFormatException e) {
			this.error(ctx, 400, LoggingModule.ERROR_SYNTAX);
			return;
		}

		final double acpm;
		if (splitParams.length > 3) {
			try {
				acpm = Double.parseDouble(splitParams[3]);
			} catch (final NumberFormatException e) {
				this.error(ctx, 400, LoggingModule.ERROR_SYNTAX);
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
				this.error(ctx, 400, LoggingModule.ERROR_SYNTAX);
				return;
			}
		} else {
			usv = Double.NaN;
		}

		final User user
				= this.srv.getDatabaseManager().getCollection(User.class).find(Filters.eq("gmcId", userGmcId)).first();

		if (user == null) {
			this.error(ctx, 404, LoggingModule.ERROR_USER_ID);
			return;
		}

		final Device device = this.srv.getDatabaseManager()
				.getCollection(Device.class)
				.find(Filters.eq("gmcId", deviceGmcId))
				.first();

		if (device == null) {
			this.error(ctx, 404, LoggingModule.ERROR_DEVICE_ID);
			return;
		}

		if (!user.getId().equals(device.getOwner())) {
			this.error(ctx, 403, LoggingModule.ERROR_DEVICE_NOT_OWNED);
			return;
		}

		final Record rec = new Record();
		rec.setDate(new Date());
		rec.setAcpm(acpm);
		rec.setCpm(cpm);
		rec.setDeviceId(device.getId());
		rec.setUsv(usv);

		if (this.logIp) {
			if (this.behindReverseProxy) {
				rec.setIp(ctx.request().getHeader("X-Forwarded-For"));
			} else {
				rec.setIp(ctx.request().remoteAddress().host());
			}
		}

		this.log.debug("Inserting record using old log {}", rec);

		this.srv.getDatabaseManager().getCollection(Record.class).insertOne(rec);
		ctx.response().setStatusCode(200).end();

		this.publishRecord(rec);

		this.srv.getAlertManager()
				.checkAlert()
				.setDev(device)
				.setOwner(user)
				.setLatestRecord(rec)
				.execute()
				.onFailure(t -> this.log.error("Failed to check alert email", t));
	}

	private void publishRecord(final Record rec) {
		this.srv.getEventBus().publish(LoggingModule.ADDRESS_PREFIX_RECORD_LOG + rec.getDeviceId(), rec);
	}

	@Override
	protected void error(final RoutingContext ctx, final int status, final String desc, final JsonObject extra) {
		if ("application/json".equals(ctx.getAcceptableContentType())) {
			super.error(ctx, status, desc, extra);
			return;
		}

		ctx.response()
				.setStatusCode(status)
				.putHeader("X-GMC-Extras", extra == null ? null : extra.encode())
				.putHeader("Content-Type", "text/plain")
				.end(desc);
	}
}
